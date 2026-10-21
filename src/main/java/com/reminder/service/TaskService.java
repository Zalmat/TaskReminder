package com.reminder.service;

import com.reminder.catalog.TaskCatalogService;
import com.reminder.model.Task;
import com.reminder.model.WeekEntry;
import com.reminder.model.WorkEntry;
import com.reminder.storage.PredefinedTaskRepository;
import com.reminder.storage.StorageException;
import com.reminder.storage.StorageRepositories;
import com.reminder.storage.WorkEntryRepository;
import com.reminder.util.Hours;
import com.reminder.util.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Доступ к данным о работе и предзаданных задачах (хранение в JSON/UTF-8). */
public class TaskService {

    private final WorkEntryRepository entryRepository;
    private final PredefinedTaskRepository predefinedTaskRepository;
    private final TaskCatalogService catalog;
    private List<WorkEntry> entries;
    private List<Task> predefinedTasks;

    public TaskService(StorageRepositories repositories) {
        this.entryRepository = repositories.workEntries();
        this.predefinedTaskRepository = repositories.predefinedTasks();
        this.catalog = new TaskCatalogService(repositories.catalog());
        this.entries = entryRepository.load();
        this.predefinedTasks = predefinedTaskRepository.load();
    }

    /** Добавляет запись. При allowOvertime пропускается проверка дневного лимита. */
    public boolean addWorkEntry(WorkEntry entry, boolean allowOvertime) {
        if (entry == null || !Hours.validEntry(entry.getHours())) {
            return false;
        }
        if (!allowOvertime
                && getTotalHoursForDate(entry.getDate()) + entry.getHours() > WorkTimeService.MAX_DAILY_HOURS) {
            return false;
        }
        List<WorkEntry> next = new ArrayList<>(entries);
        next.add(entry);
        entryRepository.save(next);
        entries = next;
        return true;
    }

    /** Заменяет запись. При allowOvertime пропускается проверка дневного лимита. */
    public boolean updateWorkEntry(WorkEntry original, WorkEntry updated, boolean allowOvertime) {
        int idx = entries.indexOf(original);
        if (idx < 0) {
            return false;
        }
        if (!Hours.validEntry(updated.getHours())) {
            return false;
        }
        double otherTotal = entries.stream()
                .filter(e -> !e.equals(original) && e.getDate().equals(updated.getDate()))
                .mapToDouble(WorkEntry::getHours)
                .sum();
        double newTotal = otherTotal + updated.getHours();
        double currentTotal = otherTotal + original.getHours();
        if (!allowOvertime && newTotal > WorkTimeService.MAX_DAILY_HOURS && newTotal > currentTotal) {
            return false;
        }
        updated.setCreatedAt(original.getCreatedAt());
        List<WorkEntry> next = new ArrayList<>(entries);
        next.set(idx, updated);
        entryRepository.save(next);
        entries = next;
        return true;
    }

    public boolean removeWorkEntry(WorkEntry entry) {
        List<WorkEntry> next = new ArrayList<>(entries);
        if (!next.remove(entry)) {
            return false;
        }
        entryRepository.save(next);
        entries = next;
        return true;
    }

    public double getTotalHoursForDate(LocalDate date) {
        return entries.stream()
                .filter(e -> e.getDate().equals(date))
                .mapToDouble(WorkEntry::getHours)
                .sum();
    }

    public List<WorkEntry> getEntriesForDate(LocalDate date) {
        return entries.stream()
                .filter(e -> e.getDate().equals(date))
                .sorted(Comparator.comparing(WorkEntry::getCreatedAt))
                .collect(Collectors.toList());
    }

    public List<WorkEntry> getAllEntries() {
        return new ArrayList<>(entries);
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
        projects.addAll(catalog.getProjects());
        return new ArrayList<>(projects);
    }

    public List<String> getAllTaskNames() {
        Set<String> taskNames = new HashSet<>();
        entries.forEach(e -> taskNames.add(e.getTaskName()));
        predefinedTasks.forEach(t -> taskNames.add(t.getTaskName()));
        taskNames.addAll(catalog.getTaskNames());
        return new ArrayList<>(taskNames);
    }

    /** Типы работ для автоподстановки (из каталога). */
    public List<String> getWorkTypes() {
        return catalog.getTypes();
    }

    /** Засевает типы по умолчанию, если каталог типов пуст. */
    public void ensureDefaultTypes(List<String> defaults) {
        catalog.ensureDefaultTypes(defaults);
    }

    public TaskCatalogService getCatalog() {
        return catalog;
    }

    public void addPredefinedTask(Task task) {
        if (task == null || predefinedTasks.contains(task)) {
            return;
        }
        List<Task> next = new ArrayList<>(predefinedTasks);
        next.add(task);
        try {
            predefinedTaskRepository.save(next);
            predefinedTasks = next;
        } catch (StorageException e) {
            // Это вторичный кэш для подсказок, основная запись уже сохранена.
            Log.log("TaskService", "cannot save predefined tasks: " + e.getMessage(), e);
        }
    }

    public void clearEntriesForDate(LocalDate date) {
        List<WorkEntry> next = new ArrayList<>(entries);
        next.removeIf(e -> e.getDate().equals(date));
        if (next.size() == entries.size()) {
            return;
        }
        entryRepository.save(next);
        entries = next;
    }

    public void clearEntriesForDates(List<LocalDate> dates) {
        Set<LocalDate> set = new HashSet<>(dates);
        List<WorkEntry> next = new ArrayList<>(entries);
        next.removeIf(e -> set.contains(e.getDate()));
        if (next.size() == entries.size()) {
            return;
        }
        entryRepository.save(next);
        entries = next;
    }

    /**
     * Заменяет все записи недели для пары «старая/новая задача» одним сохранением.
     * Используется редактированием недельной таблицы, где нельзя сохранять строки по одной.
     */
    public boolean replaceWeekEntry(WeekEntry previous, WeekEntry next,
                                    LocalDate start, LocalDate end) {
        if (next == null || start == null || end == null || start.isAfter(end)) {
            return false;
        }
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            Integer hours = next.getDayHours().get(day);
            if (hours != null && !Hours.validWeekCell(hours)) {
                return false;
            }
        }

        List<WorkEntry> candidate = new ArrayList<>(entries);
        candidate.removeIf(e -> inRange(e, start, end)
                && (matches(e, previous) || matches(e, next)));
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            int hours = next.getDayHours().getOrDefault(day, 0);
            if (hours <= 0) {
                continue;
            }
            WorkEntry entry = new WorkEntry(next.getProject(), next.getTaskName(),
                    next.getType(), hours);
            entry.setDate(day);
            entry.setComment(next.getComment());
            candidate.add(entry);
        }
        entryRepository.save(candidate);
        entries = candidate;
        return true;
    }

    /** Удаляет все записи недели для одной задачи одним сохранением. */
    public boolean deleteWeekEntry(WeekEntry entry, LocalDate start, LocalDate end) {
        if (entry == null || start == null || end == null || start.isAfter(end)) {
            return false;
        }
        List<WorkEntry> candidate = new ArrayList<>(entries);
        candidate.removeIf(e -> inRange(e, start, end) && matches(e, entry));
        if (candidate.size() == entries.size()) {
            return false;
        }
        entryRepository.save(candidate);
        entries = candidate;
        return true;
    }

    private static boolean inRange(WorkEntry entry, LocalDate start, LocalDate end) {
        return entry.getDate() != null
                && !entry.getDate().isBefore(start)
                && !entry.getDate().isAfter(end);
    }

    private static boolean matches(WorkEntry entry, WeekEntry weekEntry) {
        return weekEntry != null
                && Objects.equals(entry.getProject(), weekEntry.getProject())
                && Objects.equals(entry.getTaskName(), weekEntry.getTaskName())
                && Objects.equals(entry.getType(), weekEntry.getType());
    }
}