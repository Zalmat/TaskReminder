package com.reminder.service;

import com.reminder.model.Task;
import com.reminder.model.WeekEntry;
import com.reminder.model.WorkEntry;
import com.reminder.storage.DataStore;
import com.reminder.storage.FailingDataStore;
import com.reminder.storage.StorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskServiceTest {

    private TaskService service(Path dir) {
        return new TaskService(new DataStore(dir));
    }

    private WorkEntry entry(String task, double hours, LocalDate date) {
        WorkEntry e = new WorkEntry("P", task, "Разработка", hours);
        e.setDate(date);
        return e;
    }

    @Test
    void addWithinLimitAndOvertime(@TempDir Path dir) {
        TaskService s = service(dir);
        LocalDate d = LocalDate.of(2026, 10, 12);

        assertTrue(s.addWorkEntry(entry("A", 6, d), false));
        assertFalse(s.addWorkEntry(entry("B", 6, d), false)); // 12 > 8 без переработки
        assertTrue(s.addWorkEntry(entry("B", 6, d), true));   // с переработкой
        assertEquals(12.0, s.getTotalHoursForDate(d));
    }

    @Test
    void updateReducingInsideExistingOvertimeIsAllowed(@TempDir Path dir) {
        TaskService s = service(dir);
        LocalDate d = LocalDate.of(2026, 10, 12);
        WorkEntry a = entry("A", 6, d);
        s.addWorkEntry(a, false);
        s.addWorkEntry(entry("B", 6, d), true);

        assertTrue(s.updateWorkEntry(a, entry("A", 4, d), false)); // уменьшение -> можно
        assertEquals(10.0, s.getTotalHoursForDate(d));
    }

    @Test
    void updateIncreasingBeyondCurrentNeedsOvertime(@TempDir Path dir) {
        TaskService s = service(dir);
        LocalDate d = LocalDate.of(2026, 10, 12);
        WorkEntry a = entry("A", 6, d);
        s.addWorkEntry(a, false);
        s.addWorkEntry(entry("B", 6, d), true);

        assertFalse(s.updateWorkEntry(a, entry("A", 8, d), false)); // рост -> блок
        assertTrue(s.updateWorkEntry(a, entry("A", 8, d), true));   // рост с переработкой
    }

    @Test
    void removeAndClear(@TempDir Path dir) {
        TaskService s = service(dir);
        LocalDate d = LocalDate.of(2026, 10, 12);
        WorkEntry a = entry("A", 3, d);
        s.addWorkEntry(a, false);
        s.addWorkEntry(entry("B", 2, d), false);

        assertTrue(s.removeWorkEntry(a));
        assertEquals(2.0, s.getTotalHoursForDate(d));

        s.clearEntriesForDate(d);
        assertEquals(0.0, s.getTotalHoursForDate(d));
    }

    @Test
    void predefinedTasksDedupAndAggregatedChoices(@TempDir Path dir) {
        TaskService s = service(dir);
        LocalDate d = LocalDate.of(2026, 10, 12);
        s.addWorkEntry(entry("T1", 2, d), false);
        s.addPredefinedTask(new Task("P2", "T2", "Дизайн"));
        s.addPredefinedTask(new Task("P2", "T2", "Дизайн")); // дубль

        assertTrue(s.getAllProjects().containsAll(List.of("P", "P2")));
        assertTrue(s.getAllTaskNames().containsAll(List.of("T1", "T2")));
    }

    @Test
    void rejectsInvalidHours(@TempDir Path dir) {
        TaskService s = service(dir);
        LocalDate d = LocalDate.of(2026, 10, 12);

        assertFalse(s.addWorkEntry(entry("A", -1, d), true));
        assertFalse(s.addWorkEntry(entry("A", 0, d), true));
        assertFalse(s.addWorkEntry(entry("A", 25, d), true));

        WorkEntry original = entry("A", 2, d);
        assertTrue(s.addWorkEntry(original, false));
        assertFalse(s.updateWorkEntry(original, entry("A", -1, d), true));
        assertFalse(s.updateWorkEntry(original, entry("A", 25, d), true));
    }

    @Test
    void failedSaveKeepsEntriesUnchanged(@TempDir Path dir) {
        FailingDataStore store = new FailingDataStore(dir);
        TaskService s = new TaskService(store);
        LocalDate d = LocalDate.of(2026, 10, 12);
        WorkEntry first = entry("A", 3, d);
        assertTrue(s.addWorkEntry(first, false));

        store.failSaves(true);
        assertThrows(StorageException.class, () -> s.addWorkEntry(entry("B", 2, d), false));
        assertEquals(1, s.getAllEntries().size());
        assertTrue(s.getAllEntries().contains(first));

        assertThrows(StorageException.class,
                () -> s.updateWorkEntry(first, entry("A", 4, d), true));
        assertEquals(3.0, s.getAllEntries().get(0).getHours());

        assertThrows(StorageException.class, () -> s.removeWorkEntry(first));
        assertEquals(1, s.getAllEntries().size());

        WeekEntry next = new WeekEntry("P", "A", "Разработка");
        next.getDayHours().put(d, 5);
        assertThrows(StorageException.class,
                () -> s.replaceWeekEntry(null, next, d, d));
        assertEquals(1, s.getAllEntries().size());
    }

    @Test
    void replaceWeekEntryCommitsAllDaysAtOnce(@TempDir Path dir) {
        TaskService s = service(dir);
        LocalDate start = LocalDate.of(2026, 10, 12);
        LocalDate end = start.plusDays(6);
        WeekEntry week = new WeekEntry("P", "T", "Разработка");
        week.setComment("comment");
        week.getDayHours().put(start, 2);
        week.getDayHours().put(start.plusDays(2), 3);

        assertTrue(s.replaceWeekEntry(null, week, start, end));
        assertEquals(2, s.getAllEntries().size());
        assertEquals(5.0, s.getTotalHoursForDate(start)
                + s.getTotalHoursForDate(start.plusDays(2)));

        WeekEntry reloaded = new WeekEntry("P", "T", "Разработка");
        reloaded.getDayHours().put(start, 4);
        assertTrue(s.replaceWeekEntry(week, reloaded, start, end));
        assertEquals(1, s.getAllEntries().size());
        assertEquals(4.0, s.getTotalHoursForDate(start));
        assertEquals(0.0, s.getTotalHoursForDate(start.plusDays(2)));
    }
}