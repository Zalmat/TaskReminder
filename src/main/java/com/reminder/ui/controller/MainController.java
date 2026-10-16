package com.reminder.ui.controller;

import com.reminder.model.Task;
import com.reminder.model.WeekEntry;
import com.reminder.model.WorkEntry;
import com.reminder.service.ExportService;
import com.reminder.service.ReminderService;
import com.reminder.service.TaskService;
import com.reminder.service.UpdateCheckService;
import com.reminder.service.WorkTimeService;
import com.reminder.service.YamlLoaderService;
import com.reminder.ui.UpdateCheckUi;
import com.reminder.ui.component.ClockUi;
import com.reminder.ui.component.EasterEgg;
import com.reminder.ui.component.EntryForm;
import com.reminder.ui.theme.ThemeManager;
import com.reminder.ui.util.DatePickerUtils;
import com.reminder.ui.util.Dialogs;
import com.reminder.ui.view.DayView;
import com.reminder.ui.view.ReminderUi;
import com.reminder.ui.view.WeekView;
import com.reminder.util.YamlTemplate;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

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
    @FXML private MenuButton menuButton;
    @FXML private MenuItem settingsItem;

    private final TaskService taskService = new TaskService();
    private final WorkTimeService workTimeService = new WorkTimeService();
    private final ReminderService reminderService = new ReminderService();
    private final ExportService exportService = new ExportService();
    private final YamlLoaderService yamlLoaderService = new YamlLoaderService();
    private final UpdateCheckService updateCheckService = new UpdateCheckService();
    private UpdateCheckUi updateCheckUi;
    private final EasterEgg easterEgg = new EasterEgg();
    private EntryForm form;
    private final ReminderUi reminderUi = new ReminderUi(reminderService);
    private WeekView weekView;
    private DayView dayView;
    private boolean isWeekView = false;

    @FXML
    private void initialize() {
        form = new EntryForm(projectChoice, taskChoice, typeChoice, hoursField, commentArea);
        form.loadTypeChoices();

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
        form.refreshChoices(taskService);

        reminderUi.setup();
        setupUpdateCheck();
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
            DialogPane pane = loader.load();
            RemindersDialogController ctrl = loader.getController();
            ctrl.init(reminderService);
            ThemeManager.applyTo(pane);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("⏰ Управление напоминаниями");
            dialog.setDialogPane(pane);
            dialog.showAndWait();
        } catch (IOException e) {
            Dialogs.error("Ошибка", "❌ Не удалось открыть окно напоминаний: " + Dialogs.reason(e));
        }
    }

    private void openExportDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/export-dialog.fxml"));
            DialogPane pane = loader.load();
            ExportDialogController ctrl = loader.getController();
            ctrl.init(taskService, exportService, datePicker.getValue(), isWeekView, datePicker.getScene().getWindow(), Dialogs::accept);
            ThemeManager.applyTo(pane);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("📊 Экспорт данных");
            dialog.setDialogPane(pane);
            dialog.showAndWait();
        } catch (IOException e) {
            Dialogs.error("Ошибка", "❌ Не удалось открыть окно экспорта: " + Dialogs.reason(e));
        }
    }

    private void openHolidaysDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/holidays-dialog.fxml"));
            DialogPane pane = loader.load();
            HolidaysDialogController ctrl = loader.getController();
            ctrl.init(workTimeService, Dialogs::accept);
            ThemeManager.applyTo(pane);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("📅 Управление праздничными днями");
            dialog.setDialogPane(pane);
            dialog.showAndWait();

            if (isWeekView) {
                weekView.update(datePicker.getValue());
            }
        } catch (IOException e) {
            Dialogs.error("Ошибка", "❌ Не удалось открыть окно праздников: " + Dialogs.reason(e));
        }
    }

    /** Показ диалога напоминания и звук вынесены в {@link ReminderUi}. */

    private void openSettingsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings-dialog.fxml"));
            DialogPane pane = loader.load();
            SettingsDialogController ctrl = loader.getController();
            ctrl.init(datePicker.getScene(), form.typeItems());

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("⚙ Настройки");
            dialog.setDialogPane(pane);
            dialog.showAndWait();
        } catch (IOException e) {
            Dialogs.error("Ошибка", "❌ Не удалось открыть окно настроек: " + Dialogs.reason(e));
        }
    }

    // ==================== ПРОЧЕЕ ====================

    private void loadTasksFromYaml() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите YAML файл с задачами");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("YAML файлы", "*.yaml", "*.yml"));

        File file = fileChooser.showOpenDialog(datePicker.getScene().getWindow());
        if (file == null) return;

        try {
            List<Task> tasks = yamlLoaderService.loadTasksFromYaml(file.getAbsolutePath());
            if (tasks.isEmpty()) {
                Dialogs.warn("Внимание", "⚠️ Файл не содержит задач");
                return;
            }
            for (Task task : tasks) {
                taskService.addPredefinedTask(task);
            }
            form.refreshChoices(taskService);
            Dialogs.info("Успех", String.format("✅ Загружено %d задач!", tasks.size()));
        } catch (Exception e) {
            Dialogs.error("Ошибка", "❌ Не удалось загрузить файл: " + e.getMessage());
        }
    }

    private void downloadYamlTemplate() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить шаблон YAML");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("YAML файлы", "*.yaml", "*.yml"));
        fileChooser.setInitialFileName("tasks-template.yaml");

        File file = fileChooser.showSaveDialog(datePicker.getScene().getWindow());
        if (file == null) return;

        try {
            Files.writeString(file.toPath(), YamlTemplate.content(), StandardCharsets.UTF_8);
            Dialogs.info("Успех", "✅ Шаблон сохранен: " + file.getName());
        } catch (Exception e) {
            Dialogs.error("Ошибка", "❌ Не удалось сохранить шаблон: " + e.getMessage());
        }
    }

    // ==================== ОБНОВЛЕНИЯ ====================

    private void setupUpdateCheck() {
        updateCheckUi = new UpdateCheckUi(updateCheckService);
        updateButton.setOnAction(e -> updateCheckUi.checkForUpdates(true));
        updateCheckUi.checkForUpdates(false);
    }

    public void shutdown() {
        reminderService.shutdown();
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