package com.reminder.ui;

import com.reminder.i18n.Language;
import com.reminder.i18n.LanguageManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Статическая проверка FXML: наличие ключей локализации и совпадение fx:id с полями контроллеров. */
class FxmlResourcesTest {

    private static final List<String> FXML_FILES = List.of(
            "main-view", "settings-dialog", "export-dialog", "holidays-dialog", "reminders-dialog");

    private static String read(String name) throws Exception {
        try (InputStream in = FxmlResourcesTest.class.getResourceAsStream("/fxml/" + name + ".fxml")) {
            assertNotNull(in, "FXML not found: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void everyResourceKeyExistsInBaseLanguage(@TempDir Path dir) throws Exception {
        Language base = new LanguageManager(dir).load().stream()
                .filter(l -> l.code().equals("ru")).findFirst().orElseThrow();
        Set<String> keys = base.strings().keySet();

        Pattern reference = Pattern.compile("=\"%(\\S+?)\"");
        for (String name : FXML_FILES) {
            Matcher matcher = reference.matcher(read(name));
            List<String> missing = new ArrayList<>();
            while (matcher.find()) {
                if (!keys.contains(matcher.group(1))) {
                    missing.add(matcher.group(1));
                }
            }
            assertTrue(missing.isEmpty(), name + " missing keys: " + missing);
        }
    }

    @Test
    void everyFxIdMapsToControllerField() throws Exception {
        Pattern controllerPattern = Pattern.compile("fx:controller=\"([^\"]+)\"");
        Pattern idPattern = Pattern.compile("fx:id=\"([^\"]+)\"");

        for (String name : FXML_FILES) {
            // fx:define-блоки создают объекты без внедрения в поля контроллера — исключаем их.
            String text = read(name).replaceAll("(?s)<fx:define>.*?</fx:define>", "");
            Matcher controllerMatcher = controllerPattern.matcher(text);
            assertTrue(controllerMatcher.find(), name + " has no fx:controller");
            Class<?> controller = Class.forName(controllerMatcher.group(1));
            Set<String> fields = java.util.Arrays.stream(controller.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());

            Matcher idMatcher = idPattern.matcher(text);
            List<String> missing = new ArrayList<>();
            while (idMatcher.find()) {
                if (!fields.contains(idMatcher.group(1))) {
                    missing.add(idMatcher.group(1));
                }
            }
            assertTrue(missing.isEmpty(), name + " fx:id without controller field: " + missing);
        }
    }
}
