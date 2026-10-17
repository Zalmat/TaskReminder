package com.reminder.ui.controller;

import com.reminder.i18n.I18n;
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
                new ButtonType(I18n.get("button.close"), ButtonBar.ButtonData.CANCEL_CLOSE));

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
        countLabel.setText(I18n.format("holidays.count", holidayItems.size()));
    }

    private void addHoliday() {
        LocalDate date = holidayPicker.getValue();
        if (date == null) {
            alert.accept(I18n.get("common.error"), I18n.get("holidays.selectDate"));
            return;
        }
        if (!workTimeService.addHoliday(date)) {
            alert.accept(I18n.get("common.warning"), I18n.get("holidays.already"));
            return;
        }
        refreshList();
        alert.accept(I18n.get("common.success"), I18n.format("holidays.added", DateUtils.formatDate(date)));
    }

    private void removeHoliday() {
        String selected = holidayList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert.accept(I18n.get("common.warning"), I18n.get("holidays.selectForDelete"));
            return;
        }
        try {
            LocalDate date = DatePickerUtils.converter().fromString(selected);
            if (date == null) {
                alert.accept(I18n.get("common.error"), I18n.get("holidays.deleteError"));
                return;
            }
            workTimeService.removeHoliday(date);
            refreshList();
            alert.accept(I18n.get("common.success"), I18n.format("holidays.deleted", selected));
        } catch (Exception ex) {
            alert.accept(I18n.get("common.error"), I18n.get("holidays.deleteError"));
        }
    }
}