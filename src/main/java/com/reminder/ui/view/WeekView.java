package com.reminder.ui.view;

import com.reminder.i18n.I18n;
import com.reminder.model.Holiday;
import com.reminder.model.HolidayType;
import com.reminder.model.Task;
import com.reminder.model.WeekEntry;
import com.reminder.model.WorkEntry;
import com.reminder.service.TaskService;
import com.reminder.service.WorkTimeService;
import com.reminder.storage.StorageException;
import com.reminder.ui.component.EntryForm;
import com.reminder.ui.component.WeekEntryCell;
import com.reminder.ui.component.WeekTotalCell;
import com.reminder.ui.util.Dialogs;
import com.reminder.ui.util.TableCells;
import com.reminder.ui.util.Tooltips;
import com.reminder.ui.util.WrappingTableRow;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import com.reminder.util.DateUtils;
import com.reminder.util.Hours;
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
        table.setFixedCellSize(-1);
        table.setRowFactory(tv -> new WrappingTableRow<>());
        table.widthProperty().addListener((obs, oldWidth, newWidth) -> table.refresh());

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

        applyStaticHeader(projectColumn);
        applyStaticHeader(taskColumn);
        applyStaticHeader(typeColumn);
        applyStaticHeader(commentColumn);
        applyStaticHeader(totalColumn);

        ContextMenu menu = new ContextMenu();
        MenuItem editItem = new MenuItem(I18n.get("entry.editTask"));
        editItem.setOnAction(e -> edit());
        MenuItem deleteItem = new MenuItem(I18n.get("entry.deleteTask"));
        deleteItem.setOnAction(e -> delete());
        menu.getItems().addAll(editItem, deleteItem);
        table.setContextMenu(menu);

        createDayColumns();
    }

    /** Выравнивает заголовки обычных колонок с подписями дней (точки занимают верх). */
    private static void applyStaticHeader(TableColumn<?, ?> column) {
        String text = column.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        column.setText(null);
        Region spacer = new Region();
        spacer.setMinHeight(14);
        spacer.setPrefHeight(14);
        spacer.setMaxHeight(14);
        Label label = new Label(text);
        VBox box = new VBox(spacer, label);
        box.setAlignment(Pos.TOP_CENTER);
        box.setMinHeight(44);
        box.setPrefHeight(44);
        box.setMaxHeight(44);
        column.setGraphic(box);
        column.setStyle("-fx-alignment: CENTER;");
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
            col.setCellFactory(cf -> new WeekEntryCell(this::warnInvalidHours));
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

            form.refreshSuggestions();
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

        try {
            if (!taskService.replaceWeekEntry(selected, updated, weekStart(), weekEnd())) {
                Dialogs.error(I18n.get("common.error"), I18n.get("error.save"));
                return;
            }
        } catch (StorageException e) {
            Dialogs.error(I18n.get("common.error"),
                    I18n.get("error.save") + "\n" + Dialogs.reason(e));
            return;
        }

        int idx = entries.indexOf(selected);
        if (idx >= 0) {
            entries.set(idx, updated);
        }
        taskService.addPredefinedTask(new Task(result.project(), result.taskName(), result.type()));

        form.refreshSuggestions();
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
            try {
                taskService.deleteWeekEntry(selected, weekStart(), weekEnd());
                entries.remove(selected);
                updateTotals();
                table.refresh();
            } catch (StorageException e) {
                Dialogs.error(I18n.get("common.error"),
                        I18n.get("error.save") + "\n" + Dialogs.reason(e));
            }
        }
    }

    /** Очищает все записи текущей недели. */
    public void clear() {
        if (Dialogs.confirm(I18n.get("confirm.clear.title"),
                I18n.get("confirm.clear.week"),
                I18n.get("confirm.clear.content"))) {
            try {
                taskService.clearEntriesForDates(weekDays);
                entries.clear();
                updateTotals();
                table.refresh();
            } catch (StorageException e) {
                Dialogs.error(I18n.get("common.error"),
                        I18n.get("error.save") + "\n" + Dialogs.reason(e));
            }
        }
    }

    private void warnInvalidHours(String message) {
        Dialogs.warn(I18n.get("common.warning"), message);
    }

    private void handleCellEdit(TableColumn.CellEditEvent<WeekEntry, Integer> event) {        if (editInProgress) {
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
        if (newValue == null || !Hours.validWeekCell(newValue)) {
            warnInvalidHours(I18n.get("week.hoursInvalid"));
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

        WeekEntry candidate = copyEntry(entry);
        candidate.getDayHours().put(day, newValue);
        try {
            if (!taskService.replaceWeekEntry(entry, candidate, weekStart(), weekEnd())) {
                Dialogs.error(I18n.get("common.error"), I18n.get("error.save"));
                table.refresh();
                return;
            }
        } catch (StorageException e) {
            Dialogs.error(I18n.get("common.error"),
                    I18n.get("error.save") + "\n" + Dialogs.reason(e));
            table.refresh();
            return;
        }
        entry.getDayHours().put(day, newValue);
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

    /** Красный заголовок при переработке + точки событий и общий тултип. */
    private void highlightOvertimeDays() {
        for (WeekDayColumn col : dayColumns) {
            LocalDate day = col.date();
            int dayTotal = entries.stream()
                    .mapToInt(we -> we.getDayHours().getOrDefault(day, 0))
                    .sum();
            boolean overtime = dayTotal > WorkTimeService.MAX_DAILY_HOURS;
            List<Holiday> events = workTimeService.getHolidayEvents(day);
            col.setEvents(events);
            col.setHeaderStyle(overtime ? "-fx-font-weight: bold; -fx-text-fill: red;" : "");
            col.setTooltipText(tooltipText(events, overtime));
        }
    }

    private String tooltipText(List<Holiday> events, boolean overtime) {
        List<String> lines = new java.util.ArrayList<>();
        for (Holiday event : events) {
            String type = I18n.get(event.getType().labelKey());
            String name = event.getName() == null ? "" : event.getName().trim();
            lines.add(name.isEmpty() ? type : type + ": " + name);
        }
        if (overtime) {
            lines.add(I18n.get("week.overtimeTooltip"));
        }
        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    /** Сохраняет недельную задачу через один batch-commit сервиса. */
    private WeekEntry copyEntry(WeekEntry source) {
        WeekEntry copy = new WeekEntry(source.getProject(), source.getTaskName(), source.getType());
        copy.setComment(source.getComment());
        copy.getDayHours().putAll(source.getDayHours());
        return copy;
    }

    private LocalDate weekStart() {
        return weekDays.isEmpty() ? datePicker.getValue() : weekDays.get(0);
    }

    private LocalDate weekEnd() {
        return weekDays.isEmpty() ? datePicker.getValue() : weekDays.get(weekDays.size() - 1);
    }

    /** Колонка дня недели, помнит свою дату для переиспользования при навигации. */
    private static final class WeekDayColumn extends TableColumn<WeekEntry, Integer> {
        private static final double DOT_ROW_HEIGHT = 10;
        private static final double HEADER_HEIGHT = 44;

        private LocalDate date;
        private Tooltip dayTooltip;
        private final Label headerLabel = new Label();
        private final HBox dots = new HBox(2);
        private final StackPane headerBox = new StackPane(headerLabel, dots);

        WeekDayColumn(LocalDate date) {
            this.date = date;
            dots.setAlignment(Pos.CENTER);
            dots.setMinHeight(DOT_ROW_HEIGHT);
            dots.setPrefHeight(DOT_ROW_HEIGHT);
            dots.setMaxHeight(DOT_ROW_HEIGHT);
            StackPane.setAlignment(headerLabel, Pos.CENTER);
            StackPane.setAlignment(dots, Pos.TOP_CENTER);
            StackPane.setMargin(dots, new Insets(2, 0, 0, 0));
            headerBox.setMinHeight(HEADER_HEIGHT);
            headerBox.setPrefHeight(HEADER_HEIGHT);
            headerBox.setMaxHeight(HEADER_HEIGHT);
            setGraphic(headerBox);
            setStyle("-fx-alignment: CENTER;");
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

        /** Точки типов событий над датой: до трёх, дальше +N. */
        void setEvents(List<Holiday> events) {
            dots.getChildren().clear();
            int shown = Math.min(3, events.size());
            for (int i = 0; i < shown; i++) {
                Circle dot = new Circle(3, colorFor(events.get(i).getType()));
                dot.setStroke(Color.TRANSPARENT);
                dots.getChildren().add(dot);
            }
            if (events.size() > 3) {
                Label more = new Label("+" + (events.size() - 3));
                more.setStyle("-fx-font-size: 9px; -fx-padding: 0;");
                more.setMinHeight(DOT_ROW_HEIGHT);
                more.setPrefHeight(DOT_ROW_HEIGHT);
                more.setMaxHeight(DOT_ROW_HEIGHT);
                dots.getChildren().add(more);
            }
        }

        void setTooltipText(String text) {
            if (text == null || text.isBlank()) {
                if (dayTooltip != null) {
                    Tooltip.uninstall(headerBox, dayTooltip);
                    dayTooltip = null;
                }
                return;
            }
            if (dayTooltip == null) {
                dayTooltip = Tooltips.create(text);
                dayTooltip.setWrapText(true);
                dayTooltip.setMaxWidth(480);
                Tooltip.install(headerBox, dayTooltip);
            } else {
                dayTooltip.setText(text);
            }
        }

        private static Color colorFor(HolidayType type) {
            return switch (type) {
                case OFFICIAL -> Color.web("#4CAF50");
                case CORPORATE -> Color.web("#2196F3");
                case PERSONAL -> Color.web("#9C27B0");
                default -> Color.web("#9E9E9E");
            };
        }
    }
}