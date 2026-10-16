package com.reminder.settings;

/** Тема оформления приложения и её ресурс-стиль. */
public enum Theme {

    LIGHT("/styles/style-light.css"),
    DARK("/styles/style-dark.css");

    private final String stylesheet;

    Theme(String stylesheet) {
        this.stylesheet = stylesheet;
    }

    public String stylesheet() {
        return stylesheet;
    }

    /** Возвращает тему по имени; при неизвестном имени — {@link #LIGHT}. */
    public static Theme fromNameOrDefault(String name) {
        if (name != null) {
            for (Theme theme : values()) {
                if (theme.name().equals(name)) {
                    return theme;
                }
            }
        }
        return LIGHT;
    }
}