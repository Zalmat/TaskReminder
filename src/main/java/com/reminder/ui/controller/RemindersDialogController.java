package com.reminder.ui.controller;

import com.reminder.i18n.I18n;
import com.reminder.model.Reminder;
import com.reminder.service.ReminderService;
import com.reminder.ui.util.AppIcons;
import com.reminder.ui.util.Dialogs;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.time.LocalTime;

/** Контроллер окна управления напоминаниями. */
public class RemindersDialogController {

    @FXML private DialogPane dialogPane;
    @FXML private TableView<Reminder> remindersTable;
    @FXML private TableColumn<Reminder, String> timeColumn;
    @FXML private TableColumn<Reminder, String> textColumn;
    @FXML private TableColumn<Reminder, Boolean> activeColumn;
    @FXML private TableColumn<Reminder, Boolean> repeatColumn;
    @FXML private Spinner<Integer> hourSpinner;
    @FXML private Spinner<Integer> minuteSpinner;
    @FXML private TextArea textArea;
    @FXML private CheckBox repeatCheckBox;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button toggleButton;

    private ReminderService reminderService;
    private final ObservableList<Reminder> reminders = FXCollections.observableArrayList();

    public void init(ReminderService reminderService) {
        this.reminderService = reminderService;

        hourSpinner.setValueFactory(new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, LocalTime.now().getHour()));
        minuteSpinner.setValueFactory(new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, LocalTime.now().getMinute()));

        timeColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getFormattedTime()));
        textColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getText()));
        activeColumn.setCellValueFactory(cd -> new ReadOnlyBooleanWrapper(cd.getValue().isActive()));
        repeatColumn.setCellValueFactory(cd -> new ReadOnlyBooleanWrapper(cd.getValue().isRepeatDaily()));

        remindersTable.setItems(reminders);

        addButton.setOnAction(e -> addReminder());
        editButton.setOnAction(e -> editReminder());
        deleteButton.setOnAction(e -> deleteReminder());
        toggleButton.setOnAction(e -> toggleActive());

        dialogPane.getButtonTypes().setAll(
                new ButtonType(I18n.get("button.close"), ButtonBar.ButtonData.CANCEL_CLOSE));

        refreshList();
    }

    private void refreshList() {
        reminders.setAll(reminderService.getReminders());
    }

    private void addReminder() {
        LocalTime time = LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue());
        String text = textArea.getText();
        if (text == null || text.trim().isEmpty()) {
            Dialogs.error(I18n.get("common.error"), I18n.get("reminders.enterText"));
            return;
        }
        Reminder reminder = new Reminder(time, text.trim(), repeatCheckBox.isSelected());
        reminderService.addReminder(reminder);
        refreshList();
        textArea.clear();
        repeatCheckBox.setSelected(false);
        Dialogs.info(I18n.get("common.success"), I18n.get("reminders.added"));
    }

    private void editReminder() {
        Reminder selected = remindersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Dialogs.warn(I18n.get("common.warning"), I18n.get("reminders.selectEdit"));
            return;
        }

        Dialog<Reminder> dialog = new Dialog<>();
        dialog.setTitle(I18n.get("reminders.editTitle"));
        dialog.setHeaderText(I18n.get("reminders.editHeader"));
        AppIcons.applyTo(dialog);

        ButtonType saveType = new ButtonType(I18n.get("button.save"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        Spinner<Integer> hour = new Spinner<>(0, 23, selected.getTime().getHour());
        hour.setEditable(true);
        Spinner<Integer> minute = new Spinner<>(0, 59, selected.getTime().getMinute());
        minute.setEditable(true);
        HBox timeBox = new HBox(5, hour, new Label(":"), minute);

        TextArea text = new TextArea(selected.getText());
        text.setPrefRowCount(3);
        CheckBox repeat = new CheckBox(I18n.get("reminders.repeat"));
        repeat.setSelected(selected.isRepeatDaily());
        CheckBox active = new CheckBox(I18n.get("reminders.active"));
        active.setSelected(selected.isActive());

        grid.addRow(0, new Label(I18n.get("reminders.label.time")), timeBox);
        grid.addRow(1, new Label(I18n.get("reminders.label.text")), text);
        grid.addRow(2, new Label(""), repeat);
        grid.addRow(3, new Label(""), active);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton != saveType) return null;
            String txt = text.getText();
            if (txt == null || txt.trim().isEmpty()) {
                Dialogs.error(I18n.get("common.error"), I18n.get("reminders.enterText"));
                return null;
            }
            selected.setTime(LocalTime.of(hour.getValue(), minute.getValue()));
            selected.setText(txt.trim());
            selected.setRepeatDaily(repeat.isSelected());
            selected.setActive(active.isSelected());
            return selected;
        });

        dialog.showAndWait().ifPresent(r -> {
            reminderService.updateReminder(r);
            refreshList();
            Dialogs.info(I18n.get("common.success"), I18n.get("reminders.updated"));
        });
    }

    private void deleteReminder() {
        Reminder selected = remindersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Dialogs.warn(I18n.get("common.warning"), I18n.get("reminders.selectDelete"));
            return;
        }
        if (Dialogs.confirm(I18n.get("confirm.delete.title"), I18n.get("reminders.deleteConfirm"),
                I18n.format("reminders.deleteContent",
                        selected.getFormattedTime(), selected.getText()))) {
            reminderService.removeReminder(selected);
            refreshList();
        }
    }

    private void toggleActive() {
        Reminder selected = remindersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Dialogs.warn(I18n.get("common.warning"), I18n.get("reminders.selectToggle"));
            return;
        }
        selected.setActive(!selected.isActive());
        reminderService.updateReminder(selected);
        remindersTable.refresh();
        refreshList();
    }
}