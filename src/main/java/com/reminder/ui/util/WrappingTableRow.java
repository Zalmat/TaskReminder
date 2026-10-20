package com.reminder.ui.util;

import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;

/** Строка таблицы, высота которой учитывает перенесённый текст в ячейках. */
public class WrappingTableRow<S> extends TableRow<S> {

    @Override
    protected double computePrefHeight(double width) {
        double height = super.computePrefHeight(width);
        int cells = Math.max(1, getChildren().size());
        double fallbackWidth = width / cells;
        for (Node node : getChildren()) {
            if (node instanceof TableCell<?, ?> cell && !cell.isEmpty()) {
                double cellWidth = cell.getWidth() > 0 ? cell.getWidth() : fallbackWidth;
                height = Math.max(height, cell.prefHeight(cellWidth));
            }
        }
        return height;
    }
}
