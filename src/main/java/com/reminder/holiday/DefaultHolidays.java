package com.reminder.holiday;

import com.reminder.model.Holiday;
import com.reminder.model.HolidaySource;
import com.reminder.model.HolidayType;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class DefaultHolidays {

    public record Entry(MonthDay day, String name) {
    }

    private static final List<Entry> ENTRIES = List.of(
            new Entry(MonthDay.of(1, 1), "Новый год"),
            new Entry(MonthDay.of(1, 7), "Рождество Христово"),
            new Entry(MonthDay.of(2, 23), "День защитника Отечества"),
            new Entry(MonthDay.of(3, 8), "Международный женский день"),
            new Entry(MonthDay.of(5, 1), "Праздник Весны и Труда"),
            new Entry(MonthDay.of(5, 9), "День Победы"),
            new Entry(MonthDay.of(6, 12), "День России"),
            new Entry(MonthDay.of(11, 4), "День народного единства")
    );

    private static final Map<MonthDay, Entry> BY_DAY = ENTRIES.stream()
            .collect(Collectors.toUnmodifiableMap(Entry::day, entry -> entry));

    private DefaultHolidays() {
    }

    public static List<Entry> all() {
        return ENTRIES;
    }

    public static Optional<Entry> find(LocalDate date) {
        return date == null
                ? Optional.empty()
                : Optional.ofNullable(BY_DAY.get(MonthDay.from(date)));
    }

    public static Holiday create(LocalDate date, Entry entry) {
        return new Holiday(date, entry.name(), true, HolidayType.OFFICIAL, HolidaySource.DEFAULT);
    }
}
