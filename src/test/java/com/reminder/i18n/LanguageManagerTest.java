package com.reminder.i18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageManagerTest {

    private static Language find(List<Language> languages, String code) {
        return languages.stream().filter(l -> l.code().equals(code)).findFirst().orElseThrow();
    }

    @Test
    void loadsAllBuiltInLanguages(@TempDir Path dir) {
        List<Language> languages = new LanguageManager(dir).load();
        var codes = languages.stream().map(Language::code).collect(Collectors.toSet());
        assertTrue(codes.containsAll(List.of("ru", "en", "de", "bg")), "codes: " + codes);
    }

    @Test
    void extractsExternalGermanAndBulgarian(@TempDir Path dir) {
        new LanguageManager(dir).load();
        assertTrue(Files.exists(dir.resolve("de_lang.yaml")));
        assertTrue(Files.exists(dir.resolve("bg_lang.yaml")));
        assertTrue(Files.exists(dir.resolve("README.txt")));
    }

    @Test
    void existingLanguageFilesAreNotOverwritten(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("de_lang.yaml"), "kept", StandardCharsets.UTF_8);
        new LanguageManager(dir).load();
        assertEquals("kept", Files.readString(dir.resolve("de_lang.yaml"), StandardCharsets.UTF_8));
    }

    @Test
    void readmeIsRefreshedOnLoad(@TempDir Path dir) throws IOException {
        Path readme = dir.resolve("README.txt");
        Files.writeString(readme, "stale content", StandardCharsets.UTF_8);
        new LanguageManager(dir).load();
        String content = Files.readString(readme, StandardCharsets.UTF_8);
        assertTrue(content.contains("TaskReminder"), content);
        assertFalse(content.contains("stale content"), content);
    }

    @Test
    void externalFileOverridesBuiltIn(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("en_lang.yaml"),
                "_language: en\n_name: Custom English\nstrings:\n  app.title: \"Custom\"\n",
                StandardCharsets.UTF_8);
        Language en = find(new LanguageManager(dir).load(), "en");
        assertEquals("Custom English", en.displayName());
        assertEquals("Custom", en.strings().get("app.title"));
        assertEquals("Built-in", en.strings().get("settings.language.source.builtin"));
    }

    @Test
    void optionalSourceKeysDoNotMakeCustomLanguageIncomplete(@TempDir Path dir) throws IOException {
        LanguageManager manager = new LanguageManager(dir);
        Language ru = find(manager.load(), "ru");

        Map<String, Object> strings = new LinkedHashMap<>(ru.strings());
        strings.keySet().removeIf(key -> key.startsWith("settings.language.source."));
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("_language", "xx");
        root.put("_name", "Test");
        root.put("strings", strings);
        Files.writeString(dir.resolve("xx_lang.yaml"), new Yaml().dump(root),
                StandardCharsets.UTF_8);

        Language xx = find(new LanguageManager(dir).load(), "xx");
        assertEquals(LanguageStatus.OK, xx.status());
        assertEquals(xx.totalKeys(), xx.presentKeys());
    }

    @Test
    void customLanguageHasItsOwnLocale(@TempDir Path dir) {
        assertEquals("fr", I18n.localeFor("fr").getLanguage());
        assertEquals("en-GB", I18n.localeFor("en").toLanguageTag());
        assertEquals("de-DE", I18n.localeFor("de").toLanguageTag());
        assertEquals("bg-BG", I18n.localeFor("bg").toLanguageTag());
        assertEquals("ru-RU", I18n.localeFor("").toLanguageTag());
    }

    @Test
    void incompletePackageIsMarked(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("xx_lang.yaml"),
                "_language: xx\n_name: Test\nstrings:\n  app.title: \"T\"\n",
                StandardCharsets.UTF_8);
        Language xx = find(new LanguageManager(dir).load(), "xx");
        assertEquals(LanguageStatus.INCOMPLETE, xx.status());
        assertTrue(xx.presentKeys() < xx.totalKeys());
    }

    @Test
    void damagedPackageIsMarked(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("yy_lang.yaml"), "foo: [unclosed\n", StandardCharsets.UTF_8);
        Language yy = find(new LanguageManager(dir).load(), "yy");
        assertEquals(LanguageStatus.DAMAGED, yy.status());
    }

    @Test
    void missingStringsSectionIsDamaged(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("zz_lang.yaml"),
                "_language: zz\n_name: Zed\n", StandardCharsets.UTF_8);
        Language zz = find(new LanguageManager(dir).load(), "zz");
        assertEquals(LanguageStatus.DAMAGED, zz.status());
        assertTrue(zz.detail() != null && !zz.detail().isBlank());
    }
}
