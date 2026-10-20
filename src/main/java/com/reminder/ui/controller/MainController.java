package com.reminder.ui.controller;

import com.reminder.i18n.I18n;
import com.reminder.catalog.importing.CatalogImportService;
import com.reminder.catalog.importing.ImportOutcome;
import com.reminder.catalog.importing.YamlCatalogImporter;
import com.reminder.importing.ImportScheduler;
import com.reminder.importing.ImportStateStore;
import com.reminder.model.UpdateInfo;
import com.reminder.model.WeekEntry;
import com.reminder.model.WorkEntry;
import com.reminder.service.ExportService;
import com.reminder.service.ReminderService;
import com.reminder.service.TaskService;
import com.reminder.service.UpdateCheckService;
import com.reminder.service.WorkTimeService;
import com.reminder.settings.AppSettings;
import com.reminder.settings.WorkTypes;
import com.reminder.suggest.SuggestionService;
import com.reminder.ui.ImportNotifier;
import com.reminder.ui.SuggestionIndexingCoordinator;
import com.reminder.ui.UpdateCheckUi;
import com.reminder.ui.component.ClockUi;
import com.reminder.ui.component.EasterEgg;
import com.reminder.ui.component.EntryForm;
import com.reminder.ui.dialog.SuggestionManagerDialog;
import com.reminder.ui.theme.ThemeManager;
import com.reminder.ui.util.AppIcons;
import com.reminder.ui.util.DatePickerUtils;
import com.reminder.ui.util.Dialogs;
import com.reminder.ui.view.DayView;
import com.reminder.ui.view.ReminderUi;
import com.reminder.ui.view.WeekView;
import com.reminder.util.Log;
import com.reminder.util.FileValidation;
import com.reminder.util.YamlTemplate;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.BiConsumer;

/** Контроллер главного окна. Собирает слой UI поверх сервисов бизнес-логики. */
public class MainController {

    @FXML private VBox dayPanel;
    @FXML private VBox weekPanel;
    @FXML private VBox hoursContainer;
    @FXML private Label dayTotalLabel;
    @FXML private Label dayRemainingLabel;
    @FXML private Label dayOvertimeLabel;
    @FXML private Label weekTotalLabel;
    @FXML private Label clockLabel;
    @FXML private Label importBell;
    @FXML private VBox indexingStatus;
    @FXML private ProgressIndicator indexingSpinner;
    @FXML private Label indexingLabel;
    @FXML private ProgressBar indexingProgress;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> projectChoice;
    @FXML private ComboBox<String> taskChoice;
    @FXML private ComboBox<String> typeChoice;
    @FXML private TextField hoursField;
    @FXML private TextArea commentArea;
    @FXML private RadioButton dayRadio;
    @FXML private RadioButton weekRadio;
    @FXML private TableView<WorkEntry> dayTableView;
    @FXML private TableView<WeekEntry> weekTableView;
    @FXML private TableColumn<WorkEntry, String> dayProjectColumn;
    @FXML private TableColumn<WorkEntry, String> dayTaskColumn;
    @FXML private TableColumn<WorkEntry, String> dayTypeColumn;
    @FXML private TableColumn<WorkEntry, Integer> dayHoursColumn;
    @FXML private TableColumn<WorkEntry, String> dayCommentColumn;
    @FXML private TableColumn<WeekEntry, String> weekProjectColumn;
    @FXML private TableColumn<WeekEntry, String> weekTaskColumn;
    @FXML private TableColumn<WeekEntry, String> weekTypeColumn;
    @FXML private TableColumn<WeekEntry, String> weekCommentColumn;
    @FXML private TableColumn<WeekEntry, Integer> weekTotalColumn;
    @FXML private Button addButton;
    @FXML private Button loadYamlButton;
    @FXML private Button templateButton;
    @FXML private Button clearButton;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button todayButton;
    @FXML private Button holidayButton;
    @FXML private Button reminderButton;
    @FXML private Button exportButton;
    @FXML private Button updateButton;
    @FXML private MenuItem settingsItem;

    private final TaskService taskService = new TaskService();
    private final WorkTimeService workTimeService = new WorkTimeService();
    private final ReminderService reminderService = new ReminderService();
    private final ExportService exportService = new ExportService();
    private final UpdateCheckService updateCheckService = new UpdateCheckService();
    private final ImportStateStore importState = new ImportStateStore();
    private final ImportScheduler importScheduler = new ImportScheduler();
    private SuggestionService suggestionService;
    private SuggestionIndexingCoordinator suggestionIndexing;
    private Timeline indexingPoller;
    private CatalogImportService catalogImportService;
    private ImportNotifier importNotifier;
    private BiConsumer<String, String> trayNotifier = (caption, text) -> { };
    private UpdateCheckUi updateCheckUi;
    private Runnable exitAction = () -> { };
    private final EasterEgg easterEgg = new EasterEgg();
    private EntryForm form;
    private final ReminderUi reminderUi = new ReminderUi(reminderService);
    private WeekView weekView;
    private DayView dayView;
    private boolean isWeekView = false;

    @FXML
    private void initialize() {
        suggestionService = new SuggestionService(taskService);
        form = new EntryForm(projectChoice, taskChoice, typeChoice, hoursField, commentArea,
                suggestionService, this::onSuggestionDataChanged);
        taskService.ensureDefaultTypes(WorkTypes.defaults());
        form.loadTypeChoices(taskService);

        configureDatePicker();

        dayView = new DayView(dayTableView, dayProjectColumn, dayTaskColumn, dayTypeColumn,
                dayHoursColumn, dayCommentColumn, dayTotalLabel, dayRemainingLabel, dayOvertimeLabel,
                form, taskService, workTimeService, datePicker);
        dayView.configure();

        weekView = new WeekView(weekTableView, weekProjectColumn, weekTaskColumn, weekTypeColumn,
                weekCommentColumn, weekTotalColumn, weekTotalLabel, form, taskService, workTimeService, datePicker);
        weekView.configure();

        wireButtons();
        wireViewToggle();
        form.refreshSuggestions();

        reminderUi.setup();
        setupUpdateCheck();
        setupTaskImport();
        ClockUi.start(clockLabel);

        dayView.update(datePicker.getValue());
        reminderService.startMonitoring();
    }

    // ==================== НАСТРОЙКА UI ====================

    private void configureDatePicker() {
        DatePickerUtils.applyTo(datePicker);
        datePicker.setValue(LocalDate.now());
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (isWeekView) {
                    weekView.update(newVal);
                } else {
                    dayView.update(newVal);
                }
            }
        });
    }

    private void wireButtons() {
        prevButton.setOnAction(e -> {
            if (isWeekView) {
                datePicker.setValue(datePicker.getValue().minusWeeks(1));
            } else {
                datePicker.setValue(datePicker.getValue().minusDays(1));
            }
        });
        nextButton.setOnAction(e -> {
            if (isWeekView) {
                datePicker.setValue(datePicker.getValue().plusWeeks(1));
            } else {
                datePicker.setValue(datePicker.getValue().plusDays(1));
            }
        });
        todayButton.setOnAction(e -> datePicker.setValue(LocalDate.now()));

        addButton.setOnAction(e -> {
            if (isWeekView) {
                weekView.add();
            } else {
                dayView.add();
            }
        });
        loadYamlButton.setOnAction(e -> loadTasksFromYaml());
        templateButton.setOnAction(e -> downloadYamlTemplate());
        clearButton.setOnAction(e -> {
            if (isWeekView) {
                weekView.clear();
            } else {
                dayView.clear();
            }
        });
        holidayButton.setOnAction(e -> openHolidaysDialog());
        reminderButton.setOnAction(e -> openRemindersDialog());
        exportButton.setOnAction(e -> openExportDialog());
        settingsItem.setOnAction(e -> openSettingsDialog());
    }

    private void wireViewToggle() {
        dayRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                showDayView();
            }
        });
        weekRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                showWeekView();
            }
        });
    }

    // ==================== РЕЖИМЫ ПРОСМОТРА ====================

    private void showDayView() {
        isWeekView = false;
        dayPanel.setVisible(true);
        weekPanel.setVisible(false);
        hoursContainer.setVisible(true);
        dayView.update(datePicker.getValue());
    }

    private void showWeekView() {
        isWeekView = true;
        dayPanel.setVisible(false);
        weekPanel.setVisible(true);
        hoursContainer.setVisible(false);
        weekView.update(datePicker.getValue());
    }

    // ==================== ДИАЛОГИ ====================

    private void openRemindersDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reminders-dialog.fxml"));
            loader.setResources(I18n.bundle());
            DialogPane pane = loader.load();
            RemindersDialogController ctrl = loader.getController();
            ctrl.init(reminderService);
            ThemeManager.applyTo(pane);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle(I18n.get("dialog.reminders.title"));
            dialog.setDialogPane(pane);
            AppIcons.applyTo(dialog);
            dialog.showAndWait();
        } catch (IOException e) {
            Dialogs.error(I18n.get("common.error"),
                    I18n.format("dialog.reminders.error", Dialogs.reason(e)));
        }
    }

    private void openExportDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/export-dialog.fxml"));
            loader.setResources(I18n.bundle());
            DialogPane pane = loader.load();
            ExportDialogController ctrl = loader.getController();
            ctrl.init(taskService, exportService, datePicker.getValue(), isWeekView, datePicker.getScene().getWindow(), Dialogs::accept);
            ThemeManager.applyTo(pane);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle(I18n.get("dialog.export.title"));
            dialog.setDialogPane(pane);
            AppIcons.applyTo(dialog);
            dialog.showAndWait();
        } catch (IOException e) {
            Dialogs.error(I18n.get("common.error"),
                    I18n.format("dialog.export.error", Dialogs.reason(e)));
        }
    }

    private void openHolidaysDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/holidays-dialog.fxml"));
            loader.setResources(I18n.bundle());
            DialogPane pane = loader.load();
            HolidaysDialogController ctrl = loader.getController();
            ctrl.init(workTimeService, Dialogs::accept);
            ThemeManager.applyTo(pane);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle(I18n.get("dialog.holidays.title"));
            dialog.setDialogPane(pane);
            AppIcons.applyTo(dialog);
            dialog.showAndWait();

            if (isWeekView) {
                weekView.update(datePicker.getValue());
            }
        } catch (IOException e) {
            Dialogs.error(I18n.get("common.error"),
                    I18n.format("dialog.holidays.error", Dialogs.reason(e)));
        }
    }

    /** Показ диалога напоминания и звук вынесены в {@link ReminderUi}. */

    private void openSettingsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings-dialog.fxml"));
            loader.setResources(I18n.bundle());
            DialogPane pane = loader.load();
            SettingsDialogController ctrl = loader.getController();
            ctrl.init(datePicker.getScene(), form.typeItems(), this::importFromSettings,
                    this::openSuggestionManager, this::suggestionsSettingsChanged);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle(I18n.get("dialog.settings.title"));
            dialog.setDialogPane(pane);
            AppIcons.applyTo(dialog);
            dialog.showAndWait();

            if (updateCheckUi != null) {
                updateCheckUi.applySchedule(AppSettings.updateCheckEnabled(),
                        AppSettings.updateCheckHour(), AppSettings.updateCheckMinute());
            }
            applyImportSchedule();
        } catch (IOException e) {
            Dialogs.error(I18n.get("common.error"),
                    I18n.format("dialog.settings.error", Dialogs.reason(e)));
        }
    }

    // ==================== ПРОЧЕЕ ====================

    private void loadTasksFromYaml() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.get("yaml.chooseTitle"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("yaml.filter"), "*.yaml", "*.yml"));

        File file = fileChooser.showOpenDialog(datePicker.getScene().getWindow());
        if (file == null) return;

        if (!FileValidation.isReadableYaml(file.toPath())) {
            Dialogs.error(I18n.get("common.error"), I18n.get("yaml.invalidFile"));
            return;
        }

        ImportOutcome outcome = runImport(file.toPath(), true, false);
        if (outcome != null) {
            showImportResult(outcome);
        }
    }

    private void showImportResult(ImportOutcome outcome) {
        if (outcome.hasNew()) {
            Dialogs.info(I18n.get("common.success"), I18n.format("tasks.import.result",
                    outcome.added().projects(), outcome.added().taskNames(), outcome.added().types()));
        } else {
            Dialogs.info(I18n.get("common.success"), I18n.get("tasks.import.nothingNew"));
        }
    }

    /** Импорт из окна настроек (кнопки «Создать пример…»/«Выбрать…», ответ «Да»). */
    private void importFromSettings(Path file) {
        ImportOutcome outcome = runImport(file, true, false);
        if (outcome != null) {
            showImportResult(outcome);
        }
    }

    private void downloadYamlTemplate() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.get("yaml.saveTitle"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("yaml.filter"), "*.yaml", "*.yml"));
        fileChooser.setInitialFileName("tasks-template.yaml");

        File file = fileChooser.showSaveDialog(datePicker.getScene().getWindow());
        if (file == null) return;

        try {
            Files.writeString(file.toPath(), YamlTemplate.content(), StandardCharsets.UTF_8);
            Dialogs.info(I18n.get("common.success"), I18n.format("yaml.saved", file.getName()));
        } catch (Exception e) {
            Dialogs.error(I18n.get("common.error"), I18n.format("yaml.saveError", e.getMessage()));
        }
    }

    // ==================== ОБНОВЛЕНИЯ ====================

    private void setupUpdateCheck() {
        updateCheckUi = new UpdateCheckUi(updateCheckService);
        updateCheckUi.setExitAction(() -> exitAction.run());
        updateCheckUi.setUpdateIndicator(this::applyUpdateIndicator);
        updateButton.setOnAction(e -> updateCheckUi.manualCheck());
        if (AppSettings.updateCheckEnabled()) {
            updateCheckUi.checkForUpdates(UpdateCheckUi.CheckMode.STARTUP);
            updateCheckUi.applySchedule(true, AppSettings.updateCheckHour(), AppSettings.updateCheckMinute());
        }
    }

    /** Меняет текст кнопки проверки обновлений при наличии/отсутствии обновления. */
    private void applyUpdateIndicator(UpdateInfo update) {
        if (update != null && update.newer()) {
            updateButton.setText(I18n.get("main.updateAvailable"));
        } else {
            updateButton.setText(I18n.get("main.checkUpdates"));
        }
    }

    // ==================== ИМПОРТ КАТЕГОРИЙ ====================

    private void setupTaskImport() {
        catalogImportService = new CatalogImportService(
                new YamlCatalogImporter(), taskService.getCatalog(), importState);
        importNotifier = new ImportNotifier(importBell,
                (caption, text) -> trayNotifier.accept(caption, text));
        applyImportSchedule();
        javafx.application.Platform.runLater(this::runStartupImportIfEnabled);
    }

    private void runStartupImportIfEnabled() {
        if (!AppSettings.taskImportOnStartup()) {
            return;
        }
        Path file = configuredImportFile();
        if (file != null) {
            runImport(file, false, true);
        }
    }

    private void applyImportSchedule() {
        boolean enabled = AppSettings.taskImportScheduleEnabled() && configuredImportFile() != null;
        importScheduler.applySchedule(enabled, AppSettings.taskImportHour(),
                AppSettings.taskImportMinute(), this::scheduledImport);
    }

    private void scheduledImport() {
        javafx.application.Platform.runLater(() -> {
            Path file = configuredImportFile();
            if (file != null) {
                runImport(file, false, true);
            }
        });
    }

    private Path configuredImportFile() {
        String path = AppSettings.taskImportFile();
        if (path == null || path.isBlank()) {
            return null;
        }
        Path file;
        try {
            file = Path.of(path);
        } catch (RuntimeException e) {
            Log.log("TaskImport", "invalid configured path: " + path, e);
            return null;
        }
        if (!FileValidation.isReadableYaml(file)) {
            Log.log("TaskImport", "configured file is not a readable YAML: " + path);
            return null;
        }
        return file;
    }

    /** Импортирует файл; возвращает итог или {@code null} при ошибке. */
    private ImportOutcome runImport(Path file, boolean force, boolean notify) {
        try {
            ImportOutcome outcome = catalogImportService.importFile(file, force);
            if (outcome.unchangedFile()) {
                Log.log("TaskImport", "unchanged: " + file);
                return outcome;
            }
            Log.log("TaskImport", "imported " + file + ": projects+" + outcome.added().projects()
                    + ", tasks+" + outcome.added().taskNames() + ", types+" + outcome.added().types()
                    + " (rows=" + outcome.totalRows() + ", skipped=" + outcome.skipped() + ")");
            if (outcome.hasNew()) {
                suggestionService.reloadCatalog();
                form.refreshSuggestionsOnly();
                form.refreshTypeChoices(taskService);
                if (notify) {
                    importNotifier.notifyImported(LocalDateTime.now(), outcome,
                            AppSettings.taskImportTrayNotify());
                }
            }
            return outcome;
        } catch (Exception e) {
            Log.log("TaskImport", "failed: " + e.getMessage(), e);
            if (force) {
                Dialogs.error(I18n.get("common.error"),
                        I18n.format("tasks.import.error", Dialogs.reason(e)));
            }
            return null;
        }
    }

    /** Задаётся приложением: показ сообщения в трее. */
    public void setTrayNotifier(BiConsumer<String, String> trayNotifier) {
        if (trayNotifier != null) {
            this.trayNotifier = trayNotifier;
        }
    }

    /** Задаётся приложением: корректное завершение без подтверждения (для автообновления). */
    public void setExitAction(Runnable exitAction) {
        if (exitAction != null) {
            this.exitAction = exitAction;
        }
    }

    public void shutdown() {
        reminderService.shutdown();
        if (updateCheckUi != null) {
            updateCheckUi.shutdown();
        }
        importScheduler.shutdown();
        if (suggestionIndexing != null) {
            suggestionIndexing.shutdown();
        }
        if (suggestionService != null) {
            suggestionService.shutdown();
        }
        if (indexingPoller != null) {
            indexingPoller.stop();
            indexingPoller = null;
        }
    }

    /** Вызывается приложением после установки сцены: подключает фоновую индексацию. */
    public void attachStage(Stage stage) {
        if (stage == null || suggestionService == null || suggestionIndexing != null) {
            return;
        }
        suggestionIndexing = new SuggestionIndexingCoordinator(stage, suggestionService);
        suggestionIndexing.start();
        startIndexingStatusPolling();
    }

    private void startIndexingStatusPolling() {
        if (indexingPoller != null) {
            return;
        }
        indexingPoller = new Timeline(new KeyFrame(Duration.millis(200), event -> updateIndexingStatus()));
        indexingPoller.setCycleCount(Animation.INDEFINITE);
        indexingPoller.play();
    }

    private void updateIndexingStatus() {
        if (indexingStatus == null || suggestionService == null) {
            return;
        }
        if (!suggestionService.isIndexing()) {
            indexingStatus.setVisible(false);
            indexingStatus.setManaged(false);
            indexingProgress.setVisible(false);
            indexingProgress.setManaged(false);
            return;
        }
        long elapsed = System.currentTimeMillis() - suggestionService.indexingStartedAtMillis();
        if (elapsed < 400) {
            indexingStatus.setVisible(false);
            indexingStatus.setManaged(false);
            return;
        }
        indexingStatus.setVisible(true);
        indexingStatus.setManaged(true);
        boolean showProgress = elapsed >= 2000;
        indexingProgress.setVisible(showProgress);
        indexingProgress.setManaged(showProgress);
        if (showProgress) {
            indexingProgress.setProgress(suggestionService.progress());
        }
    }

    private void onSuggestionDataChanged() {
        if (suggestionIndexing != null) {
            suggestionIndexing.onDataChanged();
        }
    }

    private void openSuggestionManager(Window owner) {
        SuggestionManagerDialog.show(owner, taskService, suggestionService,
                this::catalogChanged,
                () -> {
                    if (suggestionIndexing != null) {
                        suggestionIndexing.refreshNow();
                    }
                });
    }

    private void catalogChanged() {
        suggestionService.reloadCatalog();
        form.refreshSuggestionsOnly();
        form.refreshTypeChoices(taskService);
    }

    private void suggestionsSettingsChanged() {
        if (suggestionIndexing != null) {
            suggestionIndexing.onSettingsChanged();
        }
        form.refreshSuggestionsOnly();
    }

    @FXML
    private void openGithubIssues() {
        try {
            String url = "https://github.com/Zalmat/TaskReminder/issues";
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            System.err.println("Failed to open GitHub issues: " + e.getMessage());
        }
    }

    // Шутка (обработчик вынесен в EasterEgg)
    @FXML
    private void handleLabelClick() {
        easterEgg.onLabelClick();
    }
}