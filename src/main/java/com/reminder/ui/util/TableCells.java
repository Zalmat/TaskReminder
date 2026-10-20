package com.reminder.ui.util;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;

/** Утилиты для ячеек таблиц. */
public final class TableCells {

    private static final double HORIZONTAL_INSETS = 12;
    private static final double TOOLTIP_MAX_WIDTH = 480;

    private TableCells() {
    }

    /** Ячейка с визуальным переносом текста и подсказкой с полным значением. */
    public static <S> TableCell<S, String> wrapping() {
        return new TableCell<>() {
            private final Label label = new Label();
            private final Tooltip tooltip = Tooltips.create("");

            {
                label.setWrapText(true);
                label.setMinHeight(Region.USE_PREF_SIZE);
                label.setPadding(Insets.EMPTY);
                label.maxWidthProperty().bind(Bindings.max(0, widthProperty().subtract(HORIZONTAL_INSETS)));
                label.textFillProperty().bind(textFillProperty());
                label.fontProperty().bind(fontProperty());

                tooltip.setWrapText(true);
                tooltip.setMaxWidth(TOOLTIP_MAX_WIDTH);
                setText(null);
                setGraphic(label);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    label.setText(null);
                    setGraphic(null);
                    setTooltip(null);
                } else {
                    label.setText(item);
                    setGraphic(label);
                    tooltip.setText(item);
                    setTooltip(tooltip);
                }
            }
        };
    }
}
