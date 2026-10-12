package com.reminder.ui.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/** Единое место для диалогов приложения. */
public final class Dialogs {

    private Dialogs() {
    }

    public static void info(String title, String content) {
        show(Alert.AlertType.INFORMATION, title, content);
    }

    public static void warn(String title, String content) {
        show(Alert.AlertType.WARNING, title, content);
    }

    public static void error(String title, String content) {
        show(Alert.AlertType.ERROR, title, content);
    }

    /** Подтверждение. Возвращает true, если пользователь нажал OK. */
    public static boolean confirm(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    /** Подтверждение с кнопками «Да»/«Нет». Возвращает true при выборе «Да». */
    public static boolean yesNo(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        ButtonType yesButton = new ButtonType("Да", ButtonBar.ButtonData.OK_DONE);
        ButtonType noButton = new ButtonType("Нет", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(yesButton, noButton);
        return alert.showAndWait().orElse(noButton) == yesButton;
    }

    /** Собирает непустой текст, описывающий ошибку (message + цепочка причин). */
    public static String reason(Throwable t) {
        if (t == null) {
            return "Неизвестная ошибка";
        }
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause()) {
            String msg = c.getMessage();
            if (msg != null && !msg.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(" → ");
                }
                sb.append(msg);
            }
        }
        if (sb.isEmpty()) {
            return t.getClass().getSimpleName() + " (" + t + ")";
        }
        return sb.toString();
    }

    private static void show(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}