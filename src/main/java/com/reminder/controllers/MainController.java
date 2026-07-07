package com.reminder.controllers;

import com.reminder.components.WeekEntryCell;
import com.reminder.components.WeekTotalCell;
import com.reminder.models.*;
import com.reminder.services.*;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;

public class MainController {
    private BorderPane root;
    private TaskService taskService;
    private ReminderService reminderService;
    private YamlLoaderService yamlLoaderService;
    private ExportService exportService;

    // UI компоненты
    private TableView<WorkEntry> dayTableView;
    private TableView<WeekEntry> weekTableView;
    private Label dayRemainingLabel;
    private Label dayTotalLabel;
    private Label weekTotalLabel;
    private DatePicker datePicker;
    private ComboBox<String> projectChoice;
    private ComboBox<String> taskChoice;
    private ComboBox<String> typeChoice;
    private TextField hoursField;
    private TextArea commentArea;
    private ObservableList<WorkEntry> dayEntries = FXCollections.observableArrayList();
    private ObservableList<WeekEntry> weekEntries = FXCollections.observableArrayList();
    private ObservableList<Reminder> remindersList = FXCollections.observableArrayList();

    // Для праздничных дней
    private Set<LocalDate> holidays = new HashSet<>();

    // Для отображения недели
    private List<LocalDate> weekDays = new ArrayList<>();

    // Режим просмотра
    private boolean isWeekView = false;

    // Лимиты
    private static final int MAX_DAILY_HOURS = 8;
    private static final int MAX_WEEKLY_HOURS = 48;

    public MainController() {
        this.taskService = new TaskService();
        this.reminderService = new ReminderService();
        this.yamlLoaderService = new YamlLoaderService();
        this.exportService = new ExportService();

        initializeUI();
        setupReminders();
        setupHolidays();

        reminderService.startMonitoring();
        updateDayView(LocalDate.now());
    }

    private void initializeUI() {
        root = new BorderPane();
        root.setPadding(new Insets(10));

        root.setTop(createHeaderPanel());
        root.setCenter(createTablePanel());
        root.setLeft(createInputPanel());
        root.setBottom(createStatusPanel());
    }

    private VBox createHeaderPanel() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(5, 0, 10, 0));

        Label titleLabel = new Label("📋 Учет рабочего времени");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        HBox controlBox = new HBox(10);
        controlBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Переключатель день/неделя
        ToggleGroup viewToggle = new ToggleGroup();
        RadioButton dayView = new RadioButton("📅 День");
        dayView.setToggleGroup(viewToggle);
        dayView.setSelected(true);
        RadioButton weekView = new RadioButton("📊 Неделя");
        weekView.setToggleGroup(viewToggle);

        viewToggle.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == weekView) {
                isWeekView = true;
                showWeekView();
            } else {
                isWeekView = false;
                showDayView();
            }
        });

        datePicker = new DatePicker(LocalDate.now());
        datePicker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "";
            }
            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.isEmpty()) return null;
                try {
                    return LocalDate.parse(string, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                } catch (Exception e) {
                    return null;
                }
            }
        });
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (isWeekView) {
                    showWeekView();
                } else {
                    updateDayView(newVal);
                }
            }
        });

        Button todayButton = new Button("Сегодня");
        todayButton.setOnAction(e -> datePicker.setValue(LocalDate.now()));

        Button prevButton = new Button("◀");
        prevButton.setOnAction(e -> {
            if (isWeekView) {
                datePicker.setValue(datePicker.getValue().minusWeeks(1));
            } else {
                datePicker.setValue(datePicker.getValue().minusDays(1));
            }
        });

        Button nextButton = new Button("▶");
        nextButton.setOnAction(e -> {
            if (isWeekView) {
                datePicker.setValue(datePicker.getValue().plusWeeks(1));
            } else {
                datePicker.setValue(datePicker.getValue().plusDays(1));
            }
        });

        Button holidayButton = new Button("📅 Праздники");
        holidayButton.setOnAction(e -> manageHolidays());

        controlBox.getChildren().addAll(
                dayView, weekView,
                prevButton, datePicker, nextButton, todayButton,
                new Separator(Orientation.VERTICAL),
                holidayButton
        );

        header.getChildren().addAll(titleLabel, controlBox);
        return header;
    }

    private VBox createInputPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(320);
        panel.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 1px;");

        Label titleLabel = new Label("➕ Добавить работу");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Проект
        Label projectLabel = new Label("Проект:");
        projectChoice = new ComboBox<>();
        projectChoice.setEditable(true);
        projectChoice.setPromptText("Введите или выберите проект");
        projectChoice.getEditor().setPromptText("Введите название проекта");
        projectChoice.getItems().addAll(taskService.getAllProjects());

        // Задача
        Label taskLabel = new Label("Задача:");
        taskChoice = new ComboBox<>();
        taskChoice.setEditable(true);
        taskChoice.setPromptText("Введите или выберите задачу");
        taskChoice.getEditor().setPromptText("Введите название задачи");
        List<String> tasks = taskService.getAllTaskNames();
        if (!tasks.isEmpty()) {
            taskChoice.getItems().addAll(tasks);
        }

        // Тип
        Label typeLabel = new Label("Тип работ:");
        typeChoice = new ComboBox<>();
        typeChoice.setEditable(false);
        typeChoice.setPromptText("Выберите тип работы");
        typeChoice.getItems().addAll("Разработка", "Тестирование", "Дизайн", "Аналитика", "Управление", "Коммуникация", "Другое");
        typeChoice.setValue("Разработка");

        // Часы (только для дневного режима)
        Label hoursLabel = new Label("Длительность (часы):");
        hoursField = new TextField();
        hoursField.setPromptText("Например: 1, 2, 3...");
        hoursField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                hoursField.setText(oldVal);
            }
        });

        // Скрываем поле часов по умолчанию (для недельного режима)
        hoursLabel.setVisible(true);
        hoursField.setVisible(true);

        // Комментарий
        Label commentLabel = new Label("Комментарий:");
        commentArea = new TextArea();
        commentArea.setPrefRowCount(3);
        commentArea.setPromptText("Дополнительная информация...");

        // Кнопки
        Button addButton = new Button("✅ Добавить");
        addButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(e -> {
            if (isWeekView) {
                addWeekEntry();
            } else {
                addDayEntry();
            }
        });

        Button loadYamlButton = new Button("📂 Загрузить задачи из YAML");
        loadYamlButton.setMaxWidth(Double.MAX_VALUE);
        loadYamlButton.setOnAction(e -> loadTasksFromYaml());

        Button clearButton = new Button("🗑️ Очистить");
        clearButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        clearButton.setMaxWidth(Double.MAX_VALUE);
        clearButton.setOnAction(e -> {
            if (isWeekView) {
                clearWeekEntries();
            } else {
                clearDayEntries();
            }
        });

        // Сохраняем ссылки на элементы для управления видимостью
        VBox hoursContainer = new VBox(5);
        hoursContainer.getChildren().addAll(hoursLabel, hoursField);

        panel.getChildren().addAll(
                titleLabel,
                projectLabel, projectChoice,
                taskLabel, taskChoice,
                typeLabel, typeChoice,
                hoursContainer,
                commentLabel, commentArea,
                addButton,
                new Separator(),
                loadYamlButton,
                clearButton
        );

        // Сохраняем ссылку на контейнер для управления видимостью
        panel.setUserData(hoursContainer);

        return panel;
    }

    private StackPane createTablePanel() {
        StackPane stackPane = new StackPane();

        // --- Дневной режим ---
        VBox dayPanel = new VBox(10);
        dayPanel.setPadding(new Insets(0, 10, 0, 10));
        dayPanel.setVisible(true);

        HBox dayInfoBox = new HBox(20);
        dayInfoBox.setPadding(new Insets(5, 0, 5, 0));
        dayInfoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        dayTotalLabel = new Label("Загружено: 0.0 ч");
        dayTotalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        dayRemainingLabel = new Label("Осталось: 8.0 ч");
        dayRemainingLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        dayInfoBox.getChildren().addAll(dayTotalLabel, dayRemainingLabel);

        dayTableView = new TableView<>();
        dayTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Настройка строк для дневного режима - с черным цветом текста при выделении
        dayTableView.setRowFactory(tv -> {
            TableRow<WorkEntry> row = new TableRow<>();
            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) {
                    row.setStyle("-fx-font-weight: bold;");
                }
            });
            row.setOnMouseExited(e -> {
                if (!row.isEmpty() && !row.isSelected()) {
                    row.setStyle("-fx-font-weight: normal;");
                }
            });
            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    row.setStyle("-fx-font-weight: bold; -fx-background-color: #e3f2fd; -fx-text-fill: black;");
                } else {
                    row.setStyle("-fx-font-weight: normal;");
                }
            });
            return row;
        });

        // Колонка "Проект" с переносом
        TableColumn<WorkEntry, String> dayProjectCol = new TableColumn<>("Проект");
        dayProjectCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getProject()));
        dayProjectCol.setPrefWidth(120);
        dayProjectCol.setCellFactory(tc -> {
            TableCell<WorkEntry, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                        setWrapText(true);
                    }
                }
            };
            return cell;
        });

        // Колонка "Задача" с переносом
        TableColumn<WorkEntry, String> dayTaskCol = new TableColumn<>("Задача");
        dayTaskCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTaskName()));
        dayTaskCol.setPrefWidth(150);
        dayTaskCol.setCellFactory(tc -> {
            TableCell<WorkEntry, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                        setWrapText(true);
                    }
                }
            };
            return cell;
        });

        TableColumn<WorkEntry, String> dayTypeCol = new TableColumn<>("Тип");
        dayTypeCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getType()));
        dayTypeCol.setPrefWidth(100);

        TableColumn<WorkEntry, Integer> dayHoursCol = new TableColumn<>("Часы");
        dayHoursCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty((int)cellData.getValue().getHours()).asObject());
        dayHoursCol.setPrefWidth(70);
        dayHoursCol.setStyle("-fx-alignment: CENTER;");

        // Колонка "Комментарий" с переносом
        TableColumn<WorkEntry, String> dayCommentCol = new TableColumn<>("Комментарий");
        dayCommentCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getComment()));
        dayCommentCol.setPrefWidth(200);
        dayCommentCol.setCellFactory(tc -> {
            TableCell<WorkEntry, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                        setWrapText(true);
                    }
                }
            };
            return cell;
        });

        dayTableView.getColumns().addAll(dayProjectCol, dayTaskCol, dayTypeCol, dayHoursCol, dayCommentCol);
        dayTableView.setItems(dayEntries);

        // Контекстное меню для дневного режима
        ContextMenu dayContextMenu = new ContextMenu();
        MenuItem dayEditItem = new MenuItem("✏️ Редактировать");
        dayEditItem.setOnAction(e -> editDayEntry());
        MenuItem dayDeleteItem = new MenuItem("🗑️ Удалить");
        dayDeleteItem.setOnAction(e -> deleteDayEntry());
        dayContextMenu.getItems().addAll(dayEditItem, dayDeleteItem);
        dayTableView.setContextMenu(dayContextMenu);

        VBox.setVgrow(dayTableView, Priority.ALWAYS);
        dayPanel.getChildren().addAll(dayInfoBox, dayTableView);

        // --- Недельный режим ---
        VBox weekPanel = new VBox(10);
        weekPanel.setPadding(new Insets(0, 10, 0, 10));
        weekPanel.setVisible(false);

        HBox weekInfoBox = new HBox(20);
        weekInfoBox.setPadding(new Insets(5, 0, 5, 0));
        weekInfoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        weekTotalLabel = new Label("Неделя: 0 / 48 ч");
        weekTotalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2196F3;");

        Label weekHint = new Label("(двойной клик для редактирования)");
        weekHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        weekInfoBox.getChildren().addAll(weekTotalLabel, weekHint);

        weekTableView = new TableView<>();
        weekTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        weekTableView.setEditable(true);

        // Настройка строк для недельного режима - с черным цветом текста при выделении
        weekTableView.setRowFactory(tv -> {
            TableRow<WeekEntry> row = new TableRow<>();
            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) {
                    row.setStyle("-fx-font-weight: bold;");
                }
            });
            row.setOnMouseExited(e -> {
                if (!row.isEmpty() && !row.isSelected()) {
                    row.setStyle("-fx-font-weight: normal;");
                }
            });
            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    row.setStyle("-fx-font-weight: bold; -fx-background-color: #e3f2fd; -fx-text-fill: black;");
                } else {
                    row.setStyle("-fx-font-weight: normal;");
                }
            });
            return row;
        });

        // Базовые колонки для недели с переносом
        TableColumn<WeekEntry, String> weekProjectCol = new TableColumn<>("Проект");
        weekProjectCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getProject()));
        weekProjectCol.setPrefWidth(120);
        weekProjectCol.setEditable(false);
        weekProjectCol.setCellFactory(tc -> {
            TableCell<WeekEntry, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                        setWrapText(true);
                    }
                }
            };
            return cell;
        });

        TableColumn<WeekEntry, String> weekTaskCol = new TableColumn<>("Задача");
        weekTaskCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTaskName()));
        weekTaskCol.setPrefWidth(150);
        weekTaskCol.setEditable(false);
        weekTaskCol.setCellFactory(tc -> {
            TableCell<WeekEntry, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                        setWrapText(true);
                    }
                }
            };
            return cell;
        });

        TableColumn<WeekEntry, String> weekTypeCol = new TableColumn<>("Тип работ");
        weekTypeCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getType()));
        weekTypeCol.setPrefWidth(100);
        weekTypeCol.setEditable(false);

        weekTableView.getColumns().addAll(weekProjectCol, weekTaskCol, weekTypeCol);

        // Колонки для дней недели с возможностью редактирования
        weekDays.clear();
        LocalDate startOfWeek = datePicker.getValue().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
        for (int i = 0; i < 7; i++) {
            LocalDate day = startOfWeek.plusDays(i);
            weekDays.add(day);

            String dayName = getRussianDayOfWeek(day.getDayOfWeek().getValue());
            String dateStr = day.format(DateTimeFormatter.ofPattern("dd.MM"));
            String colName = dayName + " " + dateStr;

            TableColumn<WeekEntry, Integer> dayCol = new TableColumn<>(colName);
            dayCol.setCellValueFactory(cellData -> {
                WeekEntry entry = cellData.getValue();
                if (entry != null && entry.getDayHours().containsKey(day)) {
                    return new javafx.beans.property.SimpleIntegerProperty(entry.getDayHours().get(day)).asObject();
                }
                return new javafx.beans.property.SimpleIntegerProperty(0).asObject();
            });
            dayCol.setPrefWidth(70);
            dayCol.setStyle("-fx-alignment: CENTER;");
            dayCol.setEditable(true);

            dayCol.setCellFactory(col -> new WeekEntryCell(day));

            // Проверяем только праздничные дни, выходные теперь редактируемые
            if (isHoliday(day)) {
                dayCol.setStyle("-fx-alignment: CENTER; -fx-text-fill: #999; -fx-background-color: #f5f5f5;");
                dayCol.setEditable(false);
            }

            dayCol.setOnEditCommit(event -> {
                WeekEntry entry = event.getRowValue();
                Integer newValue = event.getNewValue();
                if (newValue != null && newValue >= 0) {
                    int currentDayTotal = 0;
                    for (WeekEntry we : weekEntries) {
                        currentDayTotal += we.getDayHours().getOrDefault(day, 0);
                    }
                    int oldValue = entry.getDayHours().getOrDefault(day, 0);
                    int diff = newValue - oldValue;

                    if (currentDayTotal + diff > MAX_DAILY_HOURS) {
                        showAlert("Предупреждение", String.format(
                                "⚠️ Превышен дневной лимит (8 часов)!\nТекущий дневной итог: %d ч",
                                currentDayTotal - oldValue));
                        weekTableView.refresh();
                        return;
                    }

                    entry.getDayHours().put(day, newValue);
                    saveWeekEntryToService(entry);
                    updateWeekTotals();
                    weekTableView.refresh();
                }
            });

            weekTableView.getColumns().add(dayCol);
        }

        // Колонка "Комментарий" с переносом
        TableColumn<WeekEntry, String> weekCommentCol = new TableColumn<>("Комментарий");
        weekCommentCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getComment()));
        weekCommentCol.setPrefWidth(150);
        weekCommentCol.setEditable(false);
        weekCommentCol.setCellFactory(tc -> {
            TableCell<WeekEntry, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                        setWrapText(true);
                    }
                }
            };
            return cell;
        });
        weekTableView.getColumns().add(weekCommentCol);

        // Колонка "Всего"
        TableColumn<WeekEntry, Integer> totalCol = new TableColumn<>("Всего");
        totalCol.setCellValueFactory(cellData -> {
            WeekEntry entry = cellData.getValue();
            if (entry != null) {
                int total = entry.getTotal();
                return new javafx.beans.property.SimpleIntegerProperty(total).asObject();
            }
            return new javafx.beans.property.SimpleIntegerProperty(0).asObject();
        });
        totalCol.setPrefWidth(70);
        totalCol.setStyle("-fx-alignment: CENTER;");
        totalCol.setCellFactory(col -> new WeekTotalCell());
        totalCol.setEditable(false);

        weekTableView.getColumns().add(totalCol);
        weekTableView.setItems(weekEntries);

        // Контекстное меню для недельного режима
        ContextMenu weekContextMenu = new ContextMenu();
        MenuItem weekEditItem = new MenuItem("✏️ Редактировать задачу");
        weekEditItem.setOnAction(e -> editWeekEntry());
        MenuItem weekDeleteItem = new MenuItem("🗑️ Удалить задачу");
        weekDeleteItem.setOnAction(e -> deleteWeekEntry());
        weekContextMenu.getItems().addAll(weekEditItem, weekDeleteItem);
        weekTableView.setContextMenu(weekContextMenu);

        VBox.setVgrow(weekTableView, Priority.ALWAYS);
        weekPanel.getChildren().addAll(weekInfoBox, weekTableView);

        stackPane.getChildren().addAll(dayPanel, weekPanel);
        return stackPane;
    }

    private HBox createStatusPanel() {
        HBox panel = new HBox(10);
        panel.setPadding(new Insets(10, 0, 0, 0));
        panel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button reminderButton = new Button("⏰ Напоминания");
        reminderButton.setOnAction(e -> manageReminders());

        Button exportButton = new Button("📊 Экспорт");
        exportButton.setOnAction(e -> showExportDialog());

        Label timeLabel = new Label();
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                timeLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
        ));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        panel.getChildren().addAll(
                reminderButton,
                exportButton,
                spacer, timeLabel
        );

        return panel;
    }

    // ==================== ДНЕВНОЙ РЕЖИМ ====================

    private void showDayView() {
        // Показываем дневную панель, скрываем недельную
        for (javafx.scene.Node node : ((StackPane) root.getCenter()).getChildren()) {
            if (node instanceof VBox) {
                VBox vbox = (VBox) node;
                if (vbox.getChildren().size() > 0 && vbox.getChildren().get(0) instanceof HBox) {
                    if (vbox.getChildren().size() > 1 && vbox.getChildren().get(1) instanceof TableView) {
                        TableView<?> table = (TableView<?>) vbox.getChildren().get(1);
                        if (table.getItems() == dayEntries) {
                            vbox.setVisible(true);
                        } else {
                            vbox.setVisible(false);
                        }
                    }
                }
            }
        }

        // Показываем поле часов
        VBox panel = (VBox) root.getLeft();
        if (panel.getUserData() instanceof VBox) {
            VBox hoursContainer = (VBox) panel.getUserData();
            hoursContainer.setVisible(true);  // ДОЛЖНО БЫТЬ true, а не false!
        }

        updateDayView(datePicker.getValue());  // ДОЛЖНО БЫТЬ updateDayView, а не updateWeekView!
    }

    private void showWeekView() {
        // Показываем недельную панель, скрываем дневную
        for (javafx.scene.Node node : ((StackPane) root.getCenter()).getChildren()) {
            if (node instanceof VBox) {
                VBox vbox = (VBox) node;
                if (vbox.getChildren().size() > 0 && vbox.getChildren().get(0) instanceof HBox) {
                    if (vbox.getChildren().size() > 1 && vbox.getChildren().get(1) instanceof TableView) {
                        TableView<?> table = (TableView<?>) vbox.getChildren().get(1);
                        if (table.getItems() == weekEntries) {
                            vbox.setVisible(true);
                        } else {
                            vbox.setVisible(false);
                        }
                    }
                }
            }
        }

        // Скрываем поле часов
        VBox panel = (VBox) root.getLeft();
        if (panel.getUserData() instanceof VBox) {
            VBox hoursContainer = (VBox) panel.getUserData();
            hoursContainer.setVisible(false);  // Скрываем поле часов в недельном режиме
        }

        updateWeekView(datePicker.getValue());
    }  // Закрывающая скобка была пропущена!

    private void updateDayView(LocalDate date) {
        List<WorkEntry> entries = taskService.getEntriesForDate(date);
        dayEntries.setAll(entries);

        double total = taskService.getTotalHoursForDate(date);
        double remaining = MAX_DAILY_HOURS - total;

        dayTotalLabel.setText(String.format("Загружено: %.1f ч", total));
        dayRemainingLabel.setText(String.format("Осталось: %.1f ч", remaining));

        if (remaining < 0) {
            dayRemainingLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: red;");
        } else if (remaining < 1) {
            dayRemainingLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: orange;");
        } else {
            dayRemainingLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: green;");
        }
    }

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

            LocalDate date = datePicker.getValue();
            if (date == null) {
                date = LocalDate.now();
            }

            // Проверяем дневной лимит
            double currentTotal = taskService.getTotalHoursForDate(date);
            if (currentTotal + hours > MAX_DAILY_HOURS) {
                showAlert("Ошибка", String.format(
                        "⚠️ Превышен дневной лимит (8 часов)!\nОсталось: %.1f ч",
                        MAX_DAILY_HOURS - currentTotal));
                return;
            }

            WorkEntry entry = new WorkEntry(project, taskName, type, (double) hours);
            entry.setDate(date);
            entry.setComment(comment);

            if (taskService.addWorkEntry(entry)) {
                updateDayView(date);
                clearInputFields();

                Task newTask = new Task(project, taskName, type);
                taskService.addPredefinedTask(newTask);

                updateChoices();

                showAlert("Успех", "✅ Работа добавлена!");
            }

        } catch (Exception e) {
            showAlert("Ошибка", "❌ Произошла ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void editDayEntry() {
        WorkEntry selected = dayTableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        projectChoice.getEditor().setText(selected.getProject());
        taskChoice.getEditor().setText(selected.getTaskName());
        typeChoice.setValue(selected.getType());
        hoursField.setText(String.valueOf((int)selected.getHours()));
        commentArea.setText(selected.getComment());

        taskService.removeWorkEntry(selected);
        updateDayView(datePicker.getValue());

        showAlert("Информация", "✏️ Запись загружена для редактирования.");
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
            updateDayView(datePicker.getValue());
        }
    }

    private void clearDayEntries() {
        LocalDate date = datePicker.getValue();
        if (date == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение очистки");
        alert.setHeaderText("🗑️ Очистить все записи за " + date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + "?");
        alert.setContentText("Это действие нельзя отменить!");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            taskService.clearEntriesForDate(date);
            updateDayView(date);
        }
    }

    // ==================== НЕДЕЛЬНЫЙ РЕЖИМ ====================

    private void updateWeekView(LocalDate date) {
        LocalDate startOfWeek = date.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        weekDays.clear();

        for (int i = 0; i < 7; i++) {
            weekDays.add(startOfWeek.plusDays(i));
        }

        List<WorkEntry> weekEntriesList = taskService.getEntriesForDateRange(startOfWeek, endOfWeek);

        Map<String, WeekEntry> entryMap = new HashMap<>();
        for (WorkEntry entry : weekEntriesList) {
            String key = entry.getProject() + "|" + entry.getTaskName() + "|" + entry.getType();
            WeekEntry weekEntry = entryMap.get(key);
            if (weekEntry == null) {
                weekEntry = new WeekEntry(entry.getProject(), entry.getTaskName(), entry.getType());
                entryMap.put(key, weekEntry);
            }
            weekEntry.getDayHours().put(entry.getDate(), (int) entry.getHours());
            if (entry.getComment() != null && !entry.getComment().isEmpty()) {
                weekEntry.setComment(entry.getComment());
            }
        }

        weekEntries.setAll(new ArrayList<>(entryMap.values()));

        updateWeekColumnHeaders(startOfWeek);
        updateWeekTotals();
        weekTableView.refresh();
    }

    private void updateWeekColumnHeaders(LocalDate startOfWeek) {
        int colIndex = 3; // после Проект, Задача, Тип
        for (int i = 0; i < 7; i++) {
            LocalDate day = startOfWeek.plusDays(i);
            String dayName = getRussianDayOfWeek(day.getDayOfWeek().getValue());
            String dateStr = day.format(DateTimeFormatter.ofPattern("dd.MM"));
            String colName = dayName + " " + dateStr;

            if (colIndex < weekTableView.getColumns().size()) {
                weekTableView.getColumns().get(colIndex).setText(colName);
            }
            colIndex++;
        }
    }

    private void updateWeekTotals() {
        double weeklyTotal = 0;
        for (WeekEntry entry : weekEntries) {
            weeklyTotal += entry.getTotal();
        }

        // Теперь лимит 48 часов для всей недели (включая выходные, если они рабочие)
        String weeklyText = String.format("Неделя: %.1f / %d ч", weeklyTotal, MAX_WEEKLY_HOURS);
        if (weeklyTotal > MAX_WEEKLY_HOURS) {
            weekTotalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: red;");
        } else {
            weekTotalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2196F3;");
        }
        weekTotalLabel.setText(weeklyText);
    }

    private void deleteWeekEntry() {
        WeekEntry selected = weekTableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("🗑️ Удалить задачу за неделю?");
        alert.setContentText(String.format("%s - %s", selected.getProject(), selected.getTaskName()));

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            weekEntries.remove(selected);

            for (LocalDate date : weekDays) {
                List<WorkEntry> entries = taskService.getEntriesForDate(date);
                for (WorkEntry entry : entries) {
                    if (entry.getProject().equals(selected.getProject()) &&
                            entry.getTaskName().equals(selected.getTaskName()) &&
                            entry.getType().equals(selected.getType())) {
                        taskService.removeWorkEntry(entry);
                    }
                }
            }
            updateWeekTotals();
            weekTableView.refresh();
        }
    }

    private void saveWeekEntryToService(WeekEntry weekEntry) {
        // Удаляем старые записи
        for (LocalDate date : weekDays) {
            List<WorkEntry> entries = taskService.getEntriesForDate(date);
            List<WorkEntry> toRemove = new ArrayList<>();
            for (WorkEntry entry : entries) {
                if (entry.getProject().equals(weekEntry.getProject()) &&
                        entry.getTaskName().equals(weekEntry.getTaskName()) &&
                        entry.getType().equals(weekEntry.getType())) {
                    toRemove.add(entry);
                }
            }
            for (WorkEntry entry : toRemove) {
                taskService.removeWorkEntry(entry);
            }
        }

        // Добавляем новые записи
        for (Map.Entry<LocalDate, Integer> dayEntry : weekEntry.getDayHours().entrySet()) {
            int hours = dayEntry.getValue();
            if (hours > 0) {
                WorkEntry workEntry = new WorkEntry(
                        weekEntry.getProject(),
                        weekEntry.getTaskName(),
                        weekEntry.getType(),
                        (double) hours
                );
                workEntry.setDate(dayEntry.getKey());
                workEntry.setComment(weekEntry.getComment());
                taskService.addWorkEntry(workEntry);
            }
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private void updateTaskChoices() {
        String currentText = taskChoice.getEditor().getText();
        taskChoice.getItems().clear();
        List<String> tasks = taskService.getAllTaskNames();
        if (!tasks.isEmpty()) {
            taskChoice.getItems().addAll(tasks);
        }
        if (currentText != null && !currentText.isEmpty()) {
            taskChoice.getEditor().setText(currentText);
        }
        if (taskChoice.getItems().isEmpty()) {
            taskChoice.setPromptText("Введите или выберите задачу");
            taskChoice.getEditor().setPromptText("Введите название задачи");
        }
    }

    private void updateChoices() {
        String currentProject = projectChoice.getEditor().getText();
        projectChoice.getItems().clear();
        projectChoice.getItems().addAll(taskService.getAllProjects());
        if (!projectChoice.getItems().isEmpty() && currentProject.isEmpty()) {
            projectChoice.setValue(projectChoice.getItems().get(0));
        } else if (!currentProject.isEmpty()) {
            projectChoice.getEditor().setText(currentProject);
        }
        updateTaskChoices();
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

    private void loadTasksFromYaml() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите YAML файл с задачами");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("YAML файлы", "*.yaml", "*.yml")
        );

        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            try {
                List<Task> tasks = yamlLoaderService.loadTasksFromYaml(file.getAbsolutePath());
                if (!tasks.isEmpty()) {
                    for (Task task : tasks) {
                        taskService.addPredefinedTask(task);
                    }
                    updateChoices();
                    showAlert("Успех", String.format("✅ Загружено %d задач!", tasks.size()));
                } else {
                    showAlert("Внимание", "⚠️ Файл не содержит задач");
                }
            } catch (Exception e) {
                showAlert("Ошибка", "❌ Не удалось загрузить файл: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // ==================== МЕТОДЫ УПРАВЛЕНИЯ НАПОМИНАНИЯМИ ====================

    private void setupReminders() {
        updateRemindersList();

        if (remindersList.isEmpty()) {
            Reminder reminder = new Reminder(LocalTime.of(12, 0), "📋 Не забудьте заполнить отчет за первую половину дня!");
            reminder.setRepeatDaily(true);
            reminderService.addReminder(reminder);
            updateRemindersList();
        }

        reminderService.setOnReminderTrigger(() -> {
            updateRemindersList();
        });
    }

    private void updateRemindersList() {
        remindersList.setAll(reminderService.getReminders());
    }

    private void manageReminders() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("⏰ Управление напоминаниями");
        dialog.setHeaderText("Добавьте новое или управляйте существующими напоминаниями");
        dialog.getDialogPane().setPrefWidth(650);
        dialog.getDialogPane().setPrefHeight(600);

        ButtonType closeButtonType = new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButtonType);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        // --- Секция добавления ---
        TitledPane addPane = new TitledPane();
        addPane.setText("➕ Добавить новое напоминание");
        addPane.setExpanded(true);

        GridPane addGrid = new GridPane();
        addGrid.setHgap(10);
        addGrid.setVgap(10);
        addGrid.setPadding(new Insets(10));

        Label timeLabel = new Label("Время:");
        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, LocalTime.now().getHour());
        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, LocalTime.now().getMinute());
        HBox timeBox = new HBox(5, hourSpinner, new Label(":"), minuteSpinner);

        Label textLabel = new Label("Текст:");
        TextArea textArea = new TextArea();
        textArea.setPromptText("Введите текст напоминания...");
        textArea.setPrefRowCount(2);

        CheckBox repeatCheck = new CheckBox("Повторять ежедневно");

        Button addReminderButton = new Button("✅ Добавить");
        addReminderButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        addReminderButton.setOnAction(e -> {
            LocalTime time = LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue());
            String text = textArea.getText();
            if (text != null && !text.trim().isEmpty()) {
                Reminder reminder = new Reminder(time, text.trim(), repeatCheck.isSelected());
                reminderService.addReminder(reminder);
                updateRemindersList();
                textArea.clear();
                hourSpinner.getValueFactory().setValue(LocalTime.now().getHour());
                minuteSpinner.getValueFactory().setValue(LocalTime.now().getMinute());
                repeatCheck.setSelected(false);
                showAlert("Успех", "✅ Напоминание добавлено!");
            } else {
                showAlert("Ошибка", "⚠️ Введите текст напоминания");
            }
        });

        addGrid.addRow(0, timeLabel, timeBox);
        addGrid.addRow(1, textLabel, textArea);
        addGrid.addRow(2, new Label(""), repeatCheck);
        addGrid.addRow(3, new Label(""), addReminderButton);
        addPane.setContent(addGrid);

        // --- Секция управления существующими ---
        TitledPane managePane = new TitledPane();
        managePane.setText("📋 Список напоминаний");
        managePane.setExpanded(true);

        TableView<Reminder> reminderTable = new TableView<>();
        reminderTable.setItems(remindersList);
        reminderTable.setPrefHeight(200);

        TableColumn<Reminder, String> timeCol = new TableColumn<>("Время");
        timeCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFormattedTime()));
        timeCol.setPrefWidth(80);

        TableColumn<Reminder, String> textCol = new TableColumn<>("Текст");
        textCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getText()));
        textCol.setPrefWidth(250);

        TableColumn<Reminder, Boolean> activeCol = new TableColumn<>("Активно");
        activeCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleBooleanProperty(cellData.getValue().isActive()));
        activeCol.setPrefWidth(80);

        TableColumn<Reminder, Boolean> repeatCol = new TableColumn<>("Ежедневно");
        repeatCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleBooleanProperty(cellData.getValue().isRepeatDaily()));
        repeatCol.setPrefWidth(90);

        reminderTable.getColumns().addAll(timeCol, textCol, activeCol, repeatCol);

        HBox manageButtons = new HBox(10);
        manageButtons.setAlignment(javafx.geometry.Pos.CENTER);

        Button editButton = new Button("✏️ Редактировать");
        editButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        editButton.setOnAction(e -> {
            Reminder selected = reminderTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                editReminder(selected, reminderTable);
            } else {
                showAlert("Внимание", "⚠️ Пожалуйста, выберите напоминание для редактирования");
            }
        });

        Button deleteButton = new Button("🗑️ Удалить");
        deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        deleteButton.setOnAction(e -> {
            Reminder selected = reminderTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Подтверждение удаления");
                alert.setHeaderText("🗑️ Удалить напоминание?");
                alert.setContentText(String.format("Время: %s\nТекст: %s",
                        selected.getFormattedTime(), selected.getText()));

                if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    reminderService.removeReminder(selected);
                    updateRemindersList();
                }
            } else {
                showAlert("Внимание", "⚠️ Пожалуйста, выберите напоминание для удаления");
            }
        });

        Button toggleButton = new Button("🔄 Переключить активность");
        toggleButton.setOnAction(e -> {
            Reminder selected = reminderTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.setActive(!selected.isActive());
                reminderTable.refresh();
                updateRemindersList();
            } else {
                showAlert("Внимание", "⚠️ Пожалуйста, выберите напоминание");
            }
        });

        manageButtons.getChildren().addAll(editButton, deleteButton, toggleButton);

        VBox manageBox = new VBox(10);
        manageBox.getChildren().addAll(reminderTable, manageButtons);
        managePane.setContent(manageBox);

        updateRemindersList();

        vbox.getChildren().addAll(addPane, managePane);
        dialog.getDialogPane().setContent(vbox);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == closeButtonType) {
                dialog.close();
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void editReminder(Reminder reminder, TableView<Reminder> reminderTable) {
        Dialog<Reminder> dialog = new Dialog<>();
        dialog.setTitle("✏️ Редактирование напоминания");
        dialog.setHeaderText("Измените данные напоминания");

        ButtonType saveButtonType = new ButtonType("💾 Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        Label timeLabel = new Label("Время:");
        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, reminder.getTime().getHour());
        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, reminder.getTime().getMinute());
        HBox timeBox = new HBox(5, hourSpinner, new Label(":"), minuteSpinner);

        Label textLabel = new Label("Текст:");
        TextArea textArea = new TextArea(reminder.getText());
        textArea.setPrefRowCount(3);
        textArea.setPromptText("Введите текст напоминания...");

        CheckBox repeatCheck = new CheckBox("Повторять ежедневно");
        repeatCheck.setSelected(reminder.isRepeatDaily());

        CheckBox activeCheck = new CheckBox("Активно");
        activeCheck.setSelected(reminder.isActive());

        grid.addRow(0, timeLabel, timeBox);
        grid.addRow(1, textLabel, textArea);
        grid.addRow(2, new Label(""), repeatCheck);
        grid.addRow(3, new Label(""), activeCheck);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String text = textArea.getText();
                if (text != null && !text.trim().isEmpty()) {
                    reminder.setTime(LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue()));
                    reminder.setText(text.trim());
                    reminder.setRepeatDaily(repeatCheck.isSelected());
                    reminder.setActive(activeCheck.isSelected());

                    reminderTable.refresh();
                    updateRemindersList();

                    showAlert("Успех", "✅ Напоминание обновлено!");
                    return reminder;
                } else {
                    showAlert("Ошибка", "⚠️ Введите текст напоминания");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    // ==================== МЕТОДЫ ЭКСПОРТА ====================

    private void showExportDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("📊 Экспорт данных");
        dialog.setHeaderText("Выберите период и формат для экспорта");
        dialog.getDialogPane().setPrefWidth(400);

        ButtonType exportButton = new ButtonType("Экспорт", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(exportButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        DatePicker startDate = new DatePicker(datePicker.getValue());
        DatePicker endDate = new DatePicker(datePicker.getValue());

        if (isWeekView) {
            startDate.setValue(datePicker.getValue().with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1));
            endDate.setValue(startDate.getValue().plusDays(6));
        }

        ComboBox<String> formatChoice = new ComboBox<>();
        formatChoice.getItems().addAll("Excel (.xlsx)", "JSON (.json)", "YAML (.yaml)", "XML (.xml)");
        formatChoice.setValue("Excel (.xlsx)");

        grid.addRow(0, new Label("Начало:"), startDate);
        grid.addRow(1, new Label("Конец:"), endDate);
        grid.addRow(2, new Label("Формат:"), formatChoice);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == exportButton) {
                LocalDate start = startDate.getValue();
                LocalDate end = endDate.getValue();
                if (start != null && end != null && !start.isAfter(end)) {
                    List<WorkEntry> entries = taskService.getEntriesForDateRange(start, end);
                    if (entries.isEmpty()) {
                        showAlert("Внимание", "⚠️ Нет данных за выбранный период");
                        return null;
                    }

                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Сохранить файл");

                    String format = formatChoice.getValue();
                    String dateRange = String.format("%s_%s",
                            start.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                            end.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    );
                    String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

                    if (format.startsWith("Excel")) {
                        fileChooser.getExtensionFilters().add(
                                new FileChooser.ExtensionFilter("Excel файлы", "*.xlsx")
                        );
                        fileChooser.setInitialFileName(String.format("work_report_%s_%s.xlsx", timestamp, dateRange));
                    } else if (format.startsWith("JSON")) {
                        fileChooser.getExtensionFilters().add(
                                new FileChooser.ExtensionFilter("JSON файлы", "*.json")
                        );
                        fileChooser.setInitialFileName(String.format("work_report_%s_%s.json", timestamp, dateRange));
                    } else if (format.startsWith("YAML")) {
                        fileChooser.getExtensionFilters().add(
                                new FileChooser.ExtensionFilter("YAML файлы", "*.yaml")
                        );
                        fileChooser.setInitialFileName(String.format("work_report_%s_%s.yaml", timestamp, dateRange));
                    } else {
                        fileChooser.getExtensionFilters().add(
                                new FileChooser.ExtensionFilter("XML файлы", "*.xml")
                        );
                        fileChooser.setInitialFileName(String.format("work_report_%s_%s.xml", timestamp, dateRange));
                    }

                    File file = fileChooser.showSaveDialog(root.getScene().getWindow());
                    if (file != null) {
                        try {
                            String filePath = file.getAbsolutePath();
                            if (format.startsWith("Excel")) {
                                if (!filePath.endsWith(".xlsx")) filePath += ".xlsx";
                                exportService.exportToExcel(entries, filePath);
                            } else if (format.startsWith("JSON")) {
                                if (!filePath.endsWith(".json")) filePath += ".json";
                                exportService.exportToJson(entries, filePath);
                            } else if (format.startsWith("YAML")) {
                                if (!filePath.endsWith(".yaml")) filePath += ".yaml";
                                exportService.exportToYaml(entries, filePath);
                            } else {
                                if (!filePath.endsWith(".xml")) filePath += ".xml";
                                exportService.exportToXml(entries, filePath);
                            }
                            showAlert("Успех", String.format("✅ Данные экспортированы!\n%d записей", entries.size()));
                        } catch (Exception e) {
                            showAlert("Ошибка", "❌ Ошибка экспорта: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                } else {
                    showAlert("Ошибка", "⚠️ Некорректный диапазон дат");
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    // ==================== МЕТОДЫ ПРАЗДНИКОВ ====================

    private boolean isHoliday(LocalDate date) {
        return holidays.contains(date);
    }

    private void setupHolidays() {
        holidays.add(LocalDate.of(2026, 1, 1));
        holidays.add(LocalDate.of(2026, 1, 7));
        holidays.add(LocalDate.of(2026, 2, 23));
        holidays.add(LocalDate.of(2026, 3, 8));
        holidays.add(LocalDate.of(2026, 5, 1));
        holidays.add(LocalDate.of(2026, 5, 9));
        holidays.add(LocalDate.of(2026, 6, 12));
        holidays.add(LocalDate.of(2026, 11, 4));
    }

    private void manageHolidays() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("📅 Управление праздничными днями");
        dialog.setHeaderText("Добавьте или удалите праздничные дни");
        dialog.getDialogPane().setPrefWidth(400);
        dialog.getDialogPane().setPrefHeight(350);

        ButtonType closeButtonType = new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButtonType);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        ListView<String> holidayList = new ListView<>();
        ObservableList<String> holidayItems = FXCollections.observableArrayList();
        for (LocalDate date : holidays) {
            holidayItems.add(date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        }
        holidayItems.sort((d1, d2) -> {
            try {
                LocalDate date1 = LocalDate.parse(d1, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                LocalDate date2 = LocalDate.parse(d2, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                return date1.compareTo(date2);
            } catch (Exception e) {
                return d1.compareTo(d2);
            }
        });
        holidayList.setItems(holidayItems);

        HBox addBox = new HBox(10);
        addBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        DatePicker holidayPicker = new DatePicker(LocalDate.now());
        Button addHolidayButton = new Button("➕ Добавить");
        addHolidayButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        addHolidayButton.setOnAction(e -> {
            LocalDate date = holidayPicker.getValue();
            if (date != null && !holidays.contains(date)) {
                holidays.add(date);
                String dateStr = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                holidayItems.add(dateStr);
                holidayItems.sort((d1, d2) -> {
                    try {
                        LocalDate date1 = LocalDate.parse(d1, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                        LocalDate date2 = LocalDate.parse(d2, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                        return date1.compareTo(date2);
                    } catch (Exception ex) {
                        return d1.compareTo(d2);
                    }
                });
                showAlert("Успех", "✅ Праздничный день добавлен: " + dateStr);
            } else if (date != null && holidays.contains(date)) {
                showAlert("Внимание", "⚠️ Этот день уже добавлен как праздничный");
            } else {
                showAlert("Ошибка", "⚠️ Пожалуйста, выберите дату");
            }
        });
        addBox.getChildren().addAll(holidayPicker, addHolidayButton);

        Button deleteHolidayButton = new Button("🗑️ Удалить выбранный");
        deleteHolidayButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        deleteHolidayButton.setMaxWidth(Double.MAX_VALUE);
        deleteHolidayButton.setOnAction(e -> {
            String selected = holidayList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    LocalDate date = LocalDate.parse(selected, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                    holidays.remove(date);
                    holidayItems.remove(selected);
                    showAlert("Успех", "✅ Праздничный день удален: " + selected);
                } catch (Exception ex) {
                    showAlert("Ошибка", "⚠️ Не удалось удалить выбранный день");
                }
            } else {
                showAlert("Внимание", "⚠️ Пожалуйста, выберите день для удаления");
            }
        });

        Label countLabel = new Label("Праздничные дни (всего: " + holidayItems.size() + "):");
        holidayItems.addListener((javafx.collections.ListChangeListener<String>) change -> {
            countLabel.setText("Праздничные дни (всего: " + holidayItems.size() + "):");
        });

        vbox.getChildren().addAll(
                countLabel,
                holidayList,
                new Separator(),
                new Label("Добавить праздничный день:"),
                addBox,
                deleteHolidayButton
        );

        dialog.getDialogPane().setContent(vbox);

        dialog.setOnCloseRequest(e -> {});
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == closeButtonType) {
                dialog.close();
            }
            return null;
        });

        dialog.showAndWait();
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public BorderPane getRoot() {
        return root;
    }

    public void shutdown() {
        if (reminderService != null) {
            reminderService.shutdown();
        }
    }

        private void editWeekEntry() {
            WeekEntry selected = weekTableView.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            // Загружаем данные в форму
            projectChoice.getEditor().setText(selected.getProject());
            taskChoice.getEditor().setText(selected.getTaskName());
            typeChoice.setValue(selected.getType());
            commentArea.setText(selected.getComment());

            // Удаляем из таблицы
            weekEntries.remove(selected);

            // Удаляем записи из сервиса
            for (LocalDate date : weekDays) {
                List<WorkEntry> entries = taskService.getEntriesForDate(date);
                for (WorkEntry entry : entries) {
                    if (entry.getProject().equals(selected.getProject()) &&
                            entry.getTaskName().equals(selected.getTaskName()) &&
                            entry.getType().equals(selected.getType())) {
                        taskService.removeWorkEntry(entry);
                    }
                }
            }

            updateWeekTotals();
            weekTableView.refresh();

            // Переключаемся на дневной режим для удобства редактирования
            isWeekView = false;
            // Находим и переключаем радиокнопку "День"
            for (javafx.scene.Node node : ((VBox) root.getTop()).getChildren()) {
                if (node instanceof HBox) {
                    for (javafx.scene.Node child : ((HBox) node).getChildren()) {
                        if (child instanceof RadioButton && ((RadioButton) child).getText().equals("📅 День")) {
                            ((RadioButton) child).setSelected(true);
                            break;
                        }
                    }
                }
            }
            showDayView();

            showAlert("Информация", "✏️ Задача загружена для редактирования в дневном режиме.");
        }

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

            // Проверяем, есть ли уже такая задача
            for (WeekEntry entry : weekEntries) {
                if (entry.getProject().equals(project) &&
                        entry.getTaskName().equals(taskName) &&
                        entry.getType().equals(type)) {
                    showAlert("Внимание", "⚠️ Такая задача уже существует в этой неделе");
                    return;
                }
            }

            WeekEntry newEntry = new WeekEntry(project, taskName, type);
            newEntry.setComment(comment);

            // Инициализируем все дни нулями
            for (LocalDate day : weekDays) {
                newEntry.getDayHours().put(day, 0);
            }

            weekEntries.add(newEntry);

            // Добавляем в предопределенные задачи
            Task newTask = new Task(project, taskName, type);
            taskService.addPredefinedTask(newTask);

            updateChoices();
            clearInputFields();
            updateWeekTotals();
            weekTableView.refresh();

            showAlert("Успех", "✅ Задача добавлена! Введите часы в таблице (двойной клик).");

        } catch (Exception e) {
            showAlert("Ошибка", "❌ Произошла ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void clearWeekEntries() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение очистки");
        alert.setHeaderText("🗑️ Очистить все записи за неделю?");
        alert.setContentText("Это действие нельзя отменить!");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            for (LocalDate date : weekDays) {
                taskService.clearEntriesForDate(date);
            }
            weekEntries.clear();
            updateWeekTotals();
            weekTableView.refresh();
        }
    }

    private String getRussianDayOfWeek(int dayOfWeekValue) {
        switch (dayOfWeekValue) {
            case 1: return "Пн";
            case 2: return "Вт";
            case 3: return "Ср";
            case 4: return "Чт";
            case 5: return "Пт";
            case 6: return "Сб";
            case 7: return "Вс";
            default: return "";
        }
    }

}