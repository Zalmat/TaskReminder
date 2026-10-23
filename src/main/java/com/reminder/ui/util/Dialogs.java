package com.reminder.ui.util;

import com.reminder.i18n.I18n;
import com.reminder.ui.theme.ThemeManager;
import com.reminder.ui.util.AppIcons;
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
        ThemeManager.applyTo(alert.getDialogPane());
        AppIcons.applyTo(alert);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    /** Подтверждение с кнопками «Да»/«Нет». Возвращает true при выборе «Да». */
    public static boolean yesNo(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        ButtonType yesButton = new ButtonType(I18n.get("dialog.yes"), ButtonBar.ButtonData.OK_DONE);
        ButtonType noButton = new ButtonType(I18n.get("dialog.no"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(yesButton, noButton);
        ThemeManager.applyTo(alert.getDialogPane());
        AppIcons.applyTo(alert);
        return alert.showAndWait().orElse(noButton) == yesButton;
    }

    /** Подтверждение выхода из недели с несохранённым черновиком. */
    public static boolean confirmLeaveDraft(String title, String header, String content,
                                            String stayLabel, String leaveLabel) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        ButtonType stayButton = new ButtonType(stayLabel, ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType leaveButton = new ButtonType(leaveLabel, ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(stayButton, leaveButton);
        ThemeManager.applyTo(alert.getDialogPane());
        AppIcons.applyTo(alert);
        return alert.showAndWait().orElse(stayButton) == leaveButton;
    }

    /**
     * Маршрутизирует по заголовку: «Ошибка»→error, «Внимание»→warn, иначе→info.
     * Сравнение идёт с локализованными заголовками, поэтому не зависит от языка.
     */
    public static void accept(String title, String content) {
        if (title != null && title.equals(I18n.get("common.error"))) {
            error(title, content);
        } else if (title != null && title.equals(I18n.get("common.warning"))) {
            warn(title, content);
        } else {
            info(title, content);
        }
    }

    /** Собирает непустой текст, описывающий ошибку (message + цепочка причин). */
    public static String reason(Throwable t) {
        if (t == null) {
            return I18n.get("error.unknown");
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
        ThemeManager.applyTo(alert.getDialogPane());
        AppIcons.applyTo(alert);
        alert.showAndWait();
    }
}
