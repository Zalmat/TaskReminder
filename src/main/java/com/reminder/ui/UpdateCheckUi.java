package com.reminder.ui;

import com.reminder.app.DevMode;
import com.reminder.i18n.I18n;
import com.reminder.model.UpdateInfo;
import com.reminder.service.UpdateCheckService;
import com.reminder.ui.theme.ThemeManager;
import com.reminder.ui.util.Dialogs;
import com.reminder.ui.util.ProgramFolder;
import com.reminder.ui.util.Tooltips;
import com.reminder.util.VersionInfo;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.util.Optional;
import java.util.prefs.Preferences;

/** UI-обвязка проверки обновлений через {@link UpdateCheckService}. */
public class UpdateCheckUi {

    private static final int MAX_NOTES_LENGTH = 4000;

    private final UpdateCheckService service;

    public UpdateCheckUi(UpdateCheckService service) {
        this.service = service;
    }

    /** Проверяет наличие обновлений. При manual=true всегда показывает результат. */
    public void checkForUpdates(boolean manual) {
        // Режим разработки: показываем диалог с демо-описанием без обращения к сети.
        if (DevMode.isEnabled()) {
            Platform.runLater(() -> showUpdateDialog(DevMode.demoUpdate(), true));
            return;
        }
        service.checkLatest().thenAccept(update ->
                Platform.runLater(() -> handleUpdateResult(update, manual)));
    }

    private void handleUpdateResult(UpdateInfo update, boolean manual) {
        if (update.failed()) {
            if (manual) {
                Dialogs.warn(I18n.get("updates.title"), I18n.get("updates.checkFailed"));
            }
        } else if (update.newer()) {
            showUpdateDialog(update, manual);
        } else if (manual) {
            Dialogs.info(I18n.get("updates.title"), I18n.format("updates.actual", VersionInfo.load().version()));
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
        alert.setTitle(I18n.get("updates.available.title"));
        alert.setHeaderText(I18n.format("updates.available.header", latestKey));
        alert.setResizable(true);

        String current = "v" + VersionInfo.load().version();
        VBox content = new VBox(8);
        content.setMaxWidth(Double.MAX_VALUE);
        Label versions = new Label(I18n.format("updates.available.content", current, latestKey));
        versions.setWrapText(true);
        versions.setMaxWidth(Double.MAX_VALUE);
        content.getChildren().add(versions);

        String notes = releaseNotes(update.body());
        if (!notes.isEmpty()) {
            // Заметки — read-only TextArea: можно выделять и копировать, переносится по ширине
            // окна, длинный текст прокручивается.
            TextArea notesArea = new TextArea(notes);
            notesArea.setEditable(false);
            notesArea.setWrapText(true);
            notesArea.setMinWidth(0);
            notesArea.setMaxWidth(Double.MAX_VALUE);
            int rows = (int) Math.min(12, Math.max(3, notes.lines().count()));
            notesArea.setPrefRowCount(rows);
            content.getChildren().addAll(new Label(I18n.get("updates.whatsNew")), notesArea);
        }
        alert.getDialogPane().setContent(content);

        ButtonType downloadButton = new ButtonType(I18n.get("updates.download"), ButtonBar.ButtonData.OK_DONE);
        ButtonType releaseButton = new ButtonType(I18n.get("updates.openRelease"));
        ButtonType folderButton = new ButtonType(I18n.get("updates.openFolder"));
        ButtonType closeButton = new ButtonType(I18n.get("button.close"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(downloadButton, releaseButton, folderButton, closeButton);

        Button folder = (Button) alert.getDialogPane().lookupButton(folderButton);
        boolean folderAvailable = ProgramFolder.isAvailable();
        folder.setDisable(!folderAvailable);
        if (!folderAvailable) {
            Tooltips.apply(folder, I18n.get("settings.packagedOnly"));
        }
        // Клик по кнопке папки не закрывает диалог: открываем папку, окно оставляем открытым.
        folder.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            ProgramFolder.open();
        });

        ThemeManager.applyTo(alert.getDialogPane());

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

    private void openLink(String url) {        if (url == null || url.isEmpty()) {
            Dialogs.error(I18n.get("common.error"), I18n.get("error.link"));
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            Dialogs.error(I18n.get("common.error"), I18n.format("error.openLink", e.getMessage()));
        }
    }

    /** Готовит описание релиза к показу: обрезает по границе допустимой длины. */
    private static String releaseNotes(String body) {
        if (body == null) {
            return "";
        }
        String notes = body.trim();
        if (notes.length() > MAX_NOTES_LENGTH) {
            notes = notes.substring(0, MAX_NOTES_LENGTH) + "…";
        }
        return notes;
    }
}