package com.reminder.ui.controller;

import com.reminder.model.Reminder;
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
import com.reminder.ui.component.EasterEgg;
import com.reminder.ui.component.WeekEntryCell;
import com.reminder.ui.component.WeekTotalCell;
import com.reminder.ui.util.DatePickerUtils;
import com.reminder.ui.util.Dialogs;
import com.reminder.util.DateUtils;
import com.reminder.util.RussianDays;
import com.reminder.util.YamlTemplate;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    private final TaskService taskService = new TaskService();
    private final WorkTimeService workTimeService = new WorkTimeService();
    private final ReminderService reminderService = new ReminderService();
    private final ExportService exportService = new ExportService();
    private final YamlLoaderService yamlLoaderService = new YamlLoaderService();
    private final UpdateCheckService updateCheckService = new UpdateCheckService();
    private UpdateCheckUi updateCheckUi;
    private final EasterEgg easterEgg = new EasterEgg();

    private final ObservableList<WorkEntry> dayEntries = FXCollections.observableArrayList();
    private final ObservableList<WeekEntry> weekEntries = FXCollections.observableArrayList();

    private final List<LocalDate> weekDays = new ArrayList<>();
    private final List<WeekDayColumn> weekDayColumns = new ArrayList<>();
    private boolean isWeekView = false;
    private boolean weekCellEditInProgress = false;

    @FXML
    private void initialize() {
        configureDatePicker();
        configureColumns();
        createWeekDayColumns();
        wireButtons();
        wireViewToggle();
        loadTypeChoices();
        refreshChoices();

        setupReminders();
        setupUpdateCheck();
        startClock();

        updateDayView(datePicker.getValue());
        reminderService.startMonitoring();
    }

    // ==================== НАСТРОЙКА UI ====================

    private void configureDatePicker() {
        DatePickerUtils.applyTo(datePicker);
        datePicker.setValue(LocalDate.now());
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (isWeekView) {
                    updateWeekView(newVal);
                } else {
                    updateDayView(newVal);
                }
            }
        });
    }

    private void configureColumns() {
        dayTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        dayTableView.setItems(dayEntries);

        dayProjectColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getProject()));
        dayProjectColumn.setCellFactory(c -> wrappingCell());
        dayTaskColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getTaskName()));
        dayTaskColumn.setCellFactory(c -> wrappingCell());
        dayTypeColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getType()));
        dayTypeColumn.setCellFactory(c -> wrappingCell());
        dayHoursColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>((int) cd.getValue().getHours()));
        dayHoursColumn.setStyle("-fx-alignment: CENTER;");
        dayCommentColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getComment()));
        dayCommentColumn.setCellFactory(c -> wrappingCell());

        ContextMenu dayMenu = new ContextMenu();
        MenuItem dayEditItem = new MenuItem("✏️ Редактировать");
        dayEditItem.setOnAction(e -> editDayEntry());
        MenuItem dayDeleteItem = new MenuItem("🗑️ Удалить");
        dayDeleteItem.setOnAction(e -> deleteDayEntry());
        dayMenu.getItems().addAll(dayEditItem, dayDeleteItem);
        dayTableView.setContextMenu(dayMenu);

        weekTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        weekTableView.setItems(weekEntries);

        weekProjectColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getProject()));
        weekProjectColumn.setCellFactory(c -> wrappingCell());
        weekTaskColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getTaskName()));
        weekTaskColumn.setCellFactory(c -> wrappingCell());
        weekTypeColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getType()));
        weekTypeColumn.setCellFactory(c -> wrappingCell());
        weekCommentColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getComment()));
        weekCommentColumn.setCellFactory(c -> wrappingCell());
        weekTotalColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getTotal()));
        weekTotalColumn.setCellFactory(c -> new WeekTotalCell());

        ContextMenu weekMenu = new ContextMenu();
        MenuItem weekEditItem = new MenuItem("✏️ Редактировать задачу");
        weekEditItem.setOnAction(e -> editWeekEntry());
        MenuItem weekDeleteItem = new MenuItem("🗑️ Удалить задачу");
        weekDeleteItem.setOnAction(e -> deleteWeekEntry());
        weekMenu.getItems().addAll(weekEditItem, weekDeleteItem);
        weekTableView.setContextMenu(weekMenu);
    }

    private <S> TableCell<S, String> wrappingCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setWrapText(false);
                } else {
                    setText(item);
                    setWrapText(true);
                }
            }
        };
    }

    /** Колонка дня недели, помнит свою дату для переиспользования при навигации. */
    private static final class WeekDayColumn extends TableColumn<WeekEntry, Integer> {
        private LocalDate date;
        private Tooltip dayTooltip;
        private final Label headerLabel = new Label();

        WeekDayColumn(LocalDate date) {
            this.date = date;
            setGraphic(headerLabel);
        }

        LocalDate date() {
            return date;
        }

        void setDay(LocalDate d) {
            this.date = d;
        }

        void setHeaderText(String text) {
            headerLabel.setText(text);
        }

        void setHeaderStyle(String css) {
            headerLabel.setStyle(css);
        }

        /** Показывает/скрывает тултип о превышении лимита на заголовке дня. */
        void setOvertimeTooltip(boolean show) {
            if (show && dayTooltip == null) {
                dayTooltip = new Tooltip("Осторожно. Превышено количество рабочих часов");
                Tooltip.install(headerLabel, dayTooltip);
            } else if (!show) {
                if (dayTooltip != null) {
                    Tooltip.uninstall(headerLabel, dayTooltip);
                    dayTooltip = null;
                }
            }
        }
    }

    private void createWeekDayColumns() {
        LocalDate start = DateUtils.startOfWeek(datePicker.getValue());
        for (int i = 0; i < 7; i++) {
            WeekDayColumn col = new WeekDayColumn(start.plusDays(i));
            col.setCellValueFactory(cd ->
                    new ReadOnlyObjectWrapper<>(cd.getValue().getDayHours().getOrDefault(col.date(), 0)));
            col.setPrefWidth(70);
            col.setStyle("-fx-alignment: CENTER;");
            col.setEditable(true);
            col.setCellFactory(cf -> new WeekEntryCell());
            col.setOnEditCommit(this::handleWeekCellEdit);
            weekDayColumns.add(col);
        }
        weekTableView.getColumns().addAll(3, weekDayColumns);
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
                addWeekEntry();
            } else {
                addDayEntry();
            }
        });
        loadYamlButton.setOnAction(e -> loadTasksFromYaml());
        templateButton.setOnAction(e -> downloadYamlTemplate());
        clearButton.setOnAction(e -> {
            if (isWeekView) {
                clearWeekEntries();
            } else {
                clearDayEntries();
            }
        });
        holidayButton.setOnAction(e -> openHolidaysDialog());
        reminderButton.setOnAction(e -> openRemindersDialog());
        exportButton.setOnAction(e -> openExportDialog());
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

    private void loadTypeChoices() {
        typeChoice.getItems().addAll(
                "Разработка", "Тестирование", "Дизайн", "Аналитика",
                "Управление", "Коммуникация", "Другое");
        typeChoice.setValue("Разработка");
    }

    private void startClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                clockLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    // ==================== РЕЖИМЫ ПРОСМОТРА ====================

    private void showDayView() {
        isWeekView = false;
        dayPanel.setVisible(true);
        weekPanel.setVisible(false);
        hoursContainer.setVisible(true);
        updateDayView(datePicker.getValue());
    }

    private void showWeekView() {
        isWeekView = true;
        dayPanel.setVisible(false);
        weekPanel.setVisible(true);
        hoursContainer.setVisible(false);
        updateWeekView(datePicker.getValue());
    }

    private void updateDayView(LocalDate date) {
        List<WorkEntry> entries = taskService.getEntriesForDate(date);
        dayEntries.setAll(entries);

        double total = workTimeService.getDayTotal(entries, date);
        double remaining = workTimeService.getRemainingForDay(entries, date);

        String bold = "-fx-font-weight: bold; -fx-font-size: 14px;";
        dayTotalLabel.setText(String.format("Загружено: %.1f ч", total));
        if (remaining < 0) {
            dayRemainingLabel.setText("Осталось: 0,0 ч");
            dayRemainingLabel.setStyle(bold + "-fx-text-fill: orange;");
            dayOvertimeLabel.setText(String.format("Переработка: %.1f ч", -remaining));
            dayOvertimeLabel.setStyle(bold + "-fx-text-fill: red;");
        } else {
            dayRemainingLabel.setText(String.format("Осталось: %.1f ч", remaining));
            dayRemainingLabel.setStyle(bold + "-fx-text-fill: " + (remaining < 1 ? "orange" : "green") + ";");
            dayOvertimeLabel.setText("");
            dayOvertimeLabel.setStyle("");
        }
    }

    private void updateWeekView(LocalDate date) {
        LocalDate startOfWeek = DateUtils.startOfWeek(date);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        weekDays.clear();
        for (int i = 0; i < 7; i++) {
            weekDays.add(startOfWeek.plusDays(i));
        }

        for (int i = 0; i < 7; i++) {
            WeekDayColumn col = weekDayColumns.get(i);
            LocalDate day = weekDays.get(i);
            col.setDay(day);
            col.setHeaderText(RussianDays.shortName(day.getDayOfWeek().getValue()) + " " + DateUtils.formatShort(day));
            col.setEditable(!workTimeService.isHoliday(day));
        }

        List<WorkEntry> entries = taskService.getEntriesForDateRange(startOfWeek, endOfWeek);
        weekEntries.setAll(workTimeService.aggregateWeek(entries));
        highlightOvertimeDays();
        updateWeekTotals();
        weekTableView.refresh();
    }

    private void updateWeekTotals() {
        double weeklyTotal = workTimeService.getWeekTotal(weekEntries);
        String text = String.format("Неделя: %.1f / %.0f ч", weeklyTotal, WorkTimeService.MAX_WEEKLY_HOURS);
        if (weeklyTotal > WorkTimeService.MAX_WEEKLY_HOURS) {
            weekTotalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: red;");
        } else {
            weekTotalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2196F3;");
        }
        weekTotalLabel.setText(text);
    }

    /** Подсвечивает красным заголовок дня, если сумма часов за день превышает лимит. */
    private void highlightOvertimeDays() {
        for (WeekDayColumn col : weekDayColumns) {
            LocalDate day = col.date();
            int dayTotal = weekEntries.stream()
                    .mapToInt(we -> we.getDayHours().getOrDefault(day, 0))
                    .sum();
            if (dayTotal > WorkTimeService.MAX_DAILY_HOURS) {
                col.setStyle("-fx-alignment: CENTER;");
                col.setHeaderStyle("-fx-font-weight: bold; -fx-text-fill: red;");
                col.setOvertimeTooltip(true);
            } else if (workTimeService.isHoliday(day)) {
                col.setStyle("-fx-alignment: CENTER; -fx-background-color: #f5f5f5;");
                col.setHeaderStyle("-fx-text-fill: #999;");
                col.setOvertimeTooltip(false);
            } else {
                col.setStyle("-fx-alignment: CENTER;");
                col.setHeaderStyle("");
                col.setOvertimeTooltip(false);
            }
        }
    }

    // ==================== ДНЕВНОЙ РЕЖИМ ====================

    private void addDayEntry() {
        try {
            String project = projectChoice.getEditor().getText().trim();
            String taskName = com.reminder.util.TaskValidationUtils.getSafeTaskName(taskChoice.getEditor().getText());
            String type = typeChoice.getValue();
            String hoursText = hoursField.getText().trim();
            String comment = commentArea.getText().trim();

            if (project.isEmpty()) {
                Dialogs.info("Ошибка", "⚠️ Пожалуйста, укажите проект");
                projectChoice.getEditor().requestFocus();
                return;
            }
            if (taskName.isEmpty()) {
                Dialogs.info("Ошибка", "⚠️ Пожалуйста, укажите задачу");
                taskChoice.getEditor().requestFocus();
                return;
            }
            if (type == null || type.isEmpty()) {
                Dialogs.info("Ошибка", "⚠️ Пожалуйста, выберите тип работы");
                typeChoice.requestFocus();
                return;
            }
            if (hoursText.isEmpty()) {
                Dialogs.info("Ошибка", "⚠️ Пожалуйста, укажите длительность");
                hoursField.requestFocus();
                return;
            }

            int hours;
            try {
                hours = Integer.parseInt(hoursText);
            } catch (NumberFormatException e) {
                Dialogs.info("Ошибка", "⚠️ Длительность должна быть целым числом");
                hoursField.requestFocus();
                return;
            }
            if (hours <= 0 || hours > 24) {
                Dialogs.info("Ошибка", "⚠️ Длительность должна быть от 1 до 24 часов");
                hoursField.requestFocus();
                return;
            }

            LocalDate date = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();

            boolean allowOvertime = false;
            double currentTotal = taskService.getTotalHoursForDate(date);
            if (workTimeService.wouldExceedDayLimit(taskService.getEntriesForDate(date), date, hours)) {
                int overtime = (int) (currentTotal + hours) - (int) WorkTimeService.MAX_DAILY_HOURS;
                allowOvertime = Dialogs.yesNo("Переработка",
                        String.format("Превышен лимит %.0f ч", WorkTimeService.MAX_DAILY_HOURS),
                        String.format("Переработка %d ч. Зарегистрировать?", overtime));
                if (!allowOvertime) {
                    return;
                }
            }

            WorkEntry entry = new WorkEntry(project, taskName, type, hours);
            entry.setDate(date);
            entry.setComment(comment);

            if (taskService.addWorkEntry(entry, allowOvertime)) {
                taskService.addPredefinedTask(new Task(project, taskName, type));
                refreshChoices();
                clearInputFields();
                updateDayView(date);
                Dialogs.info("Успех", "✅ Работа добавлена!");
            }
        } catch (Exception e) {
            Dialogs.info("Ошибка", "❌ Произошла ошибка: " + e.getMessage());
        }
    }

    private void editDayEntry() {
        WorkEntry selected = dayTableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        com.reminder.ui.dialog.EntryEditDialog.EntryEditResult result =
                com.reminder.ui.dialog.EntryEditDialog.show(
                        "✏️ Редактировать запись", "Измените данные записи",
                        typeChoice.getItems(),
                        selected.getProject(), selected.getTaskName(), selected.getType(),
                        String.valueOf((int) selected.getHours()), selected.getComment(), true);
        if (result == null) return;

        int hours;
        try {
            hours = Integer.parseInt(result.hours());
        } catch (NumberFormatException e) {
            Dialogs.info("Ошибка", "⚠️ Длительность должна быть целым числом");
            return;
        }
        if (hours <= 0 || hours > 24) {
            Dialogs.info("Ошибка", "⚠️ Длительность должна быть от 1 до 24 часов");
            return;
        }

        WorkEntry updated = new WorkEntry(result.project(), result.taskName(), result.type(), hours);
        updated.setDate(selected.getDate());
        updated.setComment(result.comment());

        boolean allowOvertime = false;
        double otherTotal = workTimeService.getDayTotal(
                taskService.getEntriesForDate(selected.getDate()), selected.getDate()) - selected.getHours();
        double currentTotal = otherTotal + selected.getHours();
        double newTotal = otherTotal + hours;
        if (newTotal > WorkTimeService.MAX_DAILY_HOURS && newTotal > currentTotal) {
            int overtime = (int) newTotal - (int) WorkTimeService.MAX_DAILY_HOURS;
            allowOvertime = Dialogs.yesNo("Переработка",
                    String.format("Превышен лимит %.0f ч", WorkTimeService.MAX_DAILY_HOURS),
                    String.format("Переработка %d ч. Зарегистрировать?", overtime));
            if (!allowOvertime) {
                return;
            }
        }

        if (taskService.updateWorkEntry(selected, updated, allowOvertime)) {
            taskService.addPredefinedTask(new Task(result.project(), result.taskName(), result.type()));
            refreshChoices();
            updateDayView(selected.getDate());
            Dialogs.info("Успех", "✅ Запись обновлена!");
        } else {
            Dialogs.info("Ошибка", "⚠️ Не удалось сохранить");
        }
    }

    private void deleteDayEntry() {
        WorkEntry selected = dayTableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (Dialogs.confirm("Подтверждение удаления", "🗑️ Удалить запись?",
                String.format("%s - %s (%.0f ч)",
                        selected.getProject(), selected.getTaskName(), selected.getHours()))) {
            taskService.removeWorkEntry(selected);
            updateDayView(selected.getDate());
        }
    }

    private void clearDayEntries() {
        LocalDate date = datePicker.getValue();
        if (date == null) return;

        if (Dialogs.confirm("Подтверждение очистки",
                "🗑️ Очистить все записи за " + DateUtils.formatDate(date) + "?",
                "Это действие нельзя отменить!")) {
            taskService.clearEntriesForDate(date);
            updateDayView(date);
        }
    }

    // ==================== НЕДЕЛЬНЫЙ РЕЖИМ ====================

    private void addWeekEntry() {
        try {
            String project = projectChoice.getEditor().getText().trim();
            String taskName = com.reminder.util.TaskValidationUtils.getSafeTaskName(taskChoice.getEditor().getText());
            String type = typeChoice.getValue();
            String comment = commentArea.getText().trim();

            if (project.isEmpty()) {
                Dialogs.info("Ошибка", "⚠️ Пожалуйста, укажите проект");
                projectChoice.getEditor().requestFocus();
                return;
            }
            if (taskName.isEmpty()) {
                Dialogs.info("Ошибка", "⚠️ Пожалуйста, укажите задачу");
                taskChoice.getEditor().requestFocus();
                return;
            }
            if (type == null || type.isEmpty()) {
                Dialogs.info("Ошибка", "⚠️ Пожалуйста, выберите тип работы");
                typeChoice.requestFocus();
                return;
            }

            for (WeekEntry entry : weekEntries) {
                if (entry.getProject().equals(project)
                        && entry.getTaskName().equals(taskName)
                        && entry.getType().equals(type)) {
                    Dialogs.info("Внимание", "⚠️ Такая задача уже существует в этой неделе");
                    return;
                }
            }

            WeekEntry newEntry = new WeekEntry(project, taskName, type);
            newEntry.setComment(comment);
            for (LocalDate day : weekDays) {
                newEntry.getDayHours().put(day, 0);
            }

            weekEntries.add(newEntry);
            taskService.addPredefinedTask(new Task(project, taskName, type));

            refreshChoices();
            clearInputFields();
            updateWeekTotals();
            weekTableView.refresh();
            Dialogs.info("Успех", "✅ Задача добавлена! Введите часы в таблице (двойной клик).");
        } catch (Exception e) {
            Dialogs.info("Ошибка", "❌ Произошла ошибка: " + e.getMessage());
        }
    }

    private void handleWeekCellEdit(TableColumn.CellEditEvent<WeekEntry, Integer> event) {
        if (weekCellEditInProgress) {
            weekTableView.refresh();
            return;
        }
        weekCellEditInProgress = true;
        try {
            processWeekCellEdit(event);
        } finally {
            weekCellEditInProgress = false;
        }
    }

    private void processWeekCellEdit(TableColumn.CellEditEvent<WeekEntry, Integer> event) {
        WeekDayColumn col = (WeekDayColumn) event.getTableColumn();
        WeekEntry entry = event.getRowValue();
        LocalDate day = col.date();
        Integer newValue = event.getNewValue();
        if (newValue == null || newValue < 0) {
            weekTableView.refresh();
            return;
        }

        int oldValue = entry.getDayHours().getOrDefault(day, 0);
        int currentDayTotal = weekEntries.stream()
                .mapToInt(we -> we.getDayHours().getOrDefault(day, 0))
                .sum();

        boolean allowOvertime = false;
        if (!workTimeService.dayTotalWithinLimit(weekEntries, day, newValue, oldValue)) {
            int busy = currentDayTotal - oldValue;
            int overtime = busy + newValue - (int) WorkTimeService.MAX_DAILY_HOURS;
            allowOvertime = Dialogs.yesNo("Переработка",
                    String.format("Превышен лимит %.0f ч", WorkTimeService.MAX_DAILY_HOURS),
                    String.format("Переработка %d ч. Зарегистрировать?", overtime));
            if (!allowOvertime) {
                weekTableView.refresh();
                return;
            }
        }

        entry.getDayHours().put(day, newValue);
        saveWeekEntryToService(entry, entry);
        updateWeekTotals();
        highlightOvertimeDays();
        weekTableView.refresh();
    }

    private void editWeekEntry() {
        WeekEntry selected = weekTableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        com.reminder.ui.dialog.EntryEditDialog.EntryEditResult result =
                com.reminder.ui.dialog.EntryEditDialog.show(
                        "✏️ Редактировать задачу", "Измените данные задачи недели",
                        typeChoice.getItems(),
                        selected.getProject(), selected.getTaskName(), selected.getType(),
                        null, selected.getComment(), false);
        if (result == null) return;

        WeekEntry updated = new WeekEntry(result.project(), result.taskName(), result.type());
        updated.setComment(result.comment());
        updated.getDayHours().putAll(selected.getDayHours());

        int idx = weekEntries.indexOf(selected);
        weekEntries.set(idx, updated);
        saveWeekEntryToService(updated, selected);
        taskService.addPredefinedTask(new Task(result.project(), result.taskName(), result.type()));

        refreshChoices();
        updateWeekTotals();
        highlightOvertimeDays();
        weekTableView.refresh();
        Dialogs.info("Успех", "✅ Задача обновлена!");
    }

    private void deleteWeekEntry() {
        WeekEntry selected = weekTableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (Dialogs.confirm("Подтверждение удаления", "🗑️ Удалить задачу за неделю?",
                String.format("%s - %s", selected.getProject(), selected.getTaskName()))) {
            removeRecordsFor(selected);
            weekEntries.remove(selected);
            updateWeekTotals();
            weekTableView.refresh();
        }
    }

    private void clearWeekEntries() {
        if (Dialogs.confirm("Подтверждение очистки",
                "🗑️ Очистить все записи за неделю?",
                "Это действие нельзя отменить!")) {
            taskService.clearEntriesForDates(weekDays);
            weekEntries.clear();
            updateWeekTotals();
            weekTableView.refresh();
        }
    }

    /** Сохраняет недельную задачу в записи: удаляет старые (previous) и пишет новые. */
    private void saveWeekEntryToService(WeekEntry entry, WeekEntry previous) {
        removeRecordsFor(previous);
        removeRecordsFor(entry);

        for (LocalDate day : weekDays) {
            int hours = entry.getDayHours().getOrDefault(day, 0);
            if (hours > 0) {
                WorkEntry workEntry = new WorkEntry(
                        entry.getProject(), entry.getTaskName(), entry.getType(), hours);
                workEntry.setDate(day);
                workEntry.setComment(entry.getComment());
                taskService.addWorkEntry(workEntry, true);
            }
        }
    }

    private void removeRecordsFor(WeekEntry weekEntry) {
        for (LocalDate date : weekDays) {
            List<WorkEntry> entries = taskService.getEntriesForDate(date);
            List<WorkEntry> toRemove = entries.stream()
                    .filter(e -> e.getProject().equals(weekEntry.getProject())
                            && e.getTaskName().equals(weekEntry.getTaskName())
                            && e.getType().equals(weekEntry.getType()))
                    .toList();
            toRemove.forEach(taskService::removeWorkEntry);
        }
    }

    // ==================== ДИАЛОГИ ====================

    private void openRemindersDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reminders-dialog.fxml"));
            DialogPane pane = loader.load();
            RemindersDialogController ctrl = loader.getController();
            ctrl.init(reminderService);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("⏰ Управление напоминаниями");
            dialog.setDialogPane(pane);
            dialog.showAndWait();
        } catch (IOException e) {
            Dialogs.info("Ошибка", "❌ Не удалось открыть окно напоминаний: " + Dialogs.reason(e));
        }
    }

    private void openExportDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/export-dialog.fxml"));
            DialogPane pane = loader.load();
            ExportDialogController ctrl = loader.getController();
            ctrl.init(taskService, exportService, datePicker.getValue(), isWeekView, datePicker.getScene().getWindow(), Dialogs::info);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("📊 Экспорт данных");
            dialog.setDialogPane(pane);
            dialog.showAndWait();
        } catch (IOException e) {
            Dialogs.info("Ошибка", "❌ Не удалось открыть окно экспорта: " + Dialogs.reason(e));
        }
    }

    private void openHolidaysDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/holidays-dialog.fxml"));
            DialogPane pane = loader.load();
            HolidaysDialogController ctrl = loader.getController();
            ctrl.init(workTimeService, Dialogs::info);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("📅 Управление праздничными днями");
            dialog.setDialogPane(pane);
            dialog.showAndWait();

            if (isWeekView) {
                updateWeekView(datePicker.getValue());
            }
        } catch (IOException e) {
            Dialogs.info("Ошибка", "❌ Не удалось открыть окно праздников: " + Dialogs.reason(e));
        }
    }

    private void showReminderDialog(Reminder reminder) {
        playReminderSound();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("⏰ Напоминание");
        alert.setHeaderText("Время напоминания!");
        alert.setContentText(reminder.getText());

        ButtonType snoozeButton = new ButtonType("Отложить");
        ButtonType dismissButton = new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(snoozeButton, dismissButton);

        Spinner<Integer> snoozeMinutes = new Spinner<>(10, 120, 10, 5);
        VBox vbox = new VBox(10, new Label("Отложить на (минут):"), snoozeMinutes);
        vbox.setPadding(new Insets(10));
        alert.getDialogPane().setExpandableContent(vbox);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == snoozeButton) {
                reminder.setSnoozeUntil(LocalDateTime.now().plusMinutes(snoozeMinutes.getValue()));
                reminder.setActive(true);
                reminderService.updateReminder(reminder);
            } else if (!reminder.isRepeatDaily()) {
                reminder.setActive(false);
                reminderService.updateReminder(reminder);
            }
        }
    }

    /** Воспроизводит встроенный звук напоминания. */
    private void playReminderSound() {
        try {
            var resource = getClass().getResource("/sounds/reminder.wav");
            if (resource == null) {
                System.err.println("Cannot play reminder sound: resource not found");
                return;
            }
            new AudioClip(resource.toExternalForm()).play();
        } catch (Exception e) {
            System.err.println("Cannot play reminder sound: " + e.getMessage());
        }
    }

    /** Диалог редактирования записи/задачи вынесен в {@link com.reminder.ui.dialog.EntryEditDialog}. */

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
                Dialogs.info("Внимание", "⚠️ Файл не содержит задач");
                return;
            }
            for (Task task : tasks) {
                taskService.addPredefinedTask(task);
            }
            refreshChoices();
            Dialogs.info("Успех", String.format("✅ Загружено %d задач!", tasks.size()));
        } catch (Exception e) {
            Dialogs.info("Ошибка", "❌ Не удалось загрузить файл: " + e.getMessage());
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
            Dialogs.info("Ошибка", "❌ Не удалось сохранить шаблон: " + e.getMessage());
        }
    }

    private void refreshChoices() {
        String currentProject = projectChoice.getEditor().getText();
        projectChoice.getItems().clear();
        projectChoice.getItems().addAll(taskService.getAllProjects());
        if (currentProject.isEmpty() && !projectChoice.getItems().isEmpty()) {
            projectChoice.setValue(projectChoice.getItems().get(0));
        } else if (!currentProject.isEmpty()) {
            projectChoice.getEditor().setText(currentProject);
        }

        String currentTask = taskChoice.getEditor().getText();
        taskChoice.getItems().clear();
        List<String> cleanTasks = com.reminder.util.TaskValidationUtils.filterTaskNames(taskService.getAllTaskNames());
        taskChoice.getItems().addAll(cleanTasks);
        if (!currentTask.isEmpty()) {
            taskChoice.getEditor().setText(currentTask);
        }
    }

    private void clearInputFields() {
        hoursField.clear();
        commentArea.clear();
        projectChoice.getEditor().clear();
        taskChoice.getEditor().clear();
        typeChoice.setValue("Разработка");
        projectChoice.setValue(null);
        taskChoice.setValue(null);
        projectChoice.setPromptText("Введите или выберите проект");
        taskChoice.setPromptText("Введите или выберите задачу");
    }

    private void setupReminders() {
        if (reminderService.getReminders().isEmpty()) {
            Reminder reminder = new Reminder(LocalTime.of(12, 0),
                    "📋 Не забудьте заполнить отчет за первую половину дня!");
            reminder.setRepeatDaily(true);
            reminderService.addReminder(reminder);
        }
        reminderService.setOnReminderFire(this::showReminderDialog);
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