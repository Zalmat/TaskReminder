package com.reminder.services;

import com.reminder.models.Task;
import com.reminder.models.WorkEntry;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class TaskService {
    private List<WorkEntry> entries = new ArrayList<>();
    private List<Task> predefinedTasks = new ArrayList<>();
    private static final String DATA_FILE = "work_entries.dat";
    private static final String TASKS_FILE = "predefined_tasks.dat";
    public static final double MAX_HOURS_PER_DAY = 8.0;

    public TaskService() {
        loadEntries();
        loadPredefinedTasks();
    }

    public boolean addWorkEntry(WorkEntry entry) {
        if (entry.getHours() <= 0) {
            return false;
        }

        double totalHours = getTotalHoursForDate(entry.getDate());
        if (totalHours + entry.getHours() > MAX_HOURS_PER_DAY) {
            return false;
        }

        entries.add(entry);
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
        return MAX_HOURS_PER_DAY - getTotalHoursForDate(date);
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
        if (!predefinedTasks.contains(task)) {
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

    private void saveEntries() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(DATA_FILE))) {
            oos.writeObject(entries);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения записей: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadEntries() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(file))) {
                entries = (List<WorkEntry>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                entries = new ArrayList<>();
                System.err.println("Ошибка загрузки записей: " + e.getMessage());
            }
        }
    }

    private void savePredefinedTasks() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(TASKS_FILE))) {
            oos.writeObject(predefinedTasks);
        } catch (IOException e) {
            System.err.println("Ошибка сохранения задач: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadPredefinedTasks() {
        File file = new File(TASKS_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(file))) {
                predefinedTasks = (List<Task>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                predefinedTasks = new ArrayList<>();
                System.err.println("Ошибка загрузки задач: " + e.getMessage());
            }
        }
    }
}