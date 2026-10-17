package com.reminder.util;

import com.reminder.i18n.I18n;

import java.time.DayOfWeek;

/** Названия дней недели (локализуются через {@link I18n}). */
public final class RussianDays {

    private RussianDays() {
    }

    public static String fullName(DayOfWeek day) {
        if (day == null) return "";
        return fullName(day.getValue());
    }

    public static String shortName(DayOfWeek day) {
        if (day == null) return "";
        return shortName(day.getValue());
    }

    public static String fullName(int dayOfWeekValue) {
        return (dayOfWeekValue >= 1 && dayOfWeekValue <= 7)
                ? I18n.get("day." + dayOfWeekValue + ".full") : "";
    }

    public static String shortName(int dayOfWeekValue) {
        return (dayOfWeekValue >= 1 && dayOfWeekValue <= 7)
                ? I18n.get("day." + dayOfWeekValue + ".short") : "";
    }
}
