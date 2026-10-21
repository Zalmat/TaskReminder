package com.reminder.service;

import com.reminder.model.WeekEntry;
import com.reminder.model.WorkEntry;
import com.reminder.storage.DataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkTimeServiceTest {

    private WorkTimeService service(Path dir) {
        return new WorkTimeService(new DataStore(dir));
    }

    private WorkEntry entry(String project, String task, String type, double hours, LocalDate date) {
        WorkEntry e = new WorkEntry(project, task, type, hours);
        e.setDate(date);
        return e;
    }

    @Test
    void aggregateWeekMergesSameTripleAndSumsDayHours(@TempDir Path dir) {
        LocalDate mon = LocalDate.of(2026, 10, 12);
        WorkTimeService svc = service(dir);
        List<WorkEntry> entries = List.of(
                entry("P", "T", "Разработка", 3, mon),
                entry("P", "T", "Разработка", 2, mon),
                entry("P", "T", "Разработка", 4, mon.plusDays(1)));

        List<WeekEntry> week = svc.aggregateWeek(entries);
        assertEquals(1, week.size());
        WeekEntry we = week.get(0);
        assertEquals(5, we.getDayHours().get(mon));
        assertEquals(4, we.getDayHours().get(mon.plusDays(1)));
        assertEquals(9, we.getTotal());
        assertEquals(9.0, svc.getWeekTotal(week));
    }

    @Test
    void aggregateWeekKeepsDifferentTypesSeparate(@TempDir Path dir) {
        LocalDate d = LocalDate.of(2026, 10, 12);
        WorkTimeService svc = service(dir);
        List<WeekEntry> week = svc.aggregateWeek(List.of(
                entry("P", "T", "Разработка", 3, d),
                entry("P", "T", "Тестирование", 2, d)));
        assertEquals(2, week.size());
    }

    @Test
    void aggregateWeekKeepsDifferentProjectsSeparate(@TempDir Path dir) {
        LocalDate d = LocalDate.of(2026, 10, 12);
        WorkTimeService svc = service(dir);
        List<WeekEntry> week = svc.aggregateWeek(List.of(
                entry("P", "T", "Разработка", 3, d),
                entry("P2", "T", "Разработка", 2, d)));
        assertEquals(2, week.size());
    }

    @Test
    void aggregateWeekKeepsDifferentTaskNamesSeparate(@TempDir Path dir) {
        LocalDate d = LocalDate.of(2026, 10, 12);
        WorkTimeService svc = service(dir);
        List<WeekEntry> week = svc.aggregateWeek(List.of(
                entry("P", "T", "Разработка", 3, d),
                entry("P", "T2", "Разработка", 2, d)));
        assertEquals(2, week.size());
    }

    @Test
    void aggregateWeekKeepsLastNonEmptyComment(@TempDir Path dir) {
        LocalDate d = LocalDate.of(2026, 10, 12);
        WorkTimeService svc = service(dir);
        WorkEntry a = entry("P", "T", "Разработка", 1, d);
        WorkEntry b = entry("P", "T", "Разработка", 1, d);
        b.setComment("второй");

        List<WeekEntry> week = svc.aggregateWeek(List.of(a, b));
        assertEquals("второй", week.get(0).getComment());
    }

    @Test
    void aggregateWeekSkipsInvalidHours(@TempDir Path dir) {
        LocalDate d = LocalDate.of(2026, 10, 12);
        WorkTimeService svc = service(dir);
        WorkEntry good = entry("P", "T", "Дизайн", 3, d);
        WorkEntry negative = entry("P", "T", "Дизайн", -1, d);
        WorkEntry tooLarge = entry("P", "T", "Дизайн", 25, d);

        List<WeekEntry> week = svc.aggregateWeek(List.of(negative, tooLarge, good));
        assertEquals(1, week.size());
        assertEquals(3, week.get(0).getTotal());
    }

    @Test
    void dayTotalWithinLimitBoundaries(@TempDir Path dir) {
        LocalDate d = LocalDate.of(2026, 10, 12);
        WorkTimeService svc = service(dir);
        WeekEntry we = new WeekEntry("P", "T", "Разработка");
        we.getDayHours().put(d, 5);
        List<WeekEntry> week = List.of(we);

        assertTrue(svc.dayTotalWithinLimit(week, d, 8, 5));  // 5 - 5 + 8 = 8
        assertFalse(svc.dayTotalWithinLimit(week, d, 9, 5)); // 9 > 8
    }

    @Test
    void dayTotalsAndOvertimeChecks(@TempDir Path dir) {
        LocalDate d = LocalDate.of(2026, 10, 12);
        WorkTimeService svc = service(dir);
        List<WorkEntry> entries = List.of(entry("P", "T", "Разработка", 6, d));

        assertEquals(6.0, svc.getDayTotal(entries, d));
        assertEquals(2.0, svc.getRemainingForDay(entries, d));
        assertFalse(svc.wouldExceedDayLimit(entries, d, 2));
        assertTrue(svc.wouldExceedDayLimit(entries, d, 3));
    }
}