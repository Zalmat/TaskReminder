package com.reminder;

import com.reminder.autostart.AutoStartService;
import com.reminder.ui.TrayService;
import com.reminder.ui.controller.MainController;
import com.reminder.ui.theme.ThemeManager;
import com.reminder.ui.util.Dialogs;
import com.reminder.util.VersionInfo;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Locale;

public class App extends Application {

    private MainController mainController;
    private TrayService tray;

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
            Parent root = loader.load();
            mainController = loader.getController();

            Scene scene = new Scene(root, 1200, 800);
            ThemeManager.apply(scene);

            primaryStage.setTitle("📋 Напоминалка - Учет рабочего времени  v" + VersionInfo.load().version());
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

            if (startMinimized && tray != null) {
                tray.showMessage("Напоминалка", "Приложение запущено в трее");
            } else {
                primaryStage.show();
            }
        } catch (Exception e) {
            System.err.println("Ошибка запуска: " + Dialogs.reason(e));
            Dialogs.error("Ошибка запуска", Dialogs.reason(e));
        }
    }

    private void setupTray(Stage primaryStage) {
        if (!TrayService.isSupported()) {
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
                        tray.showMessage("Напоминалка", "Приложение свёрнуто в трей");
                    }
                });
            }
        });
    }

    private void quit() {
        if (Dialogs.confirm("Выход", "Вы уверены, что хотите выйти?", "Все данные будут сохранены.")) {
            if (mainController != null) {
                mainController.shutdown();
            }
            if (tray != null) {
                tray.remove();
            }
            Platform.exit();
        }
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        // Фиксируем русскую локаль, чтобы форматирование и начало недели были стабильными.
        Locale.setDefault(Locale.forLanguageTag("ru-RU"));
        launch(args);
    }
}