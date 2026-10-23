package com.reminder.settings;

import com.reminder.i18n.I18n;
import com.reminder.service.ExportFormat;
import com.reminder.util.Log;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/** Пользовательские настройки приложения (хранятся в Preferences). */
public final class AppSettings {

    private static final Preferences PREFS = Preferences.userRoot().node("com/reminder");

    private AppSettings() {
    }

    public static Theme theme() {
        return Theme.fromNameOrDefault(PREFS.get("theme", Theme.LIGHT.name()));
    }

    public static void setTheme(Theme theme) {
        PREFS.put("theme", theme.name());
    }

    public static String defaultType() {
        return PREFS.get("defaultType", I18n.get("worktype.development"));
    }

    public static void setDefaultType(String type) {
        if (type != null && !type.isBlank()) {
            PREFS.put("defaultType", type);
        }
    }

    /** Код выбранного языка интерфейса. */
    public static String language() {
        return PREFS.get("language", "ru");
    }

    public static void setLanguage(String code) {
        if (code != null && !code.isBlank()) {
            PREFS.put("language", code);
        }
    }

    /** Принудительно сохраняет настройки перед перезапуском или завершением. */
    public static void flush() {
        try {
            PREFS.flush();
        } catch (BackingStoreException e) {
            Log.log("Settings", "preferences flush failed: " + e.getMessage(), e);
        }
    }

    /** Десятичный разделитель для экспорта. */
    public static DecimalSeparator decimalSeparator() {
        return DecimalSeparator.fromKeyOrDefault(PREFS.get("decimalSeparator", DecimalSeparator.DOT.key()));
    }

    public static void setDecimalSeparator(DecimalSeparator separator) {
        PREFS.put("decimalSeparator", separator.key());
    }

    /** Абсолютный путь к папке для отчётов (пусто = не задано). */
    public static String exportDirectory() {
        return PREFS.get("exportDirectory", "");
    }

    public static void setExportDirectory(String path) {
        PREFS.put("exportDirectory", path == null ? "" : path);
    }

    /** Режим использования указанной папки. */
    public static ExportSaveMode exportSaveMode() {
        return ExportSaveMode.fromKeyOrDefault(PREFS.get("exportSaveMode", ExportSaveMode.SUGGEST.key()));
    }

    public static void setExportSaveMode(ExportSaveMode mode) {
        PREFS.put("exportSaveMode", mode.key());
    }

    /** Форматы, к которым применяется указанная папка. */
    public static Set<ExportFormat> exportFolderFormats() {
        return parseFormats(PREFS.get("exportFolderFormats", ""));
    }

    public static void setExportFolderFormats(Set<ExportFormat> formats) {
        PREFS.put("exportFolderFormats", joinFormats(formats));
    }

    /** Открывать папку после автосохранения. */
    public static boolean exportOpenFolder() {
        return Boolean.parseBoolean(PREFS.get("exportOpenFolder", "false"));
    }

    public static void setExportOpenFolder(boolean value) {
        PREFS.put("exportOpenFolder", Boolean.toString(value));
    }

    /** Автоматическая проверка обновлений (при запуске и ежедневно). */
    public static boolean updateCheckEnabled() {
        return Boolean.parseBoolean(PREFS.get("updateCheckEnabled", "true"));
    }

    public static void setUpdateCheckEnabled(boolean value) {
        PREFS.put("updateCheckEnabled", Boolean.toString(value));
    }

    public static int updateCheckHour() {
        return PREFS.getInt("updateCheckHour", 2);
    }

    public static int updateCheckMinute() {
        return PREFS.getInt("updateCheckMinute", 0);
    }

    public static void setUpdateCheckTime(int hour, int minute) {
        PREFS.putInt("updateCheckHour", hour);
        PREFS.putInt("updateCheckMinute", minute);
    }

    /** Путь к YAML-файлу со списком категорий (пусто = не задан). */
    public static String taskImportFile() {
        return PREFS.get("taskImportFile", "");
    }

    public static void setTaskImportFile(String path) {
        PREFS.put("taskImportFile", path == null ? "" : path);
    }

    /** Импортировать категории при запуске. */
    public static boolean taskImportOnStartup() {
        return Boolean.parseBoolean(PREFS.get("taskImportOnStartup", "false"));
    }

    public static void setTaskImportOnStartup(boolean value) {
        PREFS.put("taskImportOnStartup", Boolean.toString(value));
    }

    /** Импортировать категории ежедневно. */
    public static boolean taskImportScheduleEnabled() {
        return Boolean.parseBoolean(PREFS.get("taskImportScheduleEnabled", "false"));
    }

    public static void setTaskImportScheduleEnabled(boolean value) {
        PREFS.put("taskImportScheduleEnabled", Boolean.toString(value));
    }

    public static int taskImportHour() {
        return PREFS.getInt("taskImportHour", 9);
    }

    public static int taskImportMinute() {
        return PREFS.getInt("taskImportMinute", 0);
    }

    public static void setTaskImportTime(int hour, int minute) {
        PREFS.putInt("taskImportHour", hour);
        PREFS.putInt("taskImportMinute", minute);
    }

    /** Показывать уведомление в трее после авто-импорта. */
    public static boolean taskImportTrayNotify() {
        return Boolean.parseBoolean(PREFS.get("taskImportTrayNotify", "true"));
    }

    public static void setTaskImportTrayNotify(boolean value) {
        PREFS.put("taskImportTrayNotify", Boolean.toString(value));
    }

    /** Глубина истории для подсказок в месяцах; 0 — вся история. */
    public static int suggestionHistoryMonths() {
        return PREFS.getInt("suggestionHistoryMonths", 24);
    }

    public static void setSuggestionHistoryMonths(int months) {
        PREFS.putInt("suggestionHistoryMonths", Math.max(0, months));
    }

    /** Индексировать историю только когда окно неактивно/свёрнуто. */
    public static boolean indexOnlyWhenInactive() {
        return Boolean.parseBoolean(PREFS.get("indexOnlyWhenInactive", "true"));
    }

    public static void setIndexOnlyWhenInactive(boolean value) {
        PREFS.put("indexOnlyWhenInactive", Boolean.toString(value));
    }

    /** Путь к YAML-файлу календаря праздников (пусто = не задан). */
    public static String holidayCalendarFile() {
        return PREFS.get("holidayCalendarFile", "");
    }

    public static void setHolidayCalendarFile(String path) {
        PREFS.put("holidayCalendarFile", path == null ? "" : path);
    }

    /** Указанная папка, если она задана и существует. */
    public static Optional<Path> validExportDirectory() {
        String directory = exportDirectory();
        if (directory == null || directory.isBlank()) {
            return Optional.empty();
        }
        Path path = Path.of(directory);
        return Files.isDirectory(path) && Files.isWritable(path)
                ? Optional.of(path) : Optional.empty();
    }

    static Set<ExportFormat> parseFormats(String raw) {
        Set<ExportFormat> result = EnumSet.noneOf(ExportFormat.class);
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String part : raw.split(",")) {
            String name = part.trim();
            if (name.isEmpty()) {
                continue;
            }
            try {
                result.add(ExportFormat.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // неизвестный формат — пропускаем
            }
        }
        return result;
    }

    static String joinFormats(Set<ExportFormat> formats) {
        if (formats == null || formats.isEmpty()) {
            return "";
        }
        return formats.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }
}
