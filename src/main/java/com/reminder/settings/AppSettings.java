package com.reminder.settings;

import com.reminder.i18n.I18n;

import java.util.prefs.Preferences;

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

    /** Десятичный разделитель для экспорта. */
    public static DecimalSeparator decimalSeparator() {
        return DecimalSeparator.fromKeyOrDefault(PREFS.get("decimalSeparator", DecimalSeparator.DOT.key()));
    }

    public static void setDecimalSeparator(DecimalSeparator separator) {
        PREFS.put("decimalSeparator", separator.key());
    }
}
