package com.reminder.model;

/** Источник записи праздника. */
public enum HolidaySource {
    MANUAL,
    IMPORTED,
    DEFAULT;

    public static HolidaySource fromCode(String value) {
        if (value == null) {
            return MANUAL;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MANUAL;
        }
    }
}
