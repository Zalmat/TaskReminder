package com.reminder.services;

import com.reminder.models.Reminder;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReminderService {
    private List<Reminder> reminders = new ArrayList<>();
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;
    private Runnable onReminderTrigger;

    public ReminderService() {
        this.reminders = new ArrayList<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void startMonitoring() {
        if (isRunning) return;
        isRunning = true;

        scheduler.scheduleAtFixedRate(() -> {
            if (!isRunning) return;

            List<Reminder> triggered = new ArrayList<>();
            for (Reminder reminder : reminders) {
                if (reminder.shouldTrigger()) {
                    triggered.add(reminder);
                }
            }

            if (!triggered.isEmpty()) {
                Platform.runLater(() -> {
                    for (Reminder reminder : triggered) {
                        showReminderDialog(reminder);
                    }
                });
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    public void stopMonitoring() {
        isRunning = false;
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

    public void addReminder(Reminder reminder) {
        if (!reminders.contains(reminder)) {
            reminders.add(reminder);
        }
    }

    public void removeReminder(Reminder reminder) {
        reminders.remove(reminder);
    }

    public List<Reminder> getReminders() {
        return new ArrayList<>(reminders);
    }

    public void setOnReminderTrigger(Runnable onReminderTrigger) {
        this.onReminderTrigger = onReminderTrigger;
    }

    private void showReminderDialog(Reminder reminder) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("⏰ Напоминание");
        alert.setHeaderText("Время напоминания!");
        alert.setContentText(reminder.getText());

        ButtonType snoozeButton = new ButtonType("Отложить");
        ButtonType dismissButton = new ButtonType("Закрыть");

        alert.getButtonTypes().setAll(snoozeButton, dismissButton);

        // Создаем выбор времени откладывания
        ChoiceBox<Integer> snoozeMinutes = new ChoiceBox<>();
        snoozeMinutes.getItems().addAll(10, 15, 20, 30, 45, 60);
        snoozeMinutes.setValue(10);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().addAll(
                new Label("Отложить на (минут):"),
                snoozeMinutes
        );

        alert.getDialogPane().setExpandableContent(vbox);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == snoozeButton) {
                int minutes = snoozeMinutes.getValue();
                reminder.setSnoozeUntil(LocalDateTime.now().plusMinutes(minutes));
                if (reminder.isRepeatDaily()) {
                    // Для ежедневных напоминаний
                    reminder.setActive(true);
                }
            } else {
                if (!reminder.isRepeatDaily()) {
                    reminder.setActive(false);
                }
            }
        }

        if (onReminderTrigger != null) {
            onReminderTrigger.run();
        }
    }

    public void shutdown() {
        stopMonitoring();
    }
}