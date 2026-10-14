package com.reminder.ui.view;

import com.reminder.model.Task;
import com.reminder.model.WorkEntry;
import com.reminder.service.TaskService;
import com.reminder.service.WorkTimeService;
import com.reminder.ui.component.EntryForm;
import com.reminder.ui.util.Dialogs;
import com.reminder.ui.util.TableCells;
import com.reminder.util.DateUtils;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.util.List;

/** Дневной режим: таблица записей, итоги дня и переработка. */
public class DayView {

    private final TableView<WorkEntry> table;
    private final TableColumn<WorkEntry, String> projectColumn;
    private final TableColumn<WorkEntry, String> taskColumn;
    private final TableColumn<WorkEntry, String> typeColumn;
    private final TableColumn<WorkEntry, Integer> hoursColumn;
    private final TableColumn<WorkEntry, String> commentColumn;
    private final Label totalLabel;
    private final Label remainingLabel;
    private final Label overtimeLabel;
    private final EntryForm form;
    private final TaskService taskService;
    private final WorkTimeService workTimeService;
    private final DatePicker datePicker;

    private final ObservableList<WorkEntry> entries = FXCollections.observableArrayList();

    public DayView(TableView<WorkEntry> table,
                   TableColumn<WorkEntry, String> projectColumn,
                   TableColumn<WorkEntry, String> taskColumn,
                   TableColumn<WorkEntry, String> typeColumn,
                   TableColumn<WorkEntry, Integer> hoursColumn,
                   TableColumn<WorkEntry, String> commentColumn,
                   Label totalLabel,
                   Label remainingLabel,
                   Label overtimeLabel,
                   EntryForm form,
                   TaskService taskService,
                   WorkTimeService workTimeService,
                   DatePicker datePicker) {
        this.table = table;
        this.projectColumn = projectColumn;
        this.taskColumn = taskColumn;
        this.typeColumn = typeColumn;
        this.hoursColumn = hoursColumn;
        this.commentColumn = commentColumn;
        this.totalLabel = totalLabel;
        this.remainingLabel = remainingLabel;
        this.overtimeLabel = overtimeLabel;
        this.form = form;
        this.taskService = taskService;
        this.workTimeService = workTimeService;
        this.datePicker = datePicker;
    }

    /** Настраивает таблицу, колонки и контекстное меню. */
    public void configure() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(entries);

        projectColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getProject()));
        projectColumn.setCellFactory(c -> TableCells.wrapping());
        taskColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getTaskName()));
        taskColumn.setCellFactory(c -> TableCells.wrapping());
        typeColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getType()));
        typeColumn.setCellFactory(c -> TableCells.wrapping());
        hoursColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>((int) cd.getValue().getHours()));
        hoursColumn.setStyle("-fx-alignment: CENTER;");
        commentColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getComment()));
        commentColumn.setCellFactory(c -> TableCells.wrapping());

        ContextMenu menu = new ContextMenu();
        MenuItem editItem = new MenuItem("✏️ Редактировать");
        editItem.setOnAction(e -> edit());
        MenuItem deleteItem = new MenuItem("🗑️ Удалить");
        deleteItem.setOnAction(e -> delete());
        menu.getItems().addAll(editItem, deleteItem);
        table.setContextMenu(menu);
    }

    /** Обновляет таблицу и итоги за день. */
    public void update(LocalDate date) {
        List<WorkEntry> dayEntries = taskService.getEntriesForDate(date);
        entries.setAll(dayEntries);

        double total = workTimeService.getDayTotal(dayEntries, date);
        double remaining = workTimeService.getRemainingForDay(dayEntries, date);

        String bold = "-fx-font-weight: bold; -fx-font-size: 14px;";
        totalLabel.setText(String.format("Загружено: %.1f ч", total));
        if (remaining < 0) {
            remainingLabel.setText("Осталось: 0,0 ч");
            remainingLabel.setStyle(bold + "-fx-text-fill: orange;");
            overtimeLabel.setText(String.format("Переработка: %.1f ч", -remaining));
            overtimeLabel.setStyle(bold + "-fx-text-fill: red;");
        } else {
            remainingLabel.setText(String.format("Осталось: %.1f ч", remaining));
            remainingLabel.setStyle(bold + "-fx-text-fill: " + (remaining < 1 ? "orange" : "green") + ";");
            overtimeLabel.setText("");
            overtimeLabel.setStyle("");
        }
    }

    /** Добавляет дневную запись из полей формы. */
    public void add() {
        try {
            String project = form.project();
            String taskName = form.taskName();
            String type = form.type();
            String hoursText = form.hoursText();
            String comment = form.comment();

            if (project.isEmpty()) {
                Dialogs.error("Ошибка", "⚠️ Пожалуйста, укажите проект");
                form.focusProject();
                return;
            }
            if (taskName.isEmpty()) {
                Dialogs.error("Ошибка", "⚠️ Пожалуйста, укажите задачу");
                form.focusTask();
                return;
            }
            if (type == null || type.isEmpty()) {
                Dialogs.error("Ошибка", "⚠️ Пожалуйста, выберите тип работы");
                form.focusType();
                return;
            }
            if (hoursText.isEmpty()) {
                Dialogs.error("Ошибка", "⚠️ Пожалуйста, укажите длительность");
                form.focusHours();
                return;
            }

            int hours;
            try {
                hours = Integer.parseInt(hoursText);
            } catch (NumberFormatException e) {
                Dialogs.error("Ошибка", "⚠️ Длительность должна быть целым числом");
                form.focusHours();
                return;
            }
            if (hours <= 0 || hours > 24) {
                Dialogs.error("Ошибка", "⚠️ Длительность должна быть от 1 до 24 часов");
                form.focusHours();
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
                form.refreshChoices(taskService);
                form.clear();
                update(date);
                Dialogs.info("Успех", "✅ Работа добавлена!");
            }
        } catch (Exception e) {
            Dialogs.error("Ошибка", "❌ Произошла ошибка: " + e.getMessage());
        }
    }

    /** Редактирует выбранную запись. */
    public void edit() {
        WorkEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        com.reminder.ui.dialog.EntryEditDialog.EntryEditResult result =
                com.reminder.ui.dialog.EntryEditDialog.show(
                        "✏️ Редактировать запись", "Измените данные записи",
                        form.typeItems(),
                        selected.getProject(), selected.getTaskName(), selected.getType(),
                        String.valueOf((int) selected.getHours()), selected.getComment(), true);
        if (result == null) return;

        int hours;
        try {
            hours = Integer.parseInt(result.hours());
        } catch (NumberFormatException e) {
            Dialogs.error("Ошибка", "⚠️ Длительность должна быть целым числом");
            return;
        }
        if (hours <= 0 || hours > 24) {
            Dialogs.error("Ошибка", "⚠️ Длительность должна быть от 1 до 24 часов");
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
            form.refreshChoices(taskService);
            update(selected.getDate());
            Dialogs.info("Успех", "✅ Запись обновлена!");
        } else {
            Dialogs.error("Ошибка", "⚠️ Не удалось сохранить");
        }
    }

    /** Удаляет выбранную запись. */
    public void delete() {
        WorkEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (Dialogs.confirm("Подтверждение удаления", "🗑️ Удалить запись?",
                String.format("%s - %s (%.0f ч)",
                        selected.getProject(), selected.getTaskName(), selected.getHours()))) {
            taskService.removeWorkEntry(selected);
            update(selected.getDate());
        }
    }

    /** Очищает все записи текущего дня. */
    public void clear() {
        LocalDate date = datePicker.getValue();
        if (date == null) return;

        if (Dialogs.confirm("Подтверждение очистки",
                "🗑️ Очистить все записи за " + DateUtils.formatDate(date) + "?",
                "Это действие нельзя отменить!")) {
            taskService.clearEntriesForDate(date);
            update(date);
        }
    }
}