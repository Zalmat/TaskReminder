package com.reminder;

import com.reminder.ui.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.util.Locale;

public class App extends Application {

    private MainController mainController;

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
            Parent root = loader.load();
            mainController = loader.getController();

            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());

            primaryStage.setTitle("📋 Напоминалка - Учет рабочего времени");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);

            primaryStage.setOnCloseRequest(e -> {
                e.consume();
                handleCloseRequest(primaryStage);
            });

            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Ошибка запуска", e.getMessage());
        }
    }

    private void handleCloseRequest(Stage stage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Выход");
        alert.setHeaderText("Вы уверены, что хотите выйти?");
        alert.setContentText("Все данные будут сохранены.");

        ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);
        if (result == ButtonType.OK) {
            if (mainController != null) {
                mainController.shutdown();
            }
            stage.close();
        }
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        // Фиксируем русскую локаль, чтобы форматирование и начало недели были стабильными.
        Locale.setDefault(Locale.forLanguageTag("ru-RU"));
        launch(args);
    }
}