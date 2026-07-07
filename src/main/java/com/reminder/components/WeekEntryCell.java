package com.reminder.components;

import com.reminder.models.WeekEntry;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import java.time.LocalDate;

public class WeekEntryCell extends TableCell<WeekEntry, Integer> {
    private TextField textField;
    private LocalDate currentDate;

    public WeekEntryCell(LocalDate currentDate) {
        this.currentDate = currentDate;
    }

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
        textField.setOnKeyPressed(t -> {
            if (t.getCode() == KeyCode.ENTER) {
                commitEdit(parseValue(textField.getText()));
            } else if (t.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
            }
        });
        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                commitEdit(parseValue(textField.getText()));
            }
        });
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                textField.setText(oldVal);
            }
        });
    }

    private Integer parseValue(String value) {
        try {
            if (value == null || value.trim().isEmpty()) return 0;
            int val = Integer.parseInt(value.trim());
            if (val < 0) return 0;
            if (val > 24) return 24;
            return val;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}