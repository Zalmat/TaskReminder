package com.reminder.ui.component;

import com.reminder.model.WeekEntry;
import com.reminder.ui.util.NumericFields;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

/** Редактируемая ячейка с количеством часов для недельного режима. */
public class WeekEntryCell extends TableCell<WeekEntry, Integer> {
    private TextField textField;

    @Override
    public void startEdit() {
        super.startEdit();
        if (textField == null) {
            createTextField();
        }
        setText(null);
        setGraphic(textField);
        textField.selectAll();
        textField.requestFocus();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItem() != null ? getItem().toString() : "0");
        setGraphic(null);
    }

    @Override
    public void updateItem(Integer item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(getItem() != null ? getItem().toString() : "0");
                }
                setText(null);
                setGraphic(textField);
            } else {
                setText(getItem() != null ? getItem().toString() : "0");
                setGraphic(null);
            }
        }
    }

    private void createTextField() {
        textField = new TextField(getItem() != null ? getItem().toString() : "0");
        textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
        NumericFields.digitsOnly(textField, 2);
        textField.setOnKeyPressed(t -> {
            if (t.getCode() == KeyCode.ENTER) {
                commitEdit(parseValue(textField.getText()));
            } else if (t.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
            }
        });
        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && isEditing()) {
                commitEdit(parseValue(textField.getText()));
            }
        });
    }

    private Integer parseValue(String value) {
        try {
            if (value == null || value.trim().isEmpty()) return 0;
            return Math.max(0, Math.min(24, Integer.parseInt(value.trim())));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}