package com.reminder.ui.util;

import com.reminder.i18n.I18n;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

/** Единый локализованный placeholder для пустых таблиц. */
public final class TablePlaceholders {

    private TablePlaceholders() {
    }

    public static void apply(TableView<?> table) {
        table.setPlaceholder(new Label(I18n.get("table.empty")));
    }
}
