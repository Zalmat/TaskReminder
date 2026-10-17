package com.reminder.ui.controller;

import com.reminder.i18n.I18n;
import com.reminder.model.WorkEntry;
import com.reminder.service.ExportFormat;
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
import javafx.util.StringConverter;

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
    @FXML private ComboBox<ExportFormat> formatChoice;

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

        formatChoice.getItems().setAll(ExportFormat.values());
        formatChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(ExportFormat format) {
                return format == null ? "" : format.label();
            }

            @Override
            public ExportFormat fromString(String string) {
                return null;
            }
        });
        formatChoice.setValue(ExportFormat.EXCEL);

        ButtonType exportType = new ButtonType(I18n.get("export.button"), ButtonBar.ButtonData.OK_DONE);
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
            alert.accept(I18n.get("common.error"), I18n.get("export.invalidRange"));
            return;
        }

        List<WorkEntry> entries = taskService.getEntriesForDateRange(start, end);
        if (entries.isEmpty()) {
            alert.accept(I18n.get("common.warning"), I18n.get("export.noData"));
            return;
        }

        ExportFormat format = formatChoice.getValue() != null ? formatChoice.getValue() : ExportFormat.EXCEL;
        String dateRange = String.format("%s_%s",
                start.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                end.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.get("export.saveTitle"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get(format.filterNameKey()), "*." + format.extension()));
        fileChooser.setInitialFileName(
                String.format("work_report_%s_%s.%s", timestamp, dateRange, format.extension()));

        try {
            File file = fileChooser.showSaveDialog(owner);
            if (file == null) return;

            String filePath = file.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith("." + format.extension())) {
                filePath += "." + format.extension();
            }
            exportService.export(entries, format, filePath);
            closeDialog();
            alert.accept(I18n.get("common.success"), I18n.format("export.success", entries.size()));
        } catch (Exception ex) {
            System.err.println("Export failed: " + ex.getMessage());
            alert.accept(I18n.get("common.error"), I18n.format("export.error", Dialogs.reason(ex)));
        }
    }

    private void closeDialog() {
        javafx.stage.Window window = dialogPane.getScene().getWindow();
        if (window instanceof javafx.stage.Stage stage) {
            stage.close();
        }
    }
}
