package com.reminder.ui.controller;

import com.reminder.autostart.AutoStartService;
import com.reminder.settings.AppSettings;
import com.reminder.settings.Theme;
import com.reminder.ui.theme.ThemeManager;
import com.reminder.ui.util.ProgramFolder;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tooltip;
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
    @FXML private CheckBox autostartCheck;
    @FXML private Button openFolderButton;

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

        if (AutoStartService.isSupported()) {
            autostartCheck.setSelected(AutoStartService.isEnabled());
        } else {
            autostartCheck.setSelected(false);
            autostartCheck.setDisable(true);
            autostartCheck.setTooltip(new Tooltip("Доступно только в собранной версии"));
        }

        openFolderButton.setDisable(!ProgramFolder.isAvailable());
        if (!ProgramFolder.isAvailable()) {
            openFolderButton.setTooltip(new Tooltip("Доступно только в собранной версии"));
        }
        openFolderButton.setOnAction(e -> ProgramFolder.open());

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
        if (AutoStartService.isSupported()) {
            AutoStartService.setEnabled(autostartCheck.isSelected());
        }
        ThemeManager.apply(mainScene);
        ThemeManager.applyTo(dialogPane);
    }
}