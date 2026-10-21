package com.reminder.ui.controller;

import com.reminder.i18n.I18n;
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
import com.reminder.storage.StorageException;
import com.reminder.suggest.SuggestionService;
import com.reminder.ui.suggest.SuggestionUi;
import com.reminder.ui.UpdateCheckUi;
import com.reminder.ui.component.ClockUi;
import com.reminder.ui.component.EasterEgg;
import com.reminder.ui.component.EntryForm;
import com.reminder.ui.dialog.ExportDialog;
import com.reminder.ui.dialog.HolidaysDialog;
import com.reminder.ui.dialog.RemindersDialog;
import com.reminder.ui.dialog.SettingsDialog;
import com.reminder.ui.importing.TaskImportCoordinator;
import com.reminder.ui.util.DatePickerUtils;
import com.reminder.ui.util.Dialogs;
import com.reminder.ui.view.DayView;
import com.reminder.ui.view.ReminderUi;
import com.reminder.ui.view.ViewMode;
import com.reminder.ui.view.WeekView;
import com.reminder.util.Log;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
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
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDate;
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
    private SuggestionService suggestionService;
    private SuggestionUi suggestionUi;
    private TaskImportCoordinator taskImport;
    private BiConsumer<String, String> trayNotifier = (caption, text) -> { };
    private UpdateCheckUi updateCheckUi;
    private Runnable exitAction = () -> { };
    private final EasterEgg easterEgg = new EasterEgg();
    private EntryForm form;
    private final ReminderUi reminderUi = new ReminderUi(reminderService);
    private WeekView weekView;
    private DayView dayView;
    private ViewMode viewMode = ViewMode.DAY;

    @FXML
    private void initialize() {
        suggestionService = new SuggestionService(taskService);
        form = new EntryForm(projectChoice, taskChoice, typeChoice, hoursField, commentArea,
                suggestionService, this::onSuggestionDataChanged);
        try {
            taskService.ensureDefaultTypes(WorkTypes.defaults());
        } catch (StorageException e) {
            // Типы по умолчанию — вторичный кэш; не мешаем запуску приложения.
            Log.log("Main", "cannot save default work types: " + e.getMessage(), e);
        }
        form.loadTypeChoices(taskService);
        suggestionUi = new SuggestionUi(indexingStatus, indexingProgress,
                suggestionService, taskService, form);

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
                if (viewMode == ViewMode.WEEK) {
                    weekView.update(newVal);
                } else {
                    dayView.update(newVal);
                }
            }
        });
    }

    private void wireButtons() {
        prevButton.setOnAction(e -> {
            if (viewMode == ViewMode.WEEK) {
                datePicker.setValue(datePicker.getValue().minusWeeks(1));
            } else {
                datePicker.setValue(datePicker.getValue().minusDays(1));
            }
        });
        nextButton.setOnAction(e -> {
            if (viewMode == ViewMode.WEEK) {
                datePicker.setValue(datePicker.getValue().plusWeeks(1));
            } else {
                datePicker.setValue(datePicker.getValue().plusDays(1));
            }
        });
        todayButton.setOnAction(e -> datePicker.setValue(LocalDate.now()));

        addButton.setOnAction(e -> {
            if (viewMode == ViewMode.WEEK) {
                weekView.add();
            } else {
                dayView.add();
            }
        });
        loadYamlButton.setOnAction(e -> taskImport.importFromChooser(
                datePicker.getScene().getWindow()));
        templateButton.setOnAction(e -> taskImport.downloadTemplate(
                datePicker.getScene().getWindow()));
        clearButton.setOnAction(e -> {
            if (viewMode == ViewMode.WEEK) {
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
        switchMode(ViewMode.DAY);
    }

    private void showWeekView() {
        switchMode(ViewMode.WEEK);
    }

    private void switchMode(ViewMode mode) {
        viewMode = mode;
        boolean week = mode == ViewMode.WEEK;
        dayPanel.setVisible(!week);
        weekPanel.setVisible(week);
        hoursContainer.setVisible(!week);
        if (week) {
            weekView.update(datePicker.getValue());
        } else {
            dayView.update(datePicker.getValue());
        }
    }

    // ==================== ДИАЛОГИ ====================

    private void openRemindersDialog() {
        RemindersDialog.show(reminderService);
    }

    private void openExportDialog() {
        ExportDialog.show(taskService, exportService, datePicker.getValue(),
                viewMode == ViewMode.WEEK, datePicker.getScene().getWindow(), Dialogs::accept);
    }

    private void openHolidaysDialog() {
        HolidaysDialog.show(workTimeService, () -> {
            if (viewMode == ViewMode.WEEK) {
                weekView.update(datePicker.getValue());
            }
        });
    }

    /** Показ диалога напоминания и звук вынесены в {@link ReminderUi}. */

    private void openSettingsDialog() {
        SettingsDialog.show(datePicker.getScene(), form.typeItems(), taskImport::importFromSettings,
                suggestionUi::openManager, suggestionUi::settingsChanged, () -> {
                    if (updateCheckUi != null) {
                        updateCheckUi.applySchedule(AppSettings.updateCheckEnabled(),
                                AppSettings.updateCheckHour(), AppSettings.updateCheckMinute());
                    }
                    taskImport.applySchedule();
                });
    }

    // ==================== ПРОЧЕЕ ====================

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
        taskImport = new TaskImportCoordinator(taskService, suggestionService, form, importBell,
                (caption, text) -> trayNotifier.accept(caption, text));
        taskImport.start();
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
        if (taskImport != null) {
            taskImport.shutdown();
        }
        if (suggestionUi != null) {
            suggestionUi.shutdown();
        }
        if (suggestionService != null) {
            suggestionService.shutdown();
        }
    }

    /** Вызывается приложением после установки сцены: подключает фоновую индексацию. */
    public void attachStage(Stage stage) {
        if (suggestionUi != null) {
            suggestionUi.attachStage(stage);
        }
    }

    private void onSuggestionDataChanged() {
        if (suggestionUi != null) {
            suggestionUi.onDataChanged();
        }
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