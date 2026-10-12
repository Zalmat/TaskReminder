package com.reminder.ui.controller;

import com.reminder.service.WorkTimeService;
import com.reminder.ui.util.DatePickerUtils;
import com.reminder.util.DateUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiConsumer;

/** Контроллер окна управления праздничными днями. */
public class HolidaysDialogController {

    @FXML private DialogPane dialogPane;
    @FXML private Label countLabel;
    @FXML private ListView<String> holidayList;
    @FXML private DatePicker holidayPicker;
    @FXML private Button addHolidayButton;
    @FXML private Button deleteHolidayButton;

    private WorkTimeService workTimeService;
    private BiConsumer<String, String> alert;
    private final ObservableList<String> holidayItems = FXCollections.observableArrayList();

    public void init(WorkTimeService workTimeService, BiConsumer<String, String> alert) {
        this.workTimeService = workTimeService;
        this.alert = alert;

        DatePickerUtils.applyTo(holidayPicker);
        holidayPicker.setValue(LocalDate.now());

        holidayList.setItems(holidayItems);
        addHolidayButton.setOnAction(e -> addHoliday());
        deleteHolidayButton.setOnAction(e -> removeHoliday());

        dialogPane.getButtonTypes().setAll(
                new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE));

        refreshList();
    }

    private void refreshList() {
        List<LocalDate> holidays = workTimeService.getHolidays().stream()
                .sorted()
                .toList();
        holidayItems.setAll(holidays.stream().map(DateUtils::formatDate).toList());
        updateCount();
    }

    private void updateCount() {
        countLabel.setText("Праздничные дни (всего: " + holidayItems.size() + "):");
    }

    private void addHoliday() {
        LocalDate date = holidayPicker.getValue();
        if (date == null) {
            alert.accept("Ошибка", "⚠️ Пожалуйста, выберите дату");
            return;
        }
        if (!workTimeService.addHoliday(date)) {
            alert.accept("Внимание", "⚠️ Этот день уже добавлен как праздничный");
            return;
        }
        refreshList();
        alert.accept("Успех", "✅ Праздничный день добавлен: " + DateUtils.formatDate(date));
    }

    private void removeHoliday() {
        String selected = holidayList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert.accept("Внимание", "⚠️ Пожалуйста, выберите день для удаления");
            return;
        }
        try {
            LocalDate date = DatePickerUtils.converter().fromString(selected);
            if (date == null) {
                alert.accept("Ошибка", "⚠️ Не удалось удалить выбранный день");
                return;
            }
            workTimeService.removeHoliday(date);
            refreshList();
            alert.accept("Успех", "✅ Праздничный день удален: " + selected);
        } catch (Exception ex) {
            alert.accept("Ошибка", "⚠️ Не удалось удалить выбранный день");
        }
    }
}