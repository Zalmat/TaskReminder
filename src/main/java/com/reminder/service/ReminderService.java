package com.reminder.service;

import com.reminder.model.Reminder;
import com.reminder.storage.DataStore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Планировщик напоминаний. Не содержит UI — при срабатывании передаёт напоминание
 * через {@link #setOnReminderFire(Consumer)}. Напоминания сохраняются в JSON/UTF-8.
 */
public class ReminderService {

    private static final String REMINDERS_FILE = "reminders.json";
    private static final long POLL_SECONDS = 30;
    private static final long DEDUP_SECONDS = 120;

    private final DataStore store;
    private final List<Reminder> reminders;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;
    private volatile Consumer<Reminder> onReminderFire;

    public ReminderService() {
        this.store = new DataStore();
        this.reminders = store.loadList(REMINDERS_FILE, Reminder.class);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "reminder-monitor");
            t.setDaemon(true);
            return t;
        });
    }

    public void startMonitoring() {
        if (running) return;
        running = true;
        scheduler.scheduleAtFixedRate(this::poll, 0, POLL_SECONDS, TimeUnit.SECONDS);
    }

    private void poll() {
        if (!running) return;
        List<Reminder> triggered = new ArrayList<>();
        for (Reminder reminder : reminders) {
            if (reminder.shouldTrigger()) {
                triggered.add(reminder);
                // Подавляем повторное срабатывание в том же минутном окне.
                reminder.setSnoozeUntil(LocalDateTime.now().plusSeconds(DEDUP_SECONDS));
            }
        }
        if (triggered.isEmpty()) return;
        javafx.application.Platform.runLater(() -> {
            Consumer<Reminder> handler = onReminderFire;
            if (handler == null) return;
            for (Reminder reminder : triggered) {
                handler.accept(reminder);
            }
        });
    }

    public void setOnReminderFire(Consumer<Reminder> handler) {
        this.onReminderFire = handler;
    }

    public List<Reminder> getReminders() {
        return new ArrayList<>(reminders);
    }

    public void addReminder(Reminder reminder) {
        if (reminder != null && !reminders.contains(reminder)) {
            reminders.add(reminder);
            persist();
        }
    }

    public void updateReminder(Reminder reminder) {
        if (reminders.contains(reminder)) {
            persist();
        }
    }

    public void removeReminder(Reminder reminder) {
        if (reminders.remove(reminder)) {
            persist();
        }
    }

    public void clearSnooze() {
        // Сбрасывает отложенные срабатывания (например, при редактировании времени).
        for (Reminder r : reminders) {
            r.setSnoozeUntil(null);
        }
    }

    private void persist() {
        store.saveList(REMINDERS_FILE, reminders);
    }

    public void stopMonitoring() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void shutdown() {
        stopMonitoring();
    }
}