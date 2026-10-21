package com.reminder.model;

/** Тип события в календаре праздников. */
public enum HolidayType {
    OFFICIAL("official", "holidays.type.official"),
    CORPORATE("corporate", "holidays.type.corporate"),
    PERSONAL("personal", "holidays.type.personal"),
    OTHER("other", "holidays.type.other");

    private final String code;
    private final String labelKey;

    HolidayType(String code, String labelKey) {
        this.code = code;
        this.labelKey = labelKey;
    }

    public String code() {
        return code;
    }

    public String labelKey() {
        return labelKey;
    }

    /** Код из файла; null для неизвестного значения. */
    public static HolidayType fromCode(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        for (HolidayType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
