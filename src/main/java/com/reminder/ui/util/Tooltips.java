package com.reminder.ui.util;

import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/** Единые настройки всплывающих подсказок: тайминги появления/скрытия задаются в одном месте. */
public final class Tooltips {

    private static final Duration SHOW_DELAY = Duration.millis(250);
    private static final Duration SHOW_DURATION = Duration.seconds(5);
    private static final Duration HIDE_DELAY = Duration.millis(100);

    private Tooltips() {
    }

    /** Создаёт подсказку с едиными таймингами. */
    public static Tooltip create(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setShowDelay(SHOW_DELAY);
        tooltip.setShowDuration(SHOW_DURATION);
        tooltip.setHideDelay(HIDE_DELAY);
        return tooltip;
    }

    /** Устанавливает подсказку на контрол с едиными таймингами; {@code text == null} убирает подсказку. */
    public static void apply(Control control, String text) {
        control.setTooltip(text == null ? null : create(text));
    }
}
