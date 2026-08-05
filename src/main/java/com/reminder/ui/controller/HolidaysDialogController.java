package com.reminder.ui.controller;

import com.reminder.service.WorkTimeService;
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
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public void init(WorkTimeService workTimeService, BiConsumer<String, String> alert) {
        this.workTimeService = workTimeService;
        this.alert = alert;

        holidayPicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? date.format(DATE_FORMATTER) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.isEmpty()) return null;
                try {
                    return LocalDate.parse(string, DATE_FORMATTER);
                } catch (Exception ex) {
                    return null;
                }
            }
        });
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
        holidayItems.setAll(holidays.stream().map(d -> d.format(DATE_FORMATTER)).toList());
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
        alert.accept("Успех", "✅ Праздничный день добавлен: " + date.format(DATE_FORMATTER));
    }

    private void removeHoliday() {
        String selected = holidayList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert.accept("Внимание", "⚠️ Пожалуйста, выберите день для удаления");
            return;
        }
        try {
            LocalDate date = LocalDate.parse(selected, DATE_FORMATTER);
            workTimeService.removeHoliday(date);
            refreshList();
            alert.accept("Успех", "✅ Праздничный день удален: " + selected);
        } catch (Exception ex) {
            alert.accept("Ошибка", "⚠️ Не удалось удалить выбранный день");
        }
    }
}