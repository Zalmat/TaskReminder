package com.reminder;

import com.reminder.app.DevMode;
import com.reminder.app.LogConfig;
import com.reminder.app.SingleInstanceLock;
import com.reminder.autostart.AutoStartService;
import com.reminder.i18n.I18n;
import com.reminder.ui.TrayService;
import com.reminder.ui.controller.MainController;
import com.reminder.ui.theme.ThemeManager;
import com.reminder.ui.util.AppIcons;
import com.reminder.ui.util.Dialogs;
import com.reminder.util.Log;
import com.reminder.util.VersionInfo;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private MainController mainController;
    private TrayService tray;

    @Override
    public void start(Stage primaryStage) {
        if (!SingleInstanceLock.acquire()) {
            if (!getParameters().getRaw().contains("--minimized")) {
                Dialogs.info(I18n.get("app.startup.title"), I18n.get("app.startup.alreadyRunning"));
            }
            Platform.exit();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
            loader.setResources(I18n.bundle());
            Parent root = loader.load();
            mainController = loader.getController();
            mainController.setExitAction(this::performQuit);
            mainController.setTrayNotifier((caption, text) -> {
                if (tray != null) {
                    tray.showMessage(caption, text);
                }
            });

            Scene scene = new Scene(root, 1200, 800);
            ThemeManager.apply(scene);
            AppIcons.apply(primaryStage);

            primaryStage.setTitle(I18n.get("app.title") + "  v" + VersionInfo.load().version());
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);

            primaryStage.setOnCloseRequest(e -> {
                e.consume();
                quit();
            });

            boolean startMinimized = getParameters().getRaw().contains("--minimized");
            try {
                AutoStartService.syncPath();
            } catch (Exception ignored) {
                // автозагрузка не критична для запуска
            }

            setupTray(primaryStage);

            if (I18n.hasStartupWarning()) {
                Dialogs.warn(I18n.get("settings.language.warning.title"),
                        I18n.format("settings.language.warning.text", I18n.startupWarningDetail()));
            }

            if (startMinimized && tray != null) {
                tray.showMessage(I18n.get("app.tray.caption"), I18n.get("app.tray.startedMinimized"));
            } else {
                primaryStage.show();
            }
        } catch (Exception e) {
            Log.log("App", "startup failed: " + Dialogs.reason(e), e);
            System.err.println("Ошибка запуска: " + Dialogs.reason(e));
            Dialogs.error(I18n.get("app.startError.title"), Dialogs.reason(e));
        }
    }

    private void setupTray(Stage primaryStage) {
        if (!TrayService.isSupported()) {
            Log.log("Tray", "SystemTray not supported");
            return;
        }
        Platform.setImplicitExit(false);
        tray = new TrayService();
        tray.install(primaryStage, this::quit);

        primaryStage.iconifiedProperty().addListener((obs, wasIconified, isIconified) -> {
            if (Boolean.TRUE.equals(isIconified)) {
                Platform.runLater(() -> {
                    primaryStage.hide();
                    if (tray != null) {
                        tray.showMessage(I18n.get("app.tray.caption"), I18n.get("app.tray.minimized"));
                    }
                });
            }
        });
    }

    private void quit() {
        if (Dialogs.confirm(I18n.get("app.quit.title"),
                I18n.get("app.quit.header"), I18n.get("app.quit.content"))) {
            performQuit();
        }
    }

    /** Корректное завершение без подтверждения (используется в т.ч. при автообновлении). */
    private void performQuit() {
        if (mainController != null) {
            mainController.shutdown();
        }
        if (tray != null) {
            tray.remove();
        }
        SingleInstanceLock.release();
        Platform.exit();
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        LogConfig.init(java.util.Arrays.asList(args));
        DevMode.init(java.util.Arrays.asList(args));
        // Инициализация локализации: читает сохранённый язык и фиксирует локаль до старта JavaFX.
        I18n.init();
        LogConfig.logStartupInfo();
        launch(args);
    }
}
