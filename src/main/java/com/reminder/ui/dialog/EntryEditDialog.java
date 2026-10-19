package com.reminder.ui.dialog;

import com.reminder.i18n.I18n;
import com.reminder.ui.theme.ThemeManager;
import com.reminder.ui.util.AppIcons;
import com.reminder.util.TaskValidationUtils;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.List;

/** Диалог редактирования записи дня / задачи недели. */
public final class EntryEditDialog {

    private EntryEditDialog() {
    }

    /** Результат диалога редактирования. */
    public record EntryEditResult(String project, String taskName, String type,
                                  String hours, String comment) {
    }

    /** Открывает диалог. Возвращает результат или null при отмене. */
    public static EntryEditResult show(String title, String header, List<String> types,
                                       String project, String taskName, String type,
                                       String hours, String comment, boolean withHours) {
        Dialog<EntryEditResult> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        ButtonType saveType = new ButtonType(I18n.get("button.save"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField projectField = new TextField(project);
        TextField taskField = new TextField(taskName);
        ComboBox<String> typeField = new ComboBox<>(FXCollections.observableArrayList(types));
        typeField.setEditable(true);
        typeField.setValue(type);
        TextField hoursFieldDialog = new TextField(hours);
        TextArea commentField = new TextArea(comment);
        commentField.setPrefRowCount(3);

        grid.addRow(0, new Label(I18n.get("label.project")), projectField);
        grid.addRow(1, new Label(I18n.get("label.task")), taskField);
        grid.addRow(2, new Label(I18n.get("label.typeWork")), typeField);
        if (withHours) {
            grid.addRow(3, new Label(I18n.get("label.hours")), hoursFieldDialog);
            grid.addRow(4, new Label(I18n.get("label.comment")), commentField);
        } else {
            grid.addRow(3, new Label(I18n.get("label.comment")), commentField);
        }
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton != saveType) return null;
            String p = projectField.getText().trim();
            String t = TaskValidationUtils.getSafeTaskName(taskField.getText());
            String ty = typeField.getValue() != null ? typeField.getValue().trim() : "";
            if (p.isEmpty() || ty.isEmpty()) return null;
            return new EntryEditResult(p, t, ty,
                    withHours ? hoursFieldDialog.getText().trim() : null,
                    commentField.getText().trim());
        });

        ThemeManager.applyTo(dialog.getDialogPane());
        AppIcons.applyTo(dialog);
        return dialog.showAndWait().orElse(null);
    }
}