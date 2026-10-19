package com.reminder.ui.util;

import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.List;

/** Иконка приложения для окна (заголовок/панель задач/переключатель окон). */
public final class AppIcons {

    private static final List<String> ICON_RESOURCES = List.of(
            "/icons/app-16.png", "/icons/app-24.png", "/icons/app-32.png",
            "/icons/app-48.png", "/icons/app-64.png", "/icons/app-128.png", "/icons/app-256.png");

    private AppIcons() {
    }

    /** Устанавливает набор иконок для окна; отсутствие ресурсов не критично. */
    public static void apply(Stage stage) {
        if (stage == null) {
            return;
        }
        for (String resource : ICON_RESOURCES) {
            try (InputStream in = AppIcons.class.getResourceAsStream(resource)) {
                if (in != null) {
                    stage.getIcons().add(new Image(in));
                }
            } catch (Exception ignored) {
                // иконка не критична для работы приложения
            }
        }
    }

    /** Устанавливает иконку окна диалога (в заголовке) при его показе. */
    public static void applyTo(Dialog<?> dialog) {
        if (dialog == null) {
            return;
        }
        dialog.setOnShown(event -> {
            if (dialog.getDialogPane() != null
                    && dialog.getDialogPane().getScene() != null
                    && dialog.getDialogPane().getScene().getWindow() instanceof Stage stage) {
                apply(stage);
            }
        });
    }
}
