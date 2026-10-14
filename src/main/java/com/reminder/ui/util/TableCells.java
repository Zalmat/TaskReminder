package com.reminder.ui.util;

import javafx.scene.control.TableCell;

/** Утилиты для ячеек таблиц. */
public final class TableCells {

    private TableCells() {
    }

    /** Ячейка с переносом текста. */
    public static <S> TableCell<S, String> wrapping() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setWrapText(false);
                } else {
                    setText(item);
                    setWrapText(true);
                }
            }
        };
    }
}