package com.reminder.ui.controller;

import com.reminder.model.WorkEntry;
import com.reminder.service.ExportService;
import com.reminder.service.TaskService;
import com.reminder.util.DateUtils;
import com.reminder.ui.util.DatePickerUtils;
import com.reminder.ui.util.Dialogs;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;

/** Контроллер окна экспорта данных. */
public class ExportDialogController {

    @FXML private DialogPane dialogPane;
    @FXML private DatePicker startDate;
    @FXML private DatePicker endDate;
    @FXML private ComboBox<String> formatChoice;

    private TaskService taskService;
    private ExportService exportService;
    private Window owner;
    private BiConsumer<String, String> alert;

    public void init(TaskService taskService, ExportService exportService,
                     LocalDate currentDate, boolean weekView, Window owner, BiConsumer<String, String> alert) {
        this.taskService = taskService;
        this.exportService = exportService;
        this.owner = owner;
        this.alert = alert;

        DatePickerUtils.applyTo(startDate);
        DatePickerUtils.applyTo(endDate);

        if (weekView && currentDate != null) {
            LocalDate start = DateUtils.startOfWeek(currentDate);
            startDate.setValue(start);
            endDate.setValue(start.plusDays(6));
        } else {
            startDate.setValue(currentDate);
            endDate.setValue(currentDate);
        }

        formatChoice.getItems().addAll(
                "Excel (.xlsx)", "CSV (.csv)", "JSON (.json)", "YAML (.yaml)", "XML (.xml)");
        formatChoice.setValue("Excel (.xlsx)");

        ButtonType exportType = new ButtonType("Экспорт", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().setAll(exportType, ButtonType.CANCEL);
        Button exportButton = (Button) dialogPane.lookupButton(exportType);
        exportButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            doExport();
        });
    }

    private void doExport() {
        LocalDate start = startDate.getValue();
        LocalDate end = endDate.getValue();
        if (start == null || end == null || start.isAfter(end)) {
            alert.accept("Ошибка", "⚠️ Некорректный диапазон дат");
            return;
        }

        List<WorkEntry> entries = taskService.getEntriesForDateRange(start, end);
        if (entries.isEmpty()) {
            alert.accept("Внимание", "⚠️ Нет данных за выбранный период");
            return;
        }

        String format = formatChoice.getValue();
        String dateRange = String.format("%s_%s",
                start.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                end.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить файл");

        if (format.startsWith("Excel")) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel файлы", "*.xlsx"));
            fileChooser.setInitialFileName(String.format("work_report_%s_%s.xlsx", timestamp, dateRange));
        } else if (format.startsWith("CSV")) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV файлы", "*.csv"));
            fileChooser.setInitialFileName(String.format("work_report_%s_%s.csv", timestamp, dateRange));
        } else if (format.startsWith("JSON")) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON файлы", "*.json"));
            fileChooser.setInitialFileName(String.format("work_report_%s_%s.json", timestamp, dateRange));
        } else if (format.startsWith("YAML")) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("YAML файлы", "*.yaml"));
            fileChooser.setInitialFileName(String.format("work_report_%s_%s.yaml", timestamp, dateRange));
        } else {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML файлы", "*.xml"));
            fileChooser.setInitialFileName(String.format("work_report_%s_%s.xml", timestamp, dateRange));
        }

        try {
            File file = fileChooser.showSaveDialog(owner);
            if (file == null) return;

            String filePath = file.getAbsolutePath();
            if (format.startsWith("Excel")) {
                if (!filePath.toLowerCase().endsWith(".xlsx")) filePath += ".xlsx";
                exportService.exportToExcel(entries, filePath);
            } else if (format.startsWith("CSV")) {
                if (!filePath.toLowerCase().endsWith(".csv")) filePath += ".csv";
                exportService.exportToCsv(entries, filePath);
            } else if (format.startsWith("JSON")) {
                if (!filePath.toLowerCase().endsWith(".json")) filePath += ".json";
                exportService.exportToJson(entries, filePath);
            } else if (format.startsWith("YAML")) {
                if (!filePath.toLowerCase().endsWith(".yaml")) filePath += ".yaml";
                exportService.exportToYaml(entries, filePath);
            } else {
                if (!filePath.toLowerCase().endsWith(".xml")) filePath += ".xml";
                exportService.exportToXml(entries, filePath);
            }
            closeDialog();
            alert.accept("Успех", String.format("✅ Данные экспортированы!\n%d записей", entries.size()));
        } catch (Exception ex) {
            ex.printStackTrace();
            alert.accept("Ошибка", "❌ Ошибка экспорта: " + Dialogs.reason(ex));
        }
    }

    private void closeDialog() {
        javafx.stage.Window window = dialogPane.getScene().getWindow();
        if (window instanceof javafx.stage.Stage stage) {
            stage.close();
        }
    }
}