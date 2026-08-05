package com.reminder.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

public final class DateUtils {

    public static final Locale RUSSIAN = Locale.forLanguageTag("ru-RU");

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

    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek().getValue() >= 6;
    }

    public static boolean isHoliday(LocalDate date, Set<LocalDate> holidays) {
        return holidays != null && holidays.contains(date);
    }

    public static double calculateWorkingDays(LocalDate start, LocalDate end, Set<LocalDate> holidays) {
        double workingDays = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (!isWeekend(d) && !isHoliday(d, holidays)) {
                workingDays++;
            }
        }
        return workingDays;
    }
}
