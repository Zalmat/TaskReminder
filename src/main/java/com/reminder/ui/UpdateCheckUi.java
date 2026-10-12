package com.reminder.ui;

import com.reminder.model.UpdateInfo;
import com.reminder.service.UpdateCheckService;
import com.reminder.ui.util.Dialogs;
import com.reminder.util.VersionInfo;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.awt.Desktop;
import java.net.URI;
import java.util.Optional;
import java.util.prefs.Preferences;

/** UI-обвязка проверки обновлений через {@link UpdateCheckService}. */
public class UpdateCheckUi {

    private final UpdateCheckService service;

    public UpdateCheckUi(UpdateCheckService service) {
        this.service = service;
    }

    /** Проверяет наличие обновлений. При manual=true всегда показывает результат. */
    public void checkForUpdates(boolean manual) {
        service.checkLatest().thenAccept(update ->
                Platform.runLater(() -> handleUpdateResult(update, manual)));
    }

    private void handleUpdateResult(UpdateInfo update, boolean manual) {
        if (update.failed()) {
            if (manual) {
                Dialogs.info("Обновления", "⚠ Не удалось проверить обновления (нет интернета или проблема с GitHub API).");
            }
        } else if (update.newer()) {
            showUpdateDialog(update, manual);
        } else if (manual) {
            Dialogs.info("Обновления", "✅ У вас установлена актуальная версия v" + VersionInfo.load().version());
        }
    }

    private void showUpdateDialog(UpdateInfo update, boolean manual) {
        String latestKey = "v" + update.version();
        Preferences prefs = Preferences.userRoot().node("com/reminder");
        if (!manual && latestKey.equals(prefs.get("lastSeenVersion", ""))) {
            return;
        }
        prefs.put("lastSeenVersion", latestKey);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Доступно обновление");
        alert.setHeaderText("Доступна новая версия " + latestKey);
        String current = "v" + VersionInfo.load().version();
        alert.setContentText("Текущая версия: " + current + "\nНовая версия: " + latestKey);

        ButtonType downloadButton = new ButtonType("Скачать zip", ButtonBar.ButtonData.OK_DONE);
        ButtonType releaseButton = new ButtonType("Открыть релиз");
        ButtonType closeButton = new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(downloadButton, releaseButton, closeButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == downloadButton) {
                String url = !update.downloadUrl().isEmpty() ? update.downloadUrl() : update.releaseUrl();
                openLink(url);
            } else if (result.get() == releaseButton) {
                openLink(update.releaseUrl());
            }
        }
    }

    private void openLink(String url) {
        if (url == null || url.isEmpty()) {
            Dialogs.info("Ошибка", "Ссылка недоступна");
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            Dialogs.info("Ошибка", "Не удалось открыть ссылку: " + e.getMessage());
        }
    }
}