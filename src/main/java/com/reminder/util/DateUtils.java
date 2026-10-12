package com.reminder.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter COMPACT_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM");

    private DateUtils() {
    }

    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    public static String formatShort(LocalDate date) {
        return date != null ? date.format(COMPACT_FORMATTER) : "";
    }

    /** Начало недели — всегда понедельник (не зависит от локали системы). */
    public static LocalDate startOfWeek(LocalDate date) {
        return date.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
