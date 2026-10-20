package com.reminder.ui;

import com.reminder.catalog.importing.ImportOutcome;
import com.reminder.i18n.I18n;
import com.reminder.ui.util.Tooltips;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;

/** Уведомления об обновлении каталога: индикатор в окне и сообщение в трее. */
public class ImportNotifier {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy.MM.dd - HH:mm");

    private final Label bell;
    private final BiConsumer<String, String> tray;

    public ImportNotifier(Label bell, BiConsumer<String, String> tray) {
        this.bell = bell;
        this.tray = tray;
        if (bell != null) {
            bell.setOnMouseClicked(event -> hideBell());
        }
    }

    /** Показывает уведомление (только когда реально добавлены новые имена). */
    public void notifyImported(LocalDateTime time, ImportOutcome outcome, boolean trayEnabled) {
        if (outcome == null || !outcome.hasNew()) {
            return;
        }
        String text = I18n.format("tasks.import.notify.text", time.format(TIME),
                outcome.added().projects(), outcome.added().taskNames(), outcome.added().types());
        if (bell != null) {
            bell.setText("🔔");
            bell.setTooltip(Tooltips.create(text));
            bell.setVisible(true);
            bell.setManaged(true);
            shakeBell();
        }
        if (trayEnabled && tray != null) {
            tray.accept(I18n.get("app.tray.caption"), text);
        }
    }

    private void shakeBell() {
        if (bell == null) {
            return;
        }
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(bell.translateXProperty(), 0)),
                new KeyFrame(Duration.millis(60), new KeyValue(bell.translateXProperty(), -3, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(120), new KeyValue(bell.translateXProperty(), 3, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(180), new KeyValue(bell.translateXProperty(), -2, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(240), new KeyValue(bell.translateXProperty(), 2, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(300), new KeyValue(bell.translateXProperty(), 0, Interpolator.EASE_BOTH))
        );
        timeline.play();
    }

    private void hideBell() {
        if (bell != null) {
            bell.setVisible(false);
            bell.setManaged(false);
        }
    }
}
