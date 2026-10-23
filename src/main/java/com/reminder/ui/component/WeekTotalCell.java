package com.reminder.ui.component;

import com.reminder.model.WeekEntry;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;

/** Ячейка итоговой суммы по задаче в недельной таблице. */
public class WeekTotalCell extends TableCell<WeekEntry, Integer> {

    public WeekTotalCell() {
        setAlignment(Pos.CENTER);
    }

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