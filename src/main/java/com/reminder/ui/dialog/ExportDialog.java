package com.reminder.ui.dialog;

import com.reminder.i18n.I18n;
import com.reminder.service.ExportService;
import com.reminder.service.TaskService;
import com.reminder.ui.controller.ExportDialogController;
import javafx.stage.Window;

import java.time.LocalDate;
import java.util.function.BiConsumer;

/** Показ окна экспорта. */
public final class ExportDialog {

    private ExportDialog() {
    }

    public static void show(TaskService taskService, ExportService exportService, LocalDate date,
                            boolean weekMode, Window owner, BiConsumer<String, String> alert) {
        DialogSupport.show("/fxml/export-dialog.fxml", I18n.get("dialog.export.title"),
                ExportDialogController.class,
                controller -> controller.init(taskService, exportService, date, weekMode, owner, alert),
                null, "dialog.export.error");
    }
}
