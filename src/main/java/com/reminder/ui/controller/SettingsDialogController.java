package com.reminder.ui.controller;

import com.reminder.app.AppRestart;
import com.reminder.autostart.AutoStartService;
import com.reminder.i18n.I18n;
import com.reminder.i18n.Language;
import com.reminder.i18n.LanguageManager;
import com.reminder.i18n.LanguageStatus;
import com.reminder.settings.AppSettings;
import com.reminder.settings.DecimalSeparator;
import com.reminder.settings.ExportSaveMode;
import com.reminder.settings.Theme;
import com.reminder.service.ExportFormat;
import com.reminder.ui.theme.ThemeManager;
import com.reminder.ui.util.Dialogs;
import com.reminder.ui.util.ComboCells;
import com.reminder.ui.util.NumericFields;
import com.reminder.ui.util.ProgramFolder;
import com.reminder.ui.util.Tooltips;
import com.reminder.util.FileValidation;
import com.reminder.util.YamlTemplate;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
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
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Контроллер окна «Настройки». */
public class SettingsDialogController {

    @FXML private DialogPane dialogPane;
    @FXML private ListView<String> sectionList;
    @FXML private VBox appearancePane;
    @FXML private VBox generalPane;
    @FXML private VBox languagePane;
    @FXML private VBox exportPane;
    @FXML private VBox updatesPane;
    @FXML private VBox tasksPane;
    @FXML private RadioButton lightRadio;
    @FXML private RadioButton darkRadio;
    @FXML private ComboBox<String> typeChoice;
    @FXML private CheckBox autostartCheck;
    @FXML private Button openFolderButton;
    @FXML private ComboBox<Language> languageChoice;
    @FXML private Button restartButton;
    @FXML private Label languageWarningLabel;
    @FXML private ComboBox<DecimalSeparator> decimalChoice;
    @FXML private TextField exportDirField;
    @FXML private Button exportDirButton;
    @FXML private Button exportDirResetButton;
    @FXML private RadioButton exportModeSuggestRadio;
    @FXML private RadioButton exportModeAlwaysRadio;
    @FXML private VBox formatChecks;
    @FXML private CheckBox exportOpenFolderCheck;
    @FXML private CheckBox updateCheckEnabledCheck;
    @FXML private Spinner<Integer> updateHourSpinner;
    @FXML private Spinner<Integer> updateMinuteSpinner;
    @FXML private TextField taskImportFileField;
    @FXML private Button taskImportFileButton;
    @FXML private Button taskImportCreateExampleButton;
    @FXML private Button taskImportFileResetButton;
    @FXML private Label taskImportNoFileHint;
    @FXML private CheckBox taskImportOnStartupCheck;
    @FXML private CheckBox taskImportScheduleCheck;
    @FXML private Spinner<Integer> taskImportHourSpinner;
    @FXML private Spinner<Integer> taskImportMinuteSpinner;
    @FXML private Label taskImportTimeLabel;
    @FXML private HBox taskImportTimeBox;
    @FXML private CheckBox taskImportTrayNotifyCheck;
    @FXML private Button taskManageButton;
    @FXML private ComboBox<Integer> suggestionHistoryChoice;
    @FXML private CheckBox indexInactiveCheck;

    private Consumer<Path> importNow = path -> { };
    private Consumer<Window> openManager = window -> { };
    private Runnable onSuggestionsChanged = () -> { };

    private final Map<ExportFormat, CheckBox> formatCheckBoxes = new EnumMap<>(ExportFormat.class);

    private Scene mainScene;
    private Language previousLanguage;
    private boolean adjusting;

    public void init(Scene mainScene, List<String> workTypes, Consumer<Path> importNow,
                     Consumer<Window> openManager, Runnable onSuggestionsChanged) {
        this.mainScene = mainScene;
        this.importNow = importNow != null ? importNow : path -> { };
        this.openManager = openManager != null ? openManager : window -> { };
        this.onSuggestionsChanged = onSuggestionsChanged != null ? onSuggestionsChanged : () -> { };

        sectionList.getItems().setAll(
                I18n.get("settings.section.appearance"),
                I18n.get("settings.section.general"),
                I18n.get("settings.section.language"),
                I18n.get("settings.section.export"),
                I18n.get("settings.section.updates"),
                I18n.get("settings.section.tasks"));
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
        typeChoice.setCellFactory(list -> ComboCells.wrapping());

        if (AutoStartService.isSupported()) {
            autostartCheck.setSelected(AutoStartService.isEnabled());
        } else {
            autostartCheck.setSelected(false);
            autostartCheck.setDisable(true);
            Tooltips.apply(autostartCheck, I18n.get("settings.packagedOnly"));
        }

        openFolderButton.setDisable(!ProgramFolder.isAvailable());
        if (!ProgramFolder.isAvailable()) {
            Tooltips.apply(openFolderButton, I18n.get("settings.packagedOnly"));
        }
        openFolderButton.setOnAction(e -> ProgramFolder.open());

        setupLanguage();
        setupDecimal();
        setupExportOptions();
        setupUpdates();
        setupTasks();

        ButtonType applyType = new ButtonType(I18n.get("button.apply"), ButtonBar.ButtonData.APPLY);
        ButtonType closeType = new ButtonType(I18n.get("button.close"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType okType = new ButtonType(I18n.get("button.ok"), ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().setAll(applyType, closeType, okType);

        // Порядок кнопок задаётся внутренним ButtonBar (у DialogPane такого метода нет).
        dialogPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> {
                    Node bar = dialogPane.lookup(".button-bar");
                    if (bar instanceof ButtonBar buttonBar) {
                        buttonBar.setButtonOrder(ButtonBar.BUTTON_ORDER_NONE);
                    }
                });
            }
        });

        Button applyButton = (Button) dialogPane.lookupButton(applyType);
        applyButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            apply();
        });

        Button okButton = (Button) dialogPane.lookupButton(okType);
        Tooltips.apply(okButton, I18n.get("button.ok.tooltip"));
        // «ОК» применяет настройки и закрывает окно (событие не гасим — диалог закроется сам).
        okButton.addEventFilter(ActionEvent.ACTION, event -> apply());

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
                languageWarningLabel.setText(newValue.detail() + "\n" + sourceLabel(newValue));
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
            Tooltips.apply(restartButton, I18n.get("settings.packagedOnly"));
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

    private void setupExportOptions() {
        exportDirField.setText(AppSettings.exportDirectory());
        exportDirButton.setOnAction(e -> chooseExportDirectory());
        exportDirResetButton.setOnAction(e -> exportDirField.clear());

        if (AppSettings.exportSaveMode() == ExportSaveMode.ALWAYS) {
            exportModeAlwaysRadio.setSelected(true);
        } else {
            exportModeSuggestRadio.setSelected(true);
        }

        Set<ExportFormat> selected = AppSettings.exportFolderFormats();
        for (ExportFormat format : ExportFormat.values()) {
            CheckBox checkBox = new CheckBox(format.label());
            checkBox.setSelected(selected.contains(format));
            formatCheckBoxes.put(format, checkBox);
            formatChecks.getChildren().add(checkBox);
        }

        exportOpenFolderCheck.setSelected(AppSettings.exportOpenFolder());
    }

    private void setupUpdates() {
        updateCheckEnabledCheck.setSelected(AppSettings.updateCheckEnabled());
        updateHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, 23, AppSettings.updateCheckHour()));
        updateMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, 59, AppSettings.updateCheckMinute()));
        NumericFields.timeSpinner(updateHourSpinner, 23);
        NumericFields.timeSpinner(updateMinuteSpinner, 59);
    }

    private void setupTasks() {
        taskImportFileField.setText(AppSettings.taskImportFile());
        taskImportFileButton.setOnAction(e -> chooseTaskImportFile());
        taskImportCreateExampleButton.setOnAction(e -> createTaskImportExample());
        taskImportFileResetButton.setOnAction(e -> {
            taskImportFileField.clear();
            taskImportOnStartupCheck.setSelected(false);
            taskImportScheduleCheck.setSelected(false);
            updateTaskImportControls();
        });
        Tooltips.apply(taskImportFileResetButton, I18n.get("settings.tasks.reset.tip"));
        taskImportOnStartupCheck.setSelected(AppSettings.taskImportOnStartup());
        taskImportScheduleCheck.setSelected(AppSettings.taskImportScheduleEnabled());
        taskImportScheduleCheck.selectedProperty().addListener((obs, oldValue, newValue) ->
                updateTaskImportControls());
        taskImportHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, 23, AppSettings.taskImportHour()));
        taskImportMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, 59, AppSettings.taskImportMinute()));
        NumericFields.timeSpinner(taskImportHourSpinner, 23);
        NumericFields.timeSpinner(taskImportMinuteSpinner, 59);
        taskImportTrayNotifyCheck.setSelected(AppSettings.taskImportTrayNotify());
        taskManageButton.setOnAction(e -> openManager.accept(window()));

        suggestionHistoryChoice.getItems().setAll(12, 24, 36, 0);
        suggestionHistoryChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer months) {
                if (months == null) {
                    return "";
                }
                return months == 0 ? I18n.get("settings.tasks.history.all")
                        : I18n.format("settings.tasks.history.months", months);
            }

            @Override
            public Integer fromString(String text) {
                return null;
            }
        });
        int months = AppSettings.suggestionHistoryMonths();
        if (!suggestionHistoryChoice.getItems().contains(months)) {
            suggestionHistoryChoice.getItems().add(months);
        }
        suggestionHistoryChoice.setValue(months);
        indexInactiveCheck.setSelected(AppSettings.indexOnlyWhenInactive());
        updateTaskImportControls();
    }

    private Window window() {
        return dialogPane.getScene() != null ? dialogPane.getScene().getWindow() : null;
    }

    private void updateTaskImportControls() {
        boolean hasFile = taskImportFileField.getText() != null && !taskImportFileField.getText().isBlank();
        taskImportOnStartupCheck.setDisable(!hasFile);
        taskImportScheduleCheck.setDisable(!hasFile);
        taskImportHourSpinner.setDisable(!hasFile);
        taskImportMinuteSpinner.setDisable(!hasFile);
        taskImportTrayNotifyCheck.setDisable(!hasFile);
        taskImportNoFileHint.setVisible(!hasFile);
        taskImportNoFileHint.setManaged(!hasFile);
        boolean showTime = hasFile && taskImportScheduleCheck.isSelected();
        taskImportTimeLabel.setVisible(showTime);
        taskImportTimeLabel.setManaged(showTime);
        taskImportTimeBox.setVisible(showTime);
        taskImportTimeBox.setManaged(showTime);
    }

    private void chooseTaskImportFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("settings.tasks.file"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("yaml.filter"), "*.yaml", "*.yml"));
        Window owner = dialogPane.getScene() != null ? dialogPane.getScene().getWindow() : null;
        File file = chooser.showOpenDialog(owner);
        if (file == null) {
            return;
        }
        Path path = file.toPath();
        if (!FileValidation.isReadableYaml(path)) {
            Dialogs.error(I18n.get("common.error"), I18n.get("yaml.invalidFile"));
            return;
        }
        taskImportFileField.setText(file.getAbsolutePath());
        taskImportOnStartupCheck.setSelected(true);
        updateTaskImportControls();
        if (Dialogs.yesNo(I18n.get("settings.tasks.choose.importNow.title"),
                I18n.get("settings.tasks.choose.importNow.header"),
                I18n.format("settings.tasks.choose.importNow.text", I18n.get("main.loadYaml")))) {
            importNow.accept(path);
        }
    }

    private void createTaskImportExample() {
        Dialogs.info(I18n.get("settings.tasks.createExample.info.title"),
                I18n.get("settings.tasks.createExample.info.text"));

        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("settings.tasks.createExample"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("yaml.filter"), "*.yaml", "*.yml"));
        chooser.setInitialFileName("tasks-import.yaml");
        Window owner = dialogPane.getScene() != null ? dialogPane.getScene().getWindow() : null;
        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }
        try {
            Path path = file.toPath();
            String name = file.getName().toLowerCase();
            if (!name.endsWith(".yaml") && !name.endsWith(".yml")) {
                path = Path.of(file.getAbsolutePath() + ".yaml");
            }
            Files.writeString(path, YamlTemplate.content(), StandardCharsets.UTF_8);
            taskImportFileField.setText(path.toString());
            taskImportOnStartupCheck.setSelected(true);
            updateTaskImportControls();
            if (Dialogs.yesNo(I18n.get("settings.tasks.createExample.apply.title"),
                    I18n.get("settings.tasks.createExample.apply.header"),
                    I18n.format("settings.tasks.createExample.apply.text", I18n.get("main.loadYaml")))) {
                importNow.accept(path);
            }
        } catch (Exception e) {
            Dialogs.error(I18n.get("common.error"),
                    I18n.format("tasks.import.error", Dialogs.reason(e)));
        }
    }

    private void chooseExportDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(I18n.get("settings.export.folder"));
        AppSettings.validExportDirectory().ifPresent(path -> chooser.setInitialDirectory(path.toFile()));
        Window owner = dialogPane.getScene() != null ? dialogPane.getScene().getWindow() : null;
        File directory = chooser.showDialog(owner);
        if (directory != null) {
            Path path = directory.toPath();
            if (!Files.isDirectory(path) || !Files.isWritable(path)) {
                Dialogs.error(I18n.get("common.error"), I18n.get("settings.export.invalidDir"));
                return;
            }
            exportDirField.setText(directory.getAbsolutePath());
        }
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

    private String sourceLabel(Language language) {
        if ("built-in".equals(language.source())) {
            return I18n.get("settings.language.source.builtin");
        }
        return I18n.format("settings.language.source.file", language.source());
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
                setTooltip(Tooltips.create(languageLabel(item) + " — " + sourceLabel(item)));
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
        setActive(updatesPane, index == 4);
        setActive(tasksPane, index == 5);
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
        AppSettings.setExportDirectory(exportDirField.getText() == null ? "" : exportDirField.getText().trim());
        AppSettings.setExportSaveMode(exportModeAlwaysRadio.isSelected()
                ? ExportSaveMode.ALWAYS : ExportSaveMode.SUGGEST);
        Set<ExportFormat> formats = EnumSet.noneOf(ExportFormat.class);
        for (Map.Entry<ExportFormat, CheckBox> entry : formatCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                formats.add(entry.getKey());
            }
        }
        AppSettings.setExportFolderFormats(formats);
        AppSettings.setExportOpenFolder(exportOpenFolderCheck.isSelected());
        AppSettings.setUpdateCheckEnabled(updateCheckEnabledCheck.isSelected());
        AppSettings.setUpdateCheckTime(updateHourSpinner.getValue(), updateMinuteSpinner.getValue());
        String importFile = taskImportFileField.getText() == null ? "" : taskImportFileField.getText().trim();
        boolean hasImportFile = !importFile.isBlank();
        AppSettings.setTaskImportFile(importFile);
        AppSettings.setTaskImportOnStartup(hasImportFile && taskImportOnStartupCheck.isSelected());
        AppSettings.setTaskImportScheduleEnabled(hasImportFile && taskImportScheduleCheck.isSelected());
        AppSettings.setTaskImportTime(taskImportHourSpinner.getValue(), taskImportMinuteSpinner.getValue());
        AppSettings.setTaskImportTrayNotify(hasImportFile && taskImportTrayNotifyCheck.isSelected());

        int oldHistory = AppSettings.suggestionHistoryMonths();
        boolean oldInactive = AppSettings.indexOnlyWhenInactive();
        int history = suggestionHistoryChoice.getValue() == null
                ? 24 : suggestionHistoryChoice.getValue();
        AppSettings.setSuggestionHistoryMonths(history);
        AppSettings.setIndexOnlyWhenInactive(indexInactiveCheck.isSelected());
        if (oldHistory != history || oldInactive != indexInactiveCheck.isSelected()) {
            onSuggestionsChanged.run();
        }
        ThemeManager.apply(mainScene);
        ThemeManager.applyTo(dialogPane);
    }
}
