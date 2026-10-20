package com.reminder.suggest;

/** Нормализация значений для поиска и дедупликации. */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /**
     * Форма для поиска: нижний регистр и ё→е. Длина сохраняется,
     * поэтому позиция совпадения пригодна для подсветки исходного значения.
     */
    public static String matchForm(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = Character.toLowerCase(value.charAt(i));
            if (ch == 'ё') {
                ch = 'е';
            }
            result.append(ch);
        }
        return result.toString();
    }

    /** Ключ дедупликации: форма поиска без лишних пробелов. */
    public static String key(String value) {
        return matchForm(value).trim().replaceAll("\\s+", " ");
    }
}
