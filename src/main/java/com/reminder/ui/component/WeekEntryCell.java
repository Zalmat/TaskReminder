package com.reminder.ui.component;

import com.reminder.i18n.I18n;
import com.reminder.model.WeekEntry;
import com.reminder.ui.util.NumericFields;
import com.reminder.util.Hours;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import java.util.OptionalInt;
import java.util.function.Consumer;

/** Редактируемая ячейка с количеством часов для недельного режима. */
public class WeekEntryCell extends TableCell<WeekEntry, Integer> {
    private TextField textField;
    private final Consumer<String> onInvalid;

    public WeekEntryCell() {
        this(message -> { });
    }

    public WeekEntryCell(Consumer<String> onInvalid) {
        this.onInvalid = onInvalid == null ? message -> { } : onInvalid;
        setAlignment(Pos.CENTER);
    }

    @Override
    public void startEdit() {
        super.startEdit();
        if (textField == null) {
            createTextField();
        }
        fitEditorToCell();
        setText(null);
        setGraphic(textField);
        textField.selectAll();
        textField.requestFocus();
    }

    private void fitEditorToCell() {
        double verticalPadding = getPadding() == null
                ? 0 : getPadding().getTop() + getPadding().getBottom();
        double available = getHeight() - verticalPadding;
        if (available <= 0) {
            available = 24;
        }
        double height = Math.max(18, available);
        textField.setMinHeight(height);
        textField.setPrefHeight(height);
        textField.setMaxHeight(height);
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
                commitOrWarn(textField.getText());
            } else if (t.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
            }
        });
        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && isEditing()) {
                commitOrWarn(textField.getText());
            }
        });
    }

    private void commitOrWarn(String text) {
        OptionalInt value = Hours.parseWeekCell(text);
        if (value.isPresent()) {
            commitEdit(value.getAsInt());
        } else {
            onInvalid.accept(I18n.get("week.hoursInvalid"));
            cancelEdit();
        }
    }
}