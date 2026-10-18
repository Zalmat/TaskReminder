package com.reminder.app;

import com.reminder.model.UpdateInfo;
import com.reminder.service.UpdateDownloader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Режим разработки: включает отладочные/демонстрационные функции во время тестирования.
 * Включается любым из способов:
 * <ul>
 *     <li>аргумент запуска {@code --dev} или {@code --updateDemo};</li>
 *     <li>системное свойство {@code -Dreminder.dev=true};</li>
 *     <li>файл-маркер {@code dev.flag} в рабочем каталоге рядом с программой.</li>
 * </ul>
 * Источник архива для теста автообновления задаётся системным свойством
 * {@code -Dreminder.updateFrom=<путь|URL>} (и {@code reminder.updateVersion}, {@code reminder.updateSha})
 * либо строками в файле {@code dev.flag}:
 * <pre>
 * updateFrom=C:\path\to\TaskReminder-9.9.9-win-x64.zip
 * updateVersion=9.9.9
 * </pre>
 * При локальном файле sha256 считается автоматически.
 */
public final class DevMode {

    private static final String FLAG_FILE = "dev.flag";
    private static final String DEV_ARG = "--dev";
    private static final String UPDATE_DEMO_ARG = "--updateDemo";

    private static boolean enabled;
    private static final Map<String, String> flagProperties = new HashMap<>();

    private DevMode() {
    }

    /** Инициализация до старта приложения. */
    public static void init(List<String> args) {
        boolean byArg = args != null && (args.contains(DEV_ARG) || args.contains(UPDATE_DEMO_ARG));
        boolean hasFlagFile = Files.exists(Path.of(FLAG_FILE));
        if (hasFlagFile) {
            loadFlagFile();
        }
        enabled = byArg || Boolean.getBoolean("reminder.dev") || hasFlagFile;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** Задан ли источник архива для теста реального обновления. */
    public static boolean hasUpdateSource() {
        return !updateFrom().isBlank();
    }

    public static String updateFrom() {
        return setting("reminder.updateFrom", "updateFrom");
    }

    private static String updateVersion() {
        return setting("reminder.updateVersion", "updateVersion");
    }

    private static String updateSha() {
        return setting("reminder.updateSha", "updateSha");
    }

    /** Демо-данные диалога обновления. Если задан источник — с реальным архивом. */
    public static UpdateInfo demoUpdate() {
        if (hasUpdateSource()) {
            return devUpdate();
        }
        return new UpdateInfo("1.5.0-demo", "https://github.com/Zalmat/TaskReminder/releases",
                "TaskReminder demo",
                """
                Что нового (пример):
                - Мультиязычный интерфейс (ru/en/de/bg)
                - Настройки языка и папки для отчётов
                - Описание релиза раскрывается по ссылке «Что нового»
                - Пункт 4: проверка прокрутки длинного описания
                - Пункт 5
                - Пункт 6
                - Пункт 7
                - Пункт 8
                - Пункт 9
                - Пункт 10
                - Пункт 11
                - Пункт 12
                - Пункт 13
                - Пункт 14
                - Пункт 15
                И длинная строка без пробелов для проверки переноса: aaaabbbbbcccccdddddeeeeefffffggggghhhhhiiiiijjjjjkkkkklllllmmmmmnnnnnooooo
                """,
                "", "", "", "", true, false);
    }

    /** Реальный UpdateInfo для теста автообновления из локального источника. */
    private static UpdateInfo devUpdate() {
        String from = updateFrom();
        String version = updateVersion().isBlank() ? "9.9.9" : updateVersion();
        String archiveName = fileName(from);
        String checksum = updateSha();
        if (checksum.isBlank() && UpdateDownloader.isLocal(from)) {
            try {
                Path path = UpdateDownloader.localPath(from);
                if (Files.isRegularFile(path)) {
                    checksum = UpdateDownloader.sha256(path);
                }
            } catch (Exception ignored) {
                // без checksum — загрузчик примет локальный файл
            }
        }
        return new UpdateInfo(version, "https://github.com/Zalmat/TaskReminder/releases",
                "TaskReminder dev", "Демо-описание (dev).",
                from, archiveName, checksum, "", true, false);
    }

    private static String setting(String systemProperty, String flagKey) {
        String value = System.getProperty(systemProperty);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        String fromFile = flagProperties.get(flagKey);
        return fromFile == null ? "" : fromFile.trim();
    }

    private static void loadFlagFile() {
        try {
            for (String line : Files.readAllLines(Path.of(FLAG_FILE))) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq > 0) {
                    flagProperties.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
                }
            }
        } catch (Exception ignored) {
            // маркер-файл не читается — просто включённый dev-режим
        }
    }

    private static String fileName(String from) {
        if (UpdateDownloader.isLocal(from)) {
            return UpdateDownloader.localPath(from).getFileName().toString();
        }
        int slash = from.lastIndexOf('/');
        return slash >= 0 ? from.substring(slash + 1) : from;
    }
}
