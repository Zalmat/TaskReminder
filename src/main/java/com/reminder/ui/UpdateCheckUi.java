package com.reminder.ui;

import com.reminder.app.AppAutoUpdater;
import com.reminder.app.DevMode;
import com.reminder.i18n.I18n;
import com.reminder.model.UpdateInfo;
import com.reminder.service.UpdateCheckService;
import com.reminder.service.UpdateDownloader;
import com.reminder.ui.theme.ThemeManager;
import com.reminder.ui.util.Dialogs;
import com.reminder.ui.util.ProgramFolder;
import com.reminder.ui.util.Tooltips;
import com.reminder.util.VersionInfo;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/** UI-обвязка проверки обновлений через {@link UpdateCheckService}. */
public class UpdateCheckUi {

    private static final int MAX_NOTES_LENGTH = 4000;

    /** Режим проверки обновлений. */
    public enum CheckMode {
        /** Ручная (клик по кнопке): всегда показывает результат окном. */
        MANUAL,
        /** При запуске: окно при наличии обновления (один раз на версию). */
        STARTUP,
        /** Фоновая ежедневная: без окна, только индикатор кнопки. */
        SILENT
    }

    private final UpdateCheckService service;
    private final UpdateDownloader downloader = new UpdateDownloader();
    private Runnable exitAction = () -> { };
    private Consumer<UpdateInfo> updateIndicator;
    private UpdateInfo pendingUpdate;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> dailyTask;

    public UpdateCheckUi(UpdateCheckService service) {
        this.service = service;
    }

    /** Колбэк-индикатор: получает найденное обновление или {@code null}. */
    public void setUpdateIndicator(Consumer<UpdateInfo> consumer) {
        this.updateIndicator = consumer;
    }

    /** Действие корректного завершения приложения (задаётся приложением) — для автообновления. */
    public void setExitAction(Runnable exitAction) {
        if (exitAction != null) {
            this.exitAction = exitAction;
        }
    }

    /** Клик по кнопке проверки обновлений (ручной режим). */
    public void manualCheck() {
        if (pendingUpdate != null) {
            showUpdateDialog(pendingUpdate, true);
        } else {
            checkForUpdates(CheckMode.MANUAL);
        }
    }

    /** Проверяет наличие обновлений в заданном режиме. */
    public void checkForUpdates(CheckMode mode) {
        if (DevMode.isEnabled()) {
            UpdateInfo demo = DevMode.demoUpdate();
            Platform.runLater(() -> handleUpdateResult(demo, mode));
            return;
        }
        service.checkLatest().thenAccept(update ->
                Platform.runLater(() -> handleUpdateResult(update, mode)));
    }

    private void handleUpdateResult(UpdateInfo update, CheckMode mode) {
        if (update.failed()) {
            if (mode == CheckMode.MANUAL) {
                Dialogs.warn(I18n.get("updates.title"), I18n.get("updates.checkFailed"));
            }
            return;
        }
        if (update.newer()) {
            pendingUpdate = update;
            notifyIndicator(update);
            if (mode != CheckMode.SILENT) {
                showUpdateDialog(update, mode == CheckMode.MANUAL);
            }
        } else {
            pendingUpdate = null;
            notifyIndicator(null);
            if (mode == CheckMode.MANUAL) {
                Dialogs.info(I18n.get("updates.title"),
                        I18n.format("updates.actual", VersionInfo.load().version()));
            }
        }
    }

    private void notifyIndicator(UpdateInfo update) {
        if (updateIndicator != null) {
            updateIndicator.accept(update);
        }
    }

    /** Включает/выключает ежедневную фоновую проверку по настройкам. */
    public void applySchedule(boolean enabled, int hour, int minute) {
        if (enabled) {
            startDailyChecks(hour, minute);
        } else {
            stopDailyChecks();
        }
    }

    private void startDailyChecks(int hour, int minute) {
        stopDailyChecks();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "update-check");
            thread.setDaemon(true);
            return thread;
        });
        long initialDelay = initialDelayMillis(LocalDateTime.now(), hour, minute);
        long period = Duration.ofDays(1).toMillis();
        dailyTask = scheduler.scheduleAtFixedRate(() -> checkForUpdates(CheckMode.SILENT),
                initialDelay, period, TimeUnit.MILLISECONDS);
    }

    private void stopDailyChecks() {
        if (dailyTask != null) {
            dailyTask.cancel(false);
            dailyTask = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    /** Останавливает фоновые проверки (при завершении приложения). */
    public void shutdown() {
        stopDailyChecks();
    }

    /** Задержка до следующего срабатывания в {@code hour:minute} (сегодня или завтра). */
    static long initialDelayMillis(LocalDateTime now, int hour, int minute) {
        LocalDateTime next = now.toLocalDate().atTime(hour, minute);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return Duration.between(now, next).toMillis();
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

        ButtonType updateButton = new ButtonType(I18n.get("updates.update"), ButtonBar.ButtonData.OK_DONE);
        ButtonType downloadButton = new ButtonType(I18n.get("updates.download"));
        ButtonType releaseButton = new ButtonType(I18n.get("updates.openRelease"));
        ButtonType folderButton = new ButtonType(I18n.get("updates.openFolder"));
        ButtonType closeButton = new ButtonType(I18n.get("button.close"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(updateButton, downloadButton, releaseButton, folderButton, closeButton);

        Button updateControl = (Button) alert.getDialogPane().lookupButton(updateButton);
        boolean archiveAvailable = update.hasArchive();
        boolean updateAvailable = archiveAvailable && AppAutoUpdater.isAvailable();
        updateControl.setDisable(!updateAvailable);
        if (!updateAvailable) {
            Tooltips.apply(updateControl, I18n.get(archiveAvailable
                    ? "updates.updaterUnavailable" : "updates.noArchive"));
        }

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
        if (result.isEmpty()) {
            return;
        }
        ButtonType pressed = result.get();
        if (pressed == updateButton) {
            performUpdate(update);
        } else if (pressed == downloadButton) {
            String url = !update.archiveUrl().isEmpty() ? update.archiveUrl() : update.releaseUrl();
            openLink(url);
        } else if (pressed == releaseButton) {
            openLink(update.releaseUrl());
        }
    }

    /** Подтверждение, скачивание архива с прогрессом, запуск апдейтера и завершение приложения. */
    private void performUpdate(UpdateInfo update) {
        if (!Dialogs.confirm(I18n.get("updates.install.title"),
                I18n.get("updates.install.header"), I18n.get("updates.install.content"))) {
            return;
        }

        ProgressBar bar = new ProgressBar();
        bar.setPrefWidth(320);
        Label label = new Label(I18n.get("updates.downloading"));
        label.setWrapText(true);
        VBox box = new VBox(10, label, bar);
        box.setPadding(new Insets(10));

        Dialog<Void> progress = new Dialog<>();
        progress.setTitle(I18n.get("updates.install.title"));
        progress.getDialogPane().setContent(box);
        progress.setResizable(false);
        progress.setOnCloseRequest(javafx.event.Event::consume);
        ThemeManager.applyTo(progress.getDialogPane());

        Thread worker = new Thread(() -> {
            try {
                Path tempDir = Files.createTempDirectory("tr-update-");
                Path archive = downloader.download(update, tempDir, (downloaded, total) ->
                        Platform.runLater(() -> {
                            if (total > 0) {
                                bar.setProgress((double) downloaded / total);
                            }
                        }));
                Platform.runLater(() -> {
                    progress.close();
                    try {
                        AppAutoUpdater.start(archive);
                        exitAction.run();
                    } catch (IOException e) {
                        Dialogs.error(I18n.get("common.error"),
                                I18n.format("updates.downloadFailed", Dialogs.reason(e)));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progress.close();
                    Dialogs.error(I18n.get("common.error"),
                            I18n.format("updates.downloadFailed", Dialogs.reason(e)));
                });
            }
        }, "update-download");
        worker.setDaemon(true);
        worker.start();

        progress.showAndWait();
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