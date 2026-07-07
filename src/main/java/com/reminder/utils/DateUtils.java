package com.reminder.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class DateUtils {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek().getValue() >= 6;
    }

    public static boolean isHoliday(LocalDate date, Set<LocalDate> holidays) {
        return holidays.contains(date);
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