package com.reminder.ui.component;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Часы в статус-баре: обновляют переданный Label раз в секунду. */
public final class ClockUi {

    private ClockUi() {
    }

    public static void start(Label clockLabel) {
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                clockLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }
}