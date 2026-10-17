package com.reminder.i18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Проверяет, что все языковые пакеты содержат тот же набор ключей, что и базовый русский. */
class LanguageKeyParityTest {

    @Test
    void allBundledLanguagesCoverBaseKeys(@TempDir Path dir) {
        Map<String, Language> byCode = new LanguageManager(dir).load().stream()
                .collect(Collectors.toMap(Language::code, l -> l));
        Language base = byCode.get("ru");
        assertTrue(base != null && !base.strings().isEmpty(), "base ru missing");

        for (String code : List.of("en", "de", "bg")) {
            Language language = byCode.get(code);
            assertNotNull(language, code + " missing");
            TreeSet<String> missing = new TreeSet<>(base.strings().keySet());
            missing.removeAll(language.strings().keySet());
            assertTrue(missing.isEmpty(), code + " missing keys: " + missing);
        }
    }
}
