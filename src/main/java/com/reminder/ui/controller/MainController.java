package com.reminder.ui.controller;

import com.reminder.model.Reminder;
import com.reminder.model.Task;
import com.reminder.model.UpdateInfo;
import com.reminder.model.WeekEntry;
import com.reminder.model.WorkEntry;
import com.reminder.service.ExportService;
import com.reminder.service.ReminderService;
import com.reminder.service.TaskService;
import com.reminder.service.UpdateCheckService;
import com.reminder.service.WorkTimeService;
import com.reminder.service.YamlLoaderService;
import com.reminder.ui.component.WeekEntryCell;
import com.reminder.ui.component.WeekTotalCell;
import com.reminder.util.DateUtils;
import com.reminder.util.RussianDays;
import com.reminder.util.VersionInfo;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
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
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

/** Контроллер главного окна. Собирает слой UI поверх сервисов бизнес-логики. */
public class MainController {

    @FXML private StackPane centerStack;
    @FXML private VBox dayPanel;
    @FXML private VBox weekPanel;
    @FXML private VBox hoursContainer;
    @FXML private Label dayTotalLabel;
    @FXML private Label dayRemainingLabel;
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

    private final ObservableList<WorkEntry> dayEntries = FXCollections.observableArrayList();
    private final ObservableList<WeekEntry> weekEntries = FXCollections.observableArrayList();

    private final List<LocalDate> weekDays = new ArrayList<>();
    private final List<WeekDayColumn> weekDayColumns = new ArrayList<>();
    private boolean isWeekView = false;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @FXML
    private void initialize() {
        configureDatePicker();
        configureColumns();
        createWeekDayColumns();
        wireButtons();
        wireViewToggle();
        loadTypeChoices();
        refreshChoices();

        setupHolidays();
        setupReminders();
        setupUpdateCheck();
        startClock();

        updateDayView(datePicker.getValue());
        reminderService.startMonitoring();
    }

    // ==================== НАСТРОЙКА UI ====================

    private void configureDatePicker() {
        datePicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? date.format(DATE_FORMATTER) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.isEmpty()) return null;
                try {
                    return LocalDate.parse(string, DATE_FORMATTER);
                } catch (Exception e) {
                    return null;
                }
            }
        });
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
    private final class WeekDayColumn extends TableColumn<WeekEntry, Integer> {
        private LocalDate date;

        WeekDayColumn(LocalDate date) {
            this.date = date;
        }

        LocalDate date() {
            return date;
        }

        void setDay(LocalDate d) {
            this.date = d;
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
            col.setCellFactory(cf -> new WeekEntryCell(col.date()));
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
        dayEntries.setAll(taskService.getEntriesForDate(date));

        double total = taskService.getTotalHoursForDate(date);
        double remaining = WorkTimeService.MAX_DAILY_HOURS - total;

        dayTotalLabel.setText(String.format("Загружено: %.1f ч", total));
        dayRemainingLabel.setText(String.format("Осталось: %.1f ч", remaining));

        String color;
        if (remaining < 0) {
            color = "red";
        } else if (remaining < 1) {
            color = "orange";
        } else {
            color = "green";
        }
        dayRemainingLabel.setStyle(
                "-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " + color + ";");
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
            col.setText(RussianDays.shortName(day.getDayOfWeek().getValue()) + " " + DateUtils.formatShort(day));
            boolean holiday = workTimeService.isHoliday(day);
            col.setEditable(!holiday);
            if (holiday) {
                col.setStyle("-fx-alignment: CENTER; -fx-text-fill: #999; -fx-background-color: #f5f5f5;");
            } else {
                col.setStyle("-fx-alignment: CENTER;");
            }
        }

        List<WorkEntry> entries = taskService.getEntriesForDateRange(startOfWeek, endOfWeek);
        weekEntries.setAll(workTimeService.aggregateWeek(entries));
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

    // ==================== ДНЕВНОЙ РЕЖИМ ====================

    private void addDayEntry() {
        try {
            String project = projectChoice.getEditor().getText().trim();
            String taskName = taskChoice.getEditor().getText().trim();
            String type = typeChoice.getValue();
            String hoursText = hoursField.getText().trim();
            String comment = commentArea.getText().trim();

            if (project.isEmpty()) {
                showAlert("Ошибка", "⚠️ Пожалуйста, укажите проект");
                projectChoice.getEditor().requestFocus();
                return;
            }
            if (taskName.isEmpty()) {
                showAlert("Ошибка", "⚠️ Пожалуйста, укажите задачу");
                taskChoice.getEditor().requestFocus();
                return;
            }
            if (type == null || type.isEmpty()) {
                showAlert("Ошибка", "⚠️ Пожалуйста, выберите тип работы");
                typeChoice.requestFocus();
                return;
            }
            if (hoursText.isEmpty()) {
                showAlert("Ошибка", "⚠️ Пожалуйста, укажите длительность");
                hoursField.requestFocus();
                return;
            }

            int hours;
            try {
                hours = Integer.parseInt(hoursText);
            } catch (NumberFormatException e) {
                showAlert("Ошибка", "⚠️ Длительность должна быть целым числом");
                hoursField.requestFocus();
                return;
            }
            if (hours <= 0 || hours > 24) {
                showAlert("Ошибка", "⚠️ Длительность должна быть от 1 до 24 часов");
                hoursField.requestFocus();
                return;
            }

            LocalDate date = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();

            double currentTotal = taskService.getTotalHoursForDate(date);
            if (currentTotal + hours > WorkTimeService.MAX_DAILY_HOURS) {
                showAlert("Ошибка", String.format(
                        "⚠️ Превышен дневной лимит (%.0f часов)!\nОсталось: %.1f ч",
                        WorkTimeService.MAX_DAILY_HOURS, WorkTimeService.MAX_DAILY_HOURS - currentTotal));
                return;
            }

            WorkEntry entry = new WorkEntry(project, taskName, type, hours);
            entry.setDate(date);
            entry.setComment(comment);

            if (taskService.addWorkEntry(entry)) {
                taskService.addPredefinedTask(new Task(project, taskName, type));
                refreshChoices();
                clearInputFields();
                updateDayView(date);
                showAlert("Успех", "✅ Работа добавлена!");
            }
        } catch (Exception e) {
            showAlert("Ошибка", "❌ Произошла ошибка: " + e.getMessage());
        }
    }

    private void editDayEntry() {
        WorkEntry selected = dayTableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        EntryEditResult result = showEntryEditDialog(
                "✏️ Редактировать запись", "Измените данные записи",
                selected.getProject(), selected.getTaskName(), selected.getType(),
                String.valueOf((int) selected.getHours()), selected.getComment(), true);
        if (result == null) return;

        int hours;
        try {
            hours = Integer.parseInt(result.hours());
        } catch (NumberFormatException e) {
            showAlert("Ошибка", "⚠️ Длительность должна быть целым числом");
            return;
        }
        if (hours <= 0 || hours > 24) {
            showAlert("Ошибка", "⚠️ Длительность должна быть от 1 до 24 часов");
            return;
        }

        WorkEntry updated = new WorkEntry(result.project(), result.taskName(), result.type(), hours);
        updated.setDate(selected.getDate());
        updated.setComment(result.comment());

        if (taskService.updateWorkEntry(selected, updated)) {
            taskService.addPredefinedTask(new Task(result.project(), result.taskName(), result.type()));
            refreshChoices();
            updateDayView(selected.getDate());
            showAlert("Успех", "✅ Запись обновлена!");
        } else {
            showAlert("Ошибка", "⚠️ Не удалось сохранить (возможно, превышен дневной лимит)");
        }
    }

    private void deleteDayEntry() {
        WorkEntry selected = dayTableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("🗑️ Удалить запись?");
        alert.setContentText(String.format("%s - %s (%.0f ч)",
                selected.getProject(), selected.getTaskName(), selected.getHours()));

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            taskService.removeWorkEntry(selected);
            updateDayView(selected.getDate());
        }
    }

    private void clearDayEntries() {
        LocalDate date = datePicker.getValue();
        if (date == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение очистки");
        alert.setHeaderText("🗑️ Очистить все записи за " + date.format(DATE_FORMATTER) + "?");
        alert.setContentText("Это действие нельзя отменить!");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            taskService.clearEntriesForDate(date);
            updateDayView(date);
        }
    }

    // ==================== НЕДЕЛЬНЫЙ РЕЖИМ ====================

    private void addWeekEntry() {
        try {
            String project = projectChoice.getEditor().getText().trim();
            String taskName = taskChoice.getEditor().getText().trim();
            String type = typeChoice.getValue();
            String comment = commentArea.getText().trim();

            if (project.isEmpty()) {
                showAlert("Ошибка", "⚠️ Пожалуйста, укажите проект");
                projectChoice.getEditor().requestFocus();
                return;
            }
            if (taskName.isEmpty()) {
                showAlert("Ошибка", "⚠️ Пожалуйста, укажите задачу");
                taskChoice.getEditor().requestFocus();
                return;
            }
            if (type == null || type.isEmpty()) {
                showAlert("Ошибка", "⚠️ Пожалуйста, выберите тип работы");
                typeChoice.requestFocus();
                return;
            }

            for (WeekEntry entry : weekEntries) {
                if (entry.getProject().equals(project)
                        && entry.getTaskName().equals(taskName)
                        && entry.getType().equals(type)) {
                    showAlert("Внимание", "⚠️ Такая задача уже существует в этой неделе");
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
            showAlert("Успех", "✅ Задача добавлена! Введите часы в таблице (двойной клик).");
        } catch (Exception e) {
            showAlert("Ошибка", "❌ Произошла ошибка: " + e.getMessage());
        }
    }

    private void handleWeekCellEdit(TableColumn.CellEditEvent<WeekEntry, Integer> event) {
        WeekDayColumn col = (WeekDayColumn) event.getTableColumn();
        WeekEntry entry = event.getRowValue();
        LocalDate day = col.date();
        Integer newValue = event.getNewValue();
        if (newValue == null || newValue < 0) {
            weekTableView.refresh();
            return;
        }

        int oldValue = entry.getDayHours().getOrDefault(day, 0);
        int currentDayTotal = 0;
        for (WeekEntry we : weekEntries) {
            currentDayTotal += we.getDayHours().getOrDefault(day, 0);
        }

        if (currentDayTotal - oldValue + newValue > WorkTimeService.MAX_DAILY_HOURS) {
            showAlert("Предупреждение", String.format(
                    "⚠️ Превышен дневной лимит (%.0f часов)!\nТекущий дневной итог: %d ч",
                    WorkTimeService.MAX_DAILY_HOURS, currentDayTotal - oldValue));
            weekTableView.refresh();
            return;
        }

        entry.getDayHours().put(day, newValue);
        saveWeekEntryToService(entry, entry);
        updateWeekTotals();
        weekTableView.refresh();
    }

    private void editWeekEntry() {
        WeekEntry selected = weekTableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        EntryEditResult result = showEntryEditDialog(
                "✏️ Редактировать задачу", "Измените данные задачи недели",
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
        weekTableView.refresh();
        showAlert("Успех", "✅ Задача обновлена!");
    }

    private void deleteWeekEntry() {
        WeekEntry selected = weekTableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("🗑️ Удалить задачу за неделю?");
        alert.setContentText(String.format("%s - %s", selected.getProject(), selected.getTaskName()));

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            removeRecordsFor(selected);
            weekEntries.remove(selected);
            updateWeekTotals();
            weekTableView.refresh();
        }
    }

    private void clearWeekEntries() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение очистки");
        alert.setHeaderText("🗑️ Очистить все записи за неделю?");
        alert.setContentText("Это действие нельзя отменить!");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
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
                taskService.addWorkEntry(workEntry);
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
            showAlert("Ошибка", "❌ Не удалось открыть окно напоминаний: " + e.getMessage());
        }
    }

    private void openExportDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/export-dialog.fxml"));
            DialogPane pane = loader.load();
            ExportDialogController ctrl = loader.getController();
            ctrl.init(taskService, exportService, datePicker.getValue(), isWeekView, this::showAlert);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("📊 Экспорт данных");
            dialog.setDialogPane(pane);
            dialog.showAndWait();
        } catch (IOException e) {
            showAlert("Ошибка", "❌ Не удалось открыть окно экспорта: " + e.getMessage());
        }
    }

    private void openHolidaysDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/holidays-dialog.fxml"));
            DialogPane pane = loader.load();
            HolidaysDialogController ctrl = loader.getController();
            ctrl.init(workTimeService, this::showAlert);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("📅 Управление праздничными днями");
            dialog.setDialogPane(pane);
            dialog.showAndWait();

            if (isWeekView) {
                updateWeekView(datePicker.getValue());
            }
        } catch (IOException e) {
            showAlert("Ошибка", "❌ Не удалось открыть окно праздников: " + e.getMessage());
        }
    }

    private void showReminderDialog(Reminder reminder) {
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

    /** Диалог редактирования записи/задачи. Возвращает результат или null при отмене. */
    private EntryEditResult showEntryEditDialog(String title, String header,
                                                String project, String taskName, String type,
                                                String hours, String comment, boolean withHours) {
        Dialog<EntryEditResult> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        ButtonType saveType = new ButtonType("💾 Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField projectField = new TextField(project);
        TextField taskField = new TextField(taskName);
        ComboBox<String> typeField = new ComboBox<>(typeChoice.getItems());
        typeField.setEditable(true);
        typeField.setValue(type);
        TextField hoursFieldDialog = new TextField(hours);
        TextArea commentField = new TextArea(comment);
        commentField.setPrefRowCount(3);

        grid.addRow(0, new Label("Проект:"), projectField);
        grid.addRow(1, new Label("Задача:"), taskField);
        grid.addRow(2, new Label("Тип работ:"), typeField);
        if (withHours) {
            grid.addRow(3, new Label("Длительность (часы):"), hoursFieldDialog);
            grid.addRow(4, new Label("Комментарий:"), commentField);
        } else {
            grid.addRow(3, new Label("Комментарий:"), commentField);
        }
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton != saveType) return null;
            String p = projectField.getText().trim();
            String t = taskField.getText().trim();
            String ty = typeField.getValue() != null ? typeField.getValue().trim() : "";
            if (p.isEmpty() || t.isEmpty() || ty.isEmpty()) return null;
            return new EntryEditResult(p, t, ty,
                    withHours ? hoursFieldDialog.getText().trim() : null,
                    commentField.getText().trim());
        });

        return dialog.showAndWait().orElse(null);
    }

    private record EntryEditResult(String project, String taskName, String type,
                                   String hours, String comment) {
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
                showAlert("Внимание", "⚠️ Файл не содержит задач");
                return;
            }
            for (Task task : tasks) {
                taskService.addPredefinedTask(task);
            }
            refreshChoices();
            showAlert("Успех", String.format("✅ Загружено %d задач!", tasks.size()));
        } catch (Exception e) {
            showAlert("Ошибка", "❌ Не удалось загрузить файл: " + e.getMessage());
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
        taskChoice.getItems().addAll(taskService.getAllTaskNames());
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

    private void setupHolidays() {
        workTimeService.setDefaultHolidays();
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
        updateButton.setOnAction(e -> checkForUpdates(true));
        checkForUpdates(false);
    }

    /** Проверяет наличие обновлений. При manual=true всегда показывает результат. */
    private void checkForUpdates(boolean manual) {
        updateCheckService.checkLatest().thenAccept(update ->
                Platform.runLater(() -> handleUpdateResult(update, manual)));
    }

    private void handleUpdateResult(UpdateInfo update, boolean manual) {
        if (update.newer()) {
            showUpdateDialog(update, manual);
        } else if (manual) {
            showAlert("Обновления", "✅ У вас установлена актуальная версия v" + VersionInfo.load().version());
        }
    }

    private void showUpdateDialog(UpdateInfo update, boolean manual) {
        String latestKey = "v" + update.version();
        Preferences prefs = Preferences.userRoot().node("com/reminder");
        if (!manual && latestKey.equals(prefs.get("lastSeenVersion", ""))) {
            return;
        }
        prefs.put("lastSeenVersion", latestKey);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Доступно обновление");
        alert.setHeaderText("Доступна новая версия " + latestKey);
        String current = "v" + VersionInfo.load().version();
        alert.setContentText("Текущая версия: " + current + "\nНовая версия: " + latestKey);

        ButtonType downloadButton = new ButtonType("Скачать zip", ButtonBar.ButtonData.OK_DONE);
        ButtonType releaseButton = new ButtonType("Открыть релиз");
        ButtonType closeButton = new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(downloadButton, releaseButton, closeButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == downloadButton) {
                String url = !update.downloadUrl().isEmpty() ? update.downloadUrl() : update.releaseUrl();
                openLink(url);
            } else if (result.get() == releaseButton) {
                openLink(update.releaseUrl());
            }
        }
    }

    private void openLink(String url) {
        if (url == null || url.isEmpty()) {
            showAlert("Ошибка", "Ссылка недоступна");
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось открыть ссылку: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
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
            e.printStackTrace();
        }
    }

    // Шутка начало
    @FXML
    private Label addJobLabel;
    private int clickCount = 0;
    private Timeline resetTimeline;


    @FXML
    private void handleLabelClick() {
        // Инициализируем таймер при первом нажатии
        if (resetTimeline == null) {
            resetTimeline = new Timeline(new KeyFrame(Duration.seconds(1.0), event -> {
                clickCount = 0; // Время вышло, обнуляем
            }));
            resetTimeline.setCycleCount(1);
        }
        resetTimeline.stop();
        clickCount++;
        resetTimeline.play();
        if (clickCount == 20) {
            resetTimeline.stop();
            clickCount = 0;
            showAlertWithJoke();
        }
    }
    private void showAlertWithJoke() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Пасхалка!");
        alert.setHeaderText("Кто-то знатно заспамил кнопку...");
        alert.setContentText("— Алло, это техподдержка?\n— Да.\n— Я тут 20 раз подряд нажал на текст, и у меня открылось это окно. Что мне делать?\n— Поздравляем, вы прошли тест на стрессоустойчивость!");
        alert.showAndWait();
    }
    //ШУтка конец
}