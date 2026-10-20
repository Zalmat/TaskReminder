package com.reminder.ui.util;

import javafx.geometry.Insets;
import javafx.scene.control.ListCell;

/** Ячейки выпадающих списков: длинный текст переносится визуально, без изменения значения. */
public final class ComboCells {

    private static final double MAX_WIDTH = 480;

    private ComboCells() {
    }

    public static <T> ListCell<T> wrapping() {
        return new ListCell<>() {
            @Override
            protected double computePrefWidth(double height) {
                return Math.min(MAX_WIDTH, super.computePrefWidth(height));
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setWrapText(false);
                    return;
                }
                setText(String.valueOf(item));
                setWrapText(true);
                setMaxWidth(MAX_WIDTH);
                setPadding(new Insets(4, 6, 4, 6));
            }
        };
    }
}
