package com.reminder.service;

import com.reminder.model.Task;
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
}