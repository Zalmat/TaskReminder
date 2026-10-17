package com.reminder.ui.controller;

import com.reminder.app.AppRestart;
import com.reminder.autostart.AutoStartService;
import com.reminder.i18n.I18n;
import com.reminder.i18n.Language;
import com.reminder.i18n.LanguageManager;
import com.reminder.i18n.LanguageStatus;
import com.reminder.settings.AppSettings;
import com.reminder.settings.DecimalSeparator;
import com.reminder.settings.Theme;
import com.reminder.ui.theme.ThemeManager;
import com.reminder.ui.util.Dialogs;
import com.reminder.ui.util.ProgramFolder;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;

/** Контроллер окна «Настройки». */
public class SettingsDialogController {

    @FXML private DialogPane dialogPane;
    @FXML private ListView<String> sectionList;
    @FXML private VBox appearancePane;
    @FXML private VBox generalPane;
    @FXML private VBox languagePane;
    @FXML private VBox exportPane;
    @FXML private RadioButton lightRadio;
    @FXML private RadioButton darkRadio;
    @FXML private ComboBox<String> typeChoice;
    @FXML private CheckBox autostartCheck;
    @FXML private Button openFolderButton;
    @FXML private ComboBox<Language> languageChoice;
    @FXML private Button restartButton;
    @FXML private Label languageWarningLabel;
    @FXML private ComboBox<DecimalSeparator> decimalChoice;

    private Scene mainScene;
    private Language previousLanguage;
    private boolean adjusting;

    public void init(Scene mainScene, List<String> workTypes) {
        this.mainScene = mainScene;

        sectionList.getItems().setAll(
                I18n.get("settings.section.appearance"),
                I18n.get("settings.section.general"),
                I18n.get("settings.section.language"),
                I18n.get("settings.section.export"));
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
            autostartCheck.setTooltip(new Tooltip(I18n.get("settings.packagedOnly")));
        }

        openFolderButton.setDisable(!ProgramFolder.isAvailable());
        if (!ProgramFolder.isAvailable()) {
            openFolderButton.setTooltip(new Tooltip(I18n.get("settings.packagedOnly")));
        }
        openFolderButton.setOnAction(e -> ProgramFolder.open());

        setupLanguage();
        setupDecimal();

        ButtonType applyType = new ButtonType(I18n.get("button.apply"), ButtonBar.ButtonData.APPLY);
        ButtonType closeType = new ButtonType(I18n.get("button.close"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().setAll(applyType, closeType);
        Button applyButton = (Button) dialogPane.lookupButton(applyType);
        applyButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            apply();
        });

        ThemeManager.applyTo(dialogPane);
    }

    private void setupLanguage() {
        List<Language> languages = new LanguageManager().load();
        languageChoice.getItems().setAll(languages);
        languageChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(Language language) {
                return language == null ? "" : languageLabel(language);
            }

            @Override
            public Language fromString(String string) {
                return null;
            }
        });
        languageChoice.setCellFactory(list -> languageCell());
        languageChoice.setButtonCell(languageCell());

        Language current = find(languages, I18n.currentCode());
        previousLanguage = current;
        languageChoice.setValue(current);
        languageChoice.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (adjusting) {
                return;
            }
            if (newValue != null && newValue.status() == LanguageStatus.DAMAGED) {
                adjusting = true;
                languageWarningLabel.setText(newValue.detail() + " — " + newValue.source());
                languageWarningLabel.setVisible(true);
                languageWarningLabel.setManaged(true);
                languageChoice.setValue(oldValue != null ? oldValue : previousLanguage);
                adjusting = false;
                return;
            }
            previousLanguage = newValue != null ? newValue : previousLanguage;
            languageWarningLabel.setVisible(false);
            languageWarningLabel.setManaged(false);
        });

        restartButton.setDisable(!AppRestart.isAvailable());
        if (!AppRestart.isAvailable()) {
            restartButton.setTooltip(new Tooltip(I18n.get("settings.packagedOnly")));
        }
        restartButton.setOnAction(e -> restartNow());
    }

    private void setupDecimal() {
        decimalChoice.getItems().setAll(DecimalSeparator.values());
        decimalChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(DecimalSeparator separator) {
                return separator == null ? "" : decimalLabel(separator);
            }

            @Override
            public DecimalSeparator fromString(String string) {
                return null;
            }
        });
        decimalChoice.setValue(AppSettings.decimalSeparator());
    }

    private String languageLabel(Language language) {
        String suffix = switch (language.status()) {
            case DAMAGED -> " - " + I18n.get("settings.language.damaged");
            case INCOMPLETE -> " - " + I18n.format("settings.language.incomplete",
                    language.presentKeys(), language.totalKeys());
            default -> "";
        };
        return language.displayName() + suffix;
    }

    private String decimalLabel(DecimalSeparator separator) {
        return switch (separator) {
            case SYSTEM -> I18n.get("settings.decimal.system");
            case COMMA -> I18n.get("settings.decimal.comma");
            default -> I18n.get("settings.decimal.dot");
        };
    }

    private ListCell<Language> languageCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Language item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setTooltip(null);
                    return;
                }
                setText(languageLabel(item));
                setStyle(item.status() == LanguageStatus.DAMAGED ? "-fx-text-fill: #999999;" : "");
                setTooltip(new Tooltip(item.source()));
            }
        };
    }

    private static Language find(List<Language> languages, String code) {
        for (Language language : languages) {
            if (language.code().equals(code) && language.status() != LanguageStatus.DAMAGED) {
                return language;
            }
        }
        for (Language language : languages) {
            if (language.status() != LanguageStatus.DAMAGED) {
                return language;
            }
        }
        return languages.isEmpty() ? null : languages.get(0);
    }

    private void restartNow() {
        apply();
        if (AppRestart.restart()) {
            Platform.exit();
        } else {
            Dialogs.warn(I18n.get("dialog.settings.title"), I18n.get("settings.language.restartUnavailable"));
        }
    }

    private void showSection(int index) {
        setActive(appearancePane, index == 0);
        setActive(generalPane, index == 1);
        setActive(languagePane, index == 2);
        setActive(exportPane, index == 3);
    }

    private static void setActive(VBox pane, boolean active) {
        pane.setVisible(active);
        pane.setManaged(active);
    }

    private void apply() {
        AppSettings.setTheme(darkRadio.isSelected() ? Theme.DARK : Theme.LIGHT);
        if (typeChoice.getValue() != null) {
            AppSettings.setDefaultType(typeChoice.getValue());
        }
        if (AutoStartService.isSupported()) {
            AutoStartService.setEnabled(autostartCheck.isSelected());
        }
        Language language = languageChoice.getValue();
        if (language != null && language.status() != LanguageStatus.DAMAGED) {
            AppSettings.setLanguage(language.code());
        }
        if (decimalChoice.getValue() != null) {
            AppSettings.setDecimalSeparator(decimalChoice.getValue());
        }
        ThemeManager.apply(mainScene);
        ThemeManager.applyTo(dialogPane);
    }
}
