package com.reminder.i18n;

import java.util.Map;

/** Один языковой пакет: код, отображаемое имя, строки и состояние. */
public final class Language {

    private final String code;
    private final String displayName;
    private final Map<String, String> strings;
    private final LanguageStatus status;
    private final String source;
    private final String detail;
    private final int present;
    private final int total;

    public Language(String code, String displayName, Map<String, String> strings,
                    LanguageStatus status, String source, String detail, int present, int total) {
        this.code = code;
        this.displayName = displayName;
        this.strings = strings;
        this.status = status;
        this.source = source;
        this.detail = detail;
        this.present = present;
        this.total = total;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public Map<String, String> strings() {
        return strings;
    }

    public LanguageStatus status() {
        return status;
    }

    /** Путь к файлу пакета или пометка «встроенный». */
    public String source() {
        return source;
    }

    /** Причина повреждения (для тултипа). */
    public String detail() {
        return detail;
    }

    public int presentKeys() {
        return present;
    }

    public int totalKeys() {
        return total;
    }

    @Override
    public String toString() {
        return displayName + " (" + code + ")";
    }
}
