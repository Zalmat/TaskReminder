package com.reminder.ui.controller;

import com.reminder.settings.AppSettings;
import com.reminder.settings.Theme;
import com.reminder.ui.theme.ThemeManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.VBox;

import java.util.List;

/** Контроллер окна «Настройки». */
public class SettingsDialogController {

    @FXML private DialogPane dialogPane;
    @FXML private ListView<String> sectionList;
    @FXML private VBox appearancePane;
    @FXML private VBox generalPane;
    @FXML private RadioButton lightRadio;
    @FXML private RadioButton darkRadio;
    @FXML private ComboBox<String> typeChoice;

    private Scene mainScene;

    public void init(Scene mainScene, List<String> workTypes) {
        this.mainScene = mainScene;

        sectionList.getItems().addAll("Внешний вид", "Общие");
        sectionList.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) ->
                showSection(newIdx.intValue()));
        sectionList.getSelectionModel().selectFirst();

        if (AppSettings.theme() == Theme.DARK) {
            darkRadio.setSelected(true);
        } else {
            lightRadio.setSelected(true);
        }

        typeChoice.getItems().setAll(workTypes);
        typeChoice.setValue(AppSettings.defaultType());

        ButtonType applyType = new ButtonType("Применить", ButtonBar.ButtonData.APPLY);
        ButtonType closeType = new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().setAll(applyType, closeType);
        Button applyButton = (Button) dialogPane.lookupButton(applyType);
        applyButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            apply();
        });

        ThemeManager.applyTo(dialogPane);
    }

    private void showSection(int index) {
        boolean appearance = index <= 0;
        appearancePane.setVisible(appearance);
        appearancePane.setManaged(appearance);
        generalPane.setVisible(!appearance);
        generalPane.setManaged(!appearance);
    }

    private void apply() {
        AppSettings.setTheme(darkRadio.isSelected() ? Theme.DARK : Theme.LIGHT);
        if (typeChoice.getValue() != null) {
            AppSettings.setDefaultType(typeChoice.getValue());
        }
        ThemeManager.apply(mainScene);
        ThemeManager.applyTo(dialogPane);
    }
}