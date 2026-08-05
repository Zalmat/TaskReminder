package com.reminder.service;

import com.reminder.model.Task;
import com.reminder.model.WorkEntry;
import com.reminder.storage.DataStore;
import com.reminder.storage.LegacyMigrator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Доступ к данным о работе и предзаданных задачах (хранение в JSON/UTF-8). */
public class TaskService {

    private static final String ENTRIES_FILE = "work_entries.json";
    private static final String TASKS_FILE = "predefined_tasks.json";

    private final DataStore store;
    private List<WorkEntry> entries;
    private List<Task> predefinedTasks;

    public TaskService() {
        this.store = new DataStore();
        migrateLegacy();
        this.entries = store.loadList(ENTRIES_FILE, WorkEntry.class);
        this.predefinedTasks = store.loadList(TASKS_FILE, Task.class);
    }

    private void migrateLegacy() {
        LegacyMigrator migrator = new LegacyMigrator(store);
        migrator.migrate("work_entries.dat", ENTRIES_FILE);
        migrator.migrate("predefined_tasks.dat", TASKS_FILE);
    }

    public boolean addWorkEntry(WorkEntry entry) {
        if (entry == null || entry.getHours() <= 0) {
            return false;
        }
        if (getTotalHoursForDate(entry.getDate()) + entry.getHours() > WorkTimeService.MAX_DAILY_HOURS) {
            return false;
        }
        entries.add(entry);
        saveEntries();
        return true;
    }

    /** Заменяет существующую запись, сохраняя время создания. */
    public boolean updateWorkEntry(WorkEntry original, WorkEntry updated) {
        int idx = entries.indexOf(original);
        if (idx < 0) {
            return false;
        }
        if (updated.getHours() <= 0) {
            return false;
        }
        double otherTotal = entries.stream()
                .filter(e -> !e.equals(original) && e.getDate().equals(updated.getDate()))
                .mapToDouble(WorkEntry::getHours)
                .sum();
        if (otherTotal + updated.getHours() > WorkTimeService.MAX_DAILY_HOURS) {
            return false;
        }
        updated.setCreatedAt(original.getCreatedAt());
        entries.set(idx, updated);
        saveEntries();
        return true;
    }

    public boolean removeWorkEntry(WorkEntry entry) {
        boolean removed = entries.remove(entry);
        if (removed) {
            saveEntries();
        }
        return removed;
    }

    public double getTotalHoursForDate(LocalDate date) {
        return entries.stream()
                .filter(e -> e.getDate().equals(date))
                .mapToDouble(WorkEntry::getHours)
                .sum();
    }

    public double getRemainingHoursForDate(LocalDate date) {
        return WorkTimeService.MAX_DAILY_HOURS - getTotalHoursForDate(date);
    }

    public List<WorkEntry> getEntriesForDate(LocalDate date) {
        return entries.stream()
                .filter(e -> e.getDate().equals(date))
                .sorted(Comparator.comparing(WorkEntry::getCreatedAt))
                .collect(Collectors.toList());
    }

    public List<WorkEntry> getEntriesForDateRange(LocalDate start, LocalDate end) {
        return entries.stream()
                .filter(e -> !e.getDate().isBefore(start) && !e.getDate().isAfter(end))
                .sorted(Comparator.comparing(WorkEntry::getDate)
                        .thenComparing(WorkEntry::getCreatedAt))
                .collect(Collectors.toList());
    }

    public List<String> getAllProjects() {
        Set<String> projects = new HashSet<>();
        entries.forEach(e -> projects.add(e.getProject()));
        predefinedTasks.forEach(t -> projects.add(t.getProject()));
        return new ArrayList<>(projects);
    }

    public List<String> getAllTaskNames() {
        Set<String> taskNames = new HashSet<>();
        entries.forEach(e -> taskNames.add(e.getTaskName()));
        predefinedTasks.forEach(t -> taskNames.add(t.getTaskName()));
        return new ArrayList<>(taskNames);
    }

    public List<Task> getPredefinedTasks() {
        return new ArrayList<>(predefinedTasks);
    }

    public void setPredefinedTasks(List<Task> tasks) {
        this.predefinedTasks = new ArrayList<>(tasks);
        savePredefinedTasks();
    }

    public void addPredefinedTask(Task task) {
        if (task != null && !predefinedTasks.contains(task)) {
            predefinedTasks.add(task);
            savePredefinedTasks();
        }
    }

    public List<WorkEntry> getAllEntries() {
        return new ArrayList<>(entries);
    }

    public void clearEntriesForDate(LocalDate date) {
        entries.removeIf(e -> e.getDate().equals(date));
        saveEntries();
    }

    public void clearEntriesForDates(List<LocalDate> dates) {
        Set<LocalDate> set = new HashSet<>(dates);
        entries.removeIf(e -> set.contains(e.getDate()));
        saveEntries();
    }

    private void saveEntries() {
        store.saveList(ENTRIES_FILE, entries);
    }

    private void savePredefinedTasks() {
        store.saveList(TASKS_FILE, predefinedTasks);
    }
}