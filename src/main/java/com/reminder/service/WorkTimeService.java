package com.reminder.service;

import com.reminder.model.WeekEntry;
import com.reminder.model.WorkEntry;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Бизнес-логика учёта рабочего времени: лимиты, итоги и агрегация недели.
 * Слой не зависит от UI.
 */
public class WorkTimeService {

    public static final double MAX_DAILY_HOURS = 8.0;
    public static final double MAX_WEEKLY_HOURS = 48.0;

    private final Set<LocalDate> holidays = new TreeSet<>();

    // ==================== Праздники ====================

    public Set<LocalDate> getHolidays() {
        return new TreeSet<>(holidays);
    }

    public boolean isHoliday(LocalDate date) {
        return holidays.contains(date);
    }

    public boolean addHoliday(LocalDate date) {
        return date != null && holidays.add(date);
    }

    public boolean removeHoliday(LocalDate date) {
        return date != null && holidays.remove(date);
    }

    public void setDefaultHolidays() {
        holidays.clear();
        holidays.add(LocalDate.of(2026, 1, 1));
        holidays.add(LocalDate.of(2026, 1, 7));
        holidays.add(LocalDate.of(2026, 2, 23));
        holidays.add(LocalDate.of(2026, 3, 8));
        holidays.add(LocalDate.of(2026, 5, 1));
        holidays.add(LocalDate.of(2026, 5, 9));
        holidays.add(LocalDate.of(2026, 6, 12));
        holidays.add(LocalDate.of(2026, 11, 4));
    }

    // ==================== Итоги ====================

    public double getDayTotal(List<WorkEntry> entries, LocalDate date) {
        if (entries == null || date == null) return 0;
        return entries.stream()
                .filter(e -> date.equals(e.getDate()))
                .mapToDouble(WorkEntry::getHours)
                .sum();
    }

    public double getRemainingForDay(List<WorkEntry> entries, LocalDate date) {
        return MAX_DAILY_HOURS - getDayTotal(entries, date);
    }

    public double getWeekTotal(List<WeekEntry> weekEntries) {
        if (weekEntries == null) return 0;
        return weekEntries.stream().mapToDouble(WeekEntry::getTotal).sum();
    }

    public boolean wouldExceedDayLimit(List<WorkEntry> entries, LocalDate date, double additionalHours) {
        return getDayTotal(entries, date) + additionalHours > MAX_DAILY_HOURS;
    }

    // ==================== Агрегация недели ====================

    /**
     * Группирует рабочие записи недели по задаче (проект + задача + тип)
     * и собирает часы по дням в {@link WeekEntry}.
     */
    public List<WeekEntry> aggregateWeek(List<WorkEntry> weekEntries) {
        Map<String, WeekEntry> map = new LinkedHashMap<>();
        for (WorkEntry entry : weekEntries) {
            String key = entry.getProject() + "|" + entry.getTaskName() + "|" + entry.getType();
            WeekEntry weekEntry = map.get(key);
            if (weekEntry == null) {
                weekEntry = new WeekEntry(entry.getProject(), entry.getTaskName(), entry.getType());
                map.put(key, weekEntry);
            }
            int hours = (int) entry.getHours();
            weekEntry.getDayHours().merge(entry.getDate(), hours, Integer::sum);
            if (entry.getComment() != null && !entry.getComment().isEmpty()) {
                weekEntry.setComment(entry.getComment());
            }
        }
        return new ArrayList<>(map.values());
    }

    /** Проверка, что на день после добавления не превышен лимит 8 часов. */
    public boolean dayTotalWithinLimit(List<WeekEntry> weekEntries, LocalDate day, int newValue, int oldValue) {
        int currentDayTotal = 0;
        for (WeekEntry we : weekEntries) {
            currentDayTotal += we.getDayHours().getOrDefault(day, 0);
        }
        return currentDayTotal - oldValue + newValue <= MAX_DAILY_HOURS;
    }
}