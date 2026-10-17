package com.reminder.ui.view;

import com.reminder.i18n.I18n;
import com.reminder.model.Task;
import com.reminder.model.WeekEntry;
import com.reminder.model.WorkEntry;
import com.reminder.service.TaskService;
import com.reminder.service.WorkTimeService;
import com.reminder.ui.component.EntryForm;
import com.reminder.ui.component.WeekEntryCell;
import com.reminder.ui.component.WeekTotalCell;
import com.reminder.ui.util.Dialogs;
import com.reminder.ui.util.TableCells;
import com.reminder.util.DateUtils;
import com.reminder.util.RussianDays;
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
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Недельный режим: таблица задач, правка часов по дням, подсветка переработок. */
public class WeekView {

    private final TableView<WeekEntry> table;
    private final TableColumn<WeekEntry, String> projectColumn;
    private final TableColumn<WeekEntry, String> taskColumn;
    private final TableColumn<WeekEntry, String> typeColumn;
    private final TableColumn<WeekEntry, String> commentColumn;
    private final TableColumn<WeekEntry, Integer> totalColumn;
    private final Label totalLabel;
    private final EntryForm form;
    private final TaskService taskService;
    private final WorkTimeService workTimeService;
    private final DatePicker datePicker;

    private final ObservableList<WeekEntry> entries = FXCollections.observableArrayList();
    private final List<LocalDate> weekDays = new ArrayList<>();
    private final List<WeekDayColumn> dayColumns = new ArrayList<>();
    private boolean editInProgress = false;

    public WeekView(TableView<WeekEntry> table,
                    TableColumn<WeekEntry, String> projectColumn,
                    TableColumn<WeekEntry, String> taskColumn,
                    TableColumn<WeekEntry, String> typeColumn,
                    TableColumn<WeekEntry, String> commentColumn,
                    TableColumn<WeekEntry, Integer> totalColumn,
                    Label totalLabel,
                    EntryForm form,
                    TaskService taskService,
                    WorkTimeService workTimeService,
                    DatePicker datePicker) {
        this.table = table;
        this.projectColumn = projectColumn;
        this.taskColumn = taskColumn;
        this.typeColumn = typeColumn;
        this.commentColumn = commentColumn;
        this.totalColumn = totalColumn;
        this.totalLabel = totalLabel;
        this.form = form;
        this.taskService = taskService;
        this.workTimeService = workTimeService;
        this.datePicker = datePicker;
    }

    /** Настраивает таблицу, колонки, контекстное меню и колонки дней. */
    public void configure() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(entries);

        projectColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getProject()));
        projectColumn.setCellFactory(c -> TableCells.wrapping());
        taskColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getTaskName()));
        taskColumn.setCellFactory(c -> TableCells.wrapping());
        typeColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getType()));
        typeColumn.setCellFactory(c -> TableCells.wrapping());
        commentColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getComment()));
        commentColumn.setCellFactory(c -> TableCells.wrapping());
        totalColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getTotal()));
        totalColumn.setCellFactory(c -> new WeekTotalCell());

        ContextMenu menu = new ContextMenu();
        MenuItem editItem = new MenuItem(I18n.get("entry.editTask"));
        editItem.setOnAction(e -> edit());
        MenuItem deleteItem = new MenuItem(I18n.get("entry.deleteTask"));
        deleteItem.setOnAction(e -> delete());
        menu.getItems().addAll(editItem, deleteItem);
        table.setContextMenu(menu);

        createDayColumns();
    }

    private void createDayColumns() {
        LocalDate start = DateUtils.startOfWeek(datePicker.getValue());
        for (int i = 0; i < 7; i++) {
            WeekDayColumn col = new WeekDayColumn(start.plusDays(i));
            col.setCellValueFactory(cd ->
                    new ReadOnlyObjectWrapper<>(cd.getValue().getDayHours().getOrDefault(col.date(), 0)));
            col.setPrefWidth(70);
            col.setStyle("-fx-alignment: CENTER;");
            col.setEditable(true);
            col.setCellFactory(cf -> new WeekEntryCell());
            col.setOnEditCommit(this::handleCellEdit);
            dayColumns.add(col);
        }
        table.getColumns().addAll(3, dayColumns);
    }

    /** Пересчитывает недельное представление для даты. */
    public void update(LocalDate date) {
        LocalDate startOfWeek = DateUtils.startOfWeek(date);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        weekDays.clear();
        for (int i = 0; i < 7; i++) {
            weekDays.add(startOfWeek.plusDays(i));
        }

        for (int i = 0; i < 7; i++) {
            WeekDayColumn col = dayColumns.get(i);
            LocalDate day = weekDays.get(i);
            col.setDay(day);
            col.setHeaderText(RussianDays.shortName(day.getDayOfWeek().getValue()) + " " + DateUtils.formatShort(day));
            col.setEditable(!workTimeService.isHoliday(day));
        }

        List<WorkEntry> rangeEntries = taskService.getEntriesForDateRange(startOfWeek, endOfWeek);
        entries.setAll(workTimeService.aggregateWeek(rangeEntries));
        highlightOvertimeDays();
        updateTotals();
        table.refresh();
    }

    /** Добавляет недельную задачу из полей формы. */
    public void add() {
        try {
            String project = form.project();
            String taskName = form.taskName();
            String type = form.type();
            String comment = form.comment();

            if (project.isEmpty()) {
                Dialogs.error(I18n.get("common.error"), I18n.get("error.project"));
                form.focusProject();
                return;
            }
            if (taskName.isEmpty()) {
                Dialogs.error(I18n.get("common.error"), I18n.get("error.task"));
                form.focusTask();
                return;
            }
            if (type == null || type.isEmpty()) {
                Dialogs.error(I18n.get("common.error"), I18n.get("error.type"));
                form.focusType();
                return;
            }

            for (WeekEntry entry : entries) {
                if (entry.getProject().equals(project)
                        && entry.getTaskName().equals(taskName)
                        && entry.getType().equals(type)) {
                    Dialogs.warn(I18n.get("common.warning"), I18n.get("week.exists"));
                    return;
                }
            }

            WeekEntry newEntry = new WeekEntry(project, taskName, type);
            newEntry.setComment(comment);
            for (LocalDate day : weekDays) {
                newEntry.getDayHours().put(day, 0);
            }

            entries.add(newEntry);
            taskService.addPredefinedTask(new Task(project, taskName, type));

            form.refreshChoices(taskService);
            form.clear();
            updateTotals();
            table.refresh();
            Dialogs.info(I18n.get("common.success"), I18n.get("success.taskAddedWeek"));
        } catch (Exception e) {
            Dialogs.error(I18n.get("common.error"), I18n.format("error.generic", e.getMessage()));
        }
    }

    /** Редактирует выбранную недельную задачу. */
    public void edit() {
        WeekEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        com.reminder.ui.dialog.EntryEditDialog.EntryEditResult result =
                com.reminder.ui.dialog.EntryEditDialog.show(
                        I18n.get("entry.editTaskTitle"), I18n.get("entry.editTaskHeader"),
                        form.typeItems(),
                        selected.getProject(), selected.getTaskName(), selected.getType(),
                        null, selected.getComment(), false);
        if (result == null) return;

        WeekEntry updated = new WeekEntry(result.project(), result.taskName(), result.type());
        updated.setComment(result.comment());
        updated.getDayHours().putAll(selected.getDayHours());

        int idx = entries.indexOf(selected);
        entries.set(idx, updated);
        saveEntryToService(updated, selected);
        taskService.addPredefinedTask(new Task(result.project(), result.taskName(), result.type()));

        form.refreshChoices(taskService);
        updateTotals();
        highlightOvertimeDays();
        table.refresh();
        Dialogs.info(I18n.get("common.success"), I18n.get("success.taskUpdated"));
    }

    /** Удаляет выбранную недельную задачу. */
    public void delete() {
        WeekEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (Dialogs.confirm(I18n.get("confirm.delete.title"), I18n.get("confirm.delete.taskWeek"),
                I18n.format("confirm.delete.taskWeekContent", selected.getProject(), selected.getTaskName()))) {
            removeRecordsFor(selected);
            entries.remove(selected);
            updateTotals();
            table.refresh();
        }
    }

    /** Очищает все записи текущей недели. */
    public void clear() {
        if (Dialogs.confirm(I18n.get("confirm.clear.title"),
                I18n.get("confirm.clear.week"),
                I18n.get("confirm.clear.content"))) {
            taskService.clearEntriesForDates(weekDays);
            entries.clear();
            updateTotals();
            table.refresh();
        }
    }

    private void handleCellEdit(TableColumn.CellEditEvent<WeekEntry, Integer> event) {
        if (editInProgress) {
            table.refresh();
            return;
        }
        editInProgress = true;
        try {
            processCellEdit(event);
        } finally {
            editInProgress = false;
        }
    }

    private void processCellEdit(TableColumn.CellEditEvent<WeekEntry, Integer> event) {
        WeekDayColumn col = (WeekDayColumn) event.getTableColumn();
        WeekEntry entry = event.getRowValue();
        LocalDate day = col.date();
        Integer newValue = event.getNewValue();
        if (newValue == null || newValue < 0) {
            table.refresh();
            return;
        }

        int oldValue = entry.getDayHours().getOrDefault(day, 0);
        int currentDayTotal = entries.stream()
                .mapToInt(we -> we.getDayHours().getOrDefault(day, 0))
                .sum();

        if (!workTimeService.dayTotalWithinLimit(entries, day, newValue, oldValue)) {
            int busy = currentDayTotal - oldValue;
            int overtime = busy + newValue - (int) WorkTimeService.MAX_DAILY_HOURS;
            boolean confirmed = Dialogs.yesNo(I18n.get("overtime.title"),
                    I18n.format("overtime.limit", I18n.number(WorkTimeService.MAX_DAILY_HOURS, 0)),
                    I18n.format("overtime.register", overtime));
            if (!confirmed) {
                table.refresh();
                return;
            }
        }

        entry.getDayHours().put(day, newValue);
        saveEntryToService(entry, entry);
        updateTotals();
        highlightOvertimeDays();
        table.refresh();
    }

    private void updateTotals() {
        double weeklyTotal = workTimeService.getWeekTotal(entries);
        String text = I18n.format("main.weekTotal", I18n.number(weeklyTotal, 1),
                I18n.number(WorkTimeService.MAX_WEEKLY_HOURS, 0));
        if (weeklyTotal > WorkTimeService.MAX_WEEKLY_HOURS) {
            totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: red;");
        } else {
            totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2196F3;");
        }
        totalLabel.setText(text);
    }

    /** Подсвечивает красным заголовок дня, если сумма часов за день превышает лимит. */
    private void highlightOvertimeDays() {
        for (WeekDayColumn col : dayColumns) {
            LocalDate day = col.date();
            int dayTotal = entries.stream()
                    .mapToInt(we -> we.getDayHours().getOrDefault(day, 0))
                    .sum();
            if (dayTotal > WorkTimeService.MAX_DAILY_HOURS) {
                col.setStyle("-fx-alignment: CENTER;");
                col.setHeaderStyle("-fx-font-weight: bold; -fx-text-fill: red;");
                col.setHoliday(false);
                col.setOvertimeTooltip(true);
            } else if (workTimeService.isHoliday(day)) {
                col.setStyle("-fx-alignment: CENTER;");
                col.setHeaderStyle("");
                col.setHoliday(true);
                col.setOvertimeTooltip(false);
            } else {
                col.setStyle("-fx-alignment: CENTER;");
                col.setHeaderStyle("");
                col.setHoliday(false);
                col.setOvertimeTooltip(false);
            }
        }
    }

    /** Сохраняет недельную задачу в записи: удаляет старые (previous) и пишет новые. */
    private void saveEntryToService(WeekEntry entry, WeekEntry previous) {
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
            List<WorkEntry> dayEntries = taskService.getEntriesForDate(date);
            List<WorkEntry> toRemove = dayEntries.stream()
                    .filter(e -> e.getProject().equals(weekEntry.getProject())
                            && e.getTaskName().equals(weekEntry.getTaskName())
                            && e.getType().equals(weekEntry.getType()))
                    .toList();
            toRemove.forEach(taskService::removeWorkEntry);
        }
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

        /** Помечает день как праздничный (фон/цвет — из темы). */
        void setHoliday(boolean holiday) {
            getStyleClass().remove("holiday-column");
            headerLabel.getStyleClass().remove("holiday-header");
            if (holiday) {
                getStyleClass().add("holiday-column");
                headerLabel.getStyleClass().add("holiday-header");
            }
        }

        /** Показывает/скрывает тултип о превышении лимита на заголовке дня. */
        void setOvertimeTooltip(boolean show) {
            if (show && dayTooltip == null) {
                dayTooltip = new Tooltip(I18n.get("week.overtimeTooltip"));
                dayTooltip.setShowDelay(Duration.millis(250));
                dayTooltip.setShowDuration(Duration.seconds(5));
                dayTooltip.setHideDelay(Duration.millis(100));
                Tooltip.install(headerLabel, dayTooltip);
            } else if (!show) {
                if (dayTooltip != null) {
                    Tooltip.uninstall(headerLabel, dayTooltip);
                    dayTooltip = null;
                }
            }
        }
    }
}