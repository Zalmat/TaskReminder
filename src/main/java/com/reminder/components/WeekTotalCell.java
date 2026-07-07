package com.reminder.components;

import com.reminder.models.WeekEntry;
import javafx.scene.control.TableCell;

public class WeekTotalCell extends TableCell<WeekEntry, Integer> {

    @Override
    public void updateItem(Integer item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setStyle("");
        } else {
            setText(item != null ? item.toString() : "0");
            if (item != null && item > 40) {
                setStyle("-fx-font-weight: bold; -fx-text-fill: red;");
            } else {
                setStyle("-fx-font-weight: bold;");
            }
        }
    }
}