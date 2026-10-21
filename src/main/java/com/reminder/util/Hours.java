package com.reminder.util;

import java.util.OptionalInt;

/** Допустимые значения часов. */
public final class Hours {

    public static final int MIN_ENTRY = 1;
    public static final int MAX = 24;

    private Hours() {
    }

    /** Часы в записи: больше 0 и не больше 24. */
    public static boolean validEntry(double hours) {
        return hours >= MIN_ENTRY && hours <= MAX;
    }

    /** Часы в ячейке недели: 0 (очистить день) или 1–24. */
    public static boolean validWeekCell(int hours) {
        return hours >= 0 && hours <= MAX;
    }

    /**
     * Разбор значения ячейки недели: пусто = 0, иначе целое 0–24.
     * Некорректные значения и отрицательные — пустой Optional.
     */
    public static OptionalInt parseWeekCell(String text) {
        if (text == null || text.trim().isEmpty()) {
            return OptionalInt.of(0);
        }
        try {
            int value = Integer.parseInt(text.trim());
            return validWeekCell(value) ? OptionalInt.of(value) : OptionalInt.empty();
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }
}
