package com.reminder.util;

import java.time.DayOfWeek;

/** Русские названия дней недели. */
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
        switch (dayOfWeekValue) {
            case 1: return "Понедельник";
            case 2: return "Вторник";
            case 3: return "Среда";
            case 4: return "Четверг";
            case 5: return "Пятница";
            case 6: return "Суббота";
            case 7: return "Воскресенье";
            default: return "";
        }
    }

    public static String shortName(int dayOfWeekValue) {
        switch (dayOfWeekValue) {
            case 1: return "Пн";
            case 2: return "Вт";
            case 3: return "Ср";
            case 4: return "Чт";
            case 5: return "Пт";
            case 6: return "Сб";
            case 7: return "Вс";
            default: return "";
        }
    }
}
