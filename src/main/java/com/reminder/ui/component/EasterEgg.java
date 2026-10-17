package com.reminder.ui.component;

import com.reminder.i18n.I18n;
import com.reminder.ui.theme.ThemeManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.util.Duration;

/** Пасхалка: серия быстрых кликов по заголовку открывает окно-шутку. */
public class EasterEgg {

    private static final int CLICK_THRESHOLD = 20;

    private int clickCount = 0;
    private Timeline resetTimeline;

    public void onLabelClick() {
        if (resetTimeline == null) {
            resetTimeline = new Timeline(new KeyFrame(Duration.seconds(1.0), event -> clickCount = 0));
            resetTimeline.setCycleCount(1);
        }
        resetTimeline.stop();
        clickCount++;
        resetTimeline.play();
        if (clickCount == CLICK_THRESHOLD) {
            resetTimeline.stop();
            clickCount = 0;
            showJoke();
        }
    }

    private void showJoke() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18n.get("easteregg.title"));
        alert.setHeaderText(I18n.get("easteregg.header"));
        alert.setContentText(I18n.get("easteregg.content"));
        ThemeManager.applyTo(alert.getDialogPane());
        alert.showAndWait();
    }
}