package com.reminder.settings;

import java.util.Locale;

/** Десятичный разделитель, применяемый при экспорте данных. */
public enum DecimalSeparator {

    SYSTEM("system"),
    DOT("dot"),
    COMMA("comma");

    private final String key;

    DecimalSeparator(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static DecimalSeparator fromKeyOrDefault(String key) {
        if (key != null) {
            for (DecimalSeparator separator : values()) {
                if (separator.key.equals(key)) {
                    return separator;
                }
            }
        }
        return DOT;
    }

    /** Символ-разделитель ('\0' — использовать локаль системы). */
    public char sign() {
        return switch (this) {
            case DOT -> '.';
            case COMMA -> ',';
            default -> '\0';
        };
    }

    /** Форматирует число для экспорта в соответствии с настройкой. */
    public String format(double value, int fractionDigits) {
        char sign = sign();
        if (sign == '\0') {
            return String.format(Locale.getDefault(), "%." + fractionDigits + "f", value);
        }
        String formatted = String.format(Locale.ROOT, "%." + fractionDigits + "f", value);
        return sign == ',' ? formatted.replace('.', ',') : formatted;
    }
}
