package com.reminder.ui.view;

import com.reminder.i18n.I18n;
import com.reminder.model.Reminder;
import com.reminder.service.ReminderService;
import com.reminder.ui.theme.ThemeManager;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/** UI напоминаний: дефолтное напоминание, всплывающий диалог и звук. */
public class ReminderUi {

    private final ReminderService reminderService;

    public ReminderUi(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    /** Инициализирует дефолтное напоминание и подписку на срабатывание. */
    public void setup() {
        if (reminderService.getReminders().isEmpty()) {
            Reminder reminder = new Reminder(LocalTime.of(12, 0), I18n.get("reminder.default"));
            reminder.setRepeatDaily(true);
            reminderService.addReminder(reminder);
        }
        reminderService.setOnReminderFire(this::showDialog);
    }

    private void showDialog(Reminder reminder) {
        playSound();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18n.get("reminder.popup.title"));
        alert.setHeaderText(I18n.get("reminder.popup.header"));
        alert.setContentText(reminder.getText());

        ButtonType snoozeButton = new ButtonType(I18n.get("reminder.snooze"));
        ButtonType dismissButton = new ButtonType(I18n.get("button.close"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(snoozeButton, dismissButton);

        Spinner<Integer> snoozeMinutes = new Spinner<>(10, 120, 10, 5);
        VBox vbox = new VBox(10, new Label(I18n.get("reminder.snoozeMinutes")), snoozeMinutes);
        vbox.setPadding(new Insets(10));
        alert.getDialogPane().setExpandableContent(vbox);
        ThemeManager.applyTo(alert.getDialogPane());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == snoozeButton) {
                reminder.setSnoozeUntil(LocalDateTime.now().plusMinutes(snoozeMinutes.getValue()));
                reminder.setActive(true);
                reminderService.updateReminder(reminder);
            } else if (!reminder.isRepeatDaily()) {
                reminder.setActive(false);
                reminderService.updateReminder(reminder);
            }
        }
    }

    private void playSound() {
        try {
            var resource = getClass().getResource("/sounds/reminder.wav");
            if (resource == null) {
                System.err.println("Cannot play reminder sound: resource not found");
                return;
            }
            new AudioClip(resource.toExternalForm()).play();
        } catch (Exception e) {
            System.err.println("Cannot play reminder sound: " + e.getMessage());
        }
    }
}