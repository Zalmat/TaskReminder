package com.reminder.settings;

import java.util.prefs.Preferences;

/** Пользовательские настройки приложения (хранятся в Preferences). */
public final class AppSettings {

    private static final Preferences PREFS = Preferences.userRoot().node("com/reminder");

    public static final String DEFAULT_WORK_TYPE = "Разработка";

    private AppSettings() {
    }

    public static Theme theme() {
        return Theme.fromNameOrDefault(PREFS.get("theme", Theme.LIGHT.name()));
    }

    public static void setTheme(Theme theme) {
        PREFS.put("theme", theme.name());
    }

    public static String defaultType() {
        return PREFS.get("defaultType", DEFAULT_WORK_TYPE);
    }

    public static void setDefaultType(String type) {
        if (type != null && !type.isBlank()) {
            PREFS.put("defaultType", type);
        }
    }
}