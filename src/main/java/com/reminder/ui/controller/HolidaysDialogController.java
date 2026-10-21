package com.reminder.ui.controller;

import com.reminder.holiday.HolidayImporter;
import com.reminder.i18n.I18n;
import com.reminder.model.Holiday;
import com.reminder.model.HolidayType;
import com.reminder.service.WorkTimeService;
import com.reminder.settings.AppSettings;
import com.reminder.storage.StorageException;
import com.reminder.ui.util.Dialogs;
import com.reminder.ui.util.DatePickerUtils;
import com.reminder.ui.util.TableCells;
import com.reminder.ui.util.WrappingTableRow;
import com.reminder.util.DateUtils;
import com.reminder.util.FileValidation;
import com.reminder.util.HolidayTemplate;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;

/** Контроллер окна управления праздниками и календарём. */
public class HolidaysDialogController {

    private static final DateTimeFormatter ANNUAL_DATE = DateTimeFormatter.ofPattern("dd.MM");

    @FXML private DialogPane dialogPane;
    @FXML private Label countLabel;
    @FXML private TableView<Holiday> holidayTable;
    @FXML private TableColumn<Holiday, String> dateColumn;
    @FXML private TableColumn<Holiday, String> nameColumn;
    @FXML private TableColumn<Holiday, String> annualColumn;
    @FXML private TableColumn<Holiday, String> typeColumn;
    @FXML private DatePicker holidayPicker;
    @FXML private TextField holidayNameField;
    @FXML private CheckBox annualCheck;
    @FXML private ComboBox<HolidayType> holidayTypeChoice;
    @FXML private Button addHolidayButton;
    @FXML private Button editHolidayButton;
    @FXML private Button deleteHolidayButton;
    @FXML private TextField calendarPathField;
    @FXML private Button chooseCalendarButton;
    @FXML private Button createTemplateButton;
    @FXML private Button loadCalendarButton;

    private WorkTimeService workTimeService;
    private BiConsumer<String, String> alert;
    private final ObservableList<Holiday> holidayItems = FXCollections.observableArrayList();
    private boolean loading;

    public void init(WorkTimeService workTimeService, BiConsumer<String, String> alert) {
        this.workTimeService = workTimeService;
        this.alert = alert;

        DatePickerUtils.applyTo(holidayPicker);
        holidayPicker.setValue(LocalDate.now());

        holidayTable.setItems(holidayItems);
        holidayTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        holidayTable.setFixedCellSize(-1);
        holidayTable.setRowFactory(tv -> new WrappingTableRow<>());
        holidayTable.widthProperty().addListener((obs, oldWidth, newWidth) -> holidayTable.refresh());

        dateColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(dateText(cd.getValue())));
        nameColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getName()));
        nameColumn.setCellFactory(column -> TableCells.wrapping(2));
        annualColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                I18n.get(cd.getValue().isAnnual() ? "dialog.yes" : "dialog.no")));
        typeColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                I18n.get(cd.getValue().getType().labelKey())));

        holidayTypeChoice.getItems().setAll(HolidayType.values());
        holidayTypeChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(HolidayType type) {
                return type == null ? "" : I18n.get(type.labelKey());
            }

            @Override
            public HolidayType fromString(String text) {
                return null;
            }
        });
        holidayTypeChoice.setValue(HolidayType.OFFICIAL);

        holidayTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldItem, item) -> loadSelection(item));
        addHolidayButton.setOnAction(event -> addHoliday());
        editHolidayButton.setOnAction(event -> editHoliday());
        deleteHolidayButton.setOnAction(event -> deleteHoliday());

        calendarPathField.setText(AppSettings.holidayCalendarFile());
        chooseCalendarButton.setOnAction(event -> chooseCalendar());
        createTemplateButton.setOnAction(event -> createTemplate());
        loadCalendarButton.setOnAction(event -> loadCalendar());

        dialogPane.getButtonTypes().setAll(
                new ButtonType(I18n.get("button.close"), ButtonBar.ButtonData.CANCEL_CLOSE));

        refreshList();
    }

    private void refreshList() {
        String selectedId = selectedId();
        List<Holiday> entries = workTimeService.getHolidayEntries();
        loading = true;
        holidayItems.setAll(entries);
        loading = false;
        countLabel.setText(I18n.format("holidays.count", holidayItems.size()));
        if (selectedId != null) {
            for (Holiday holiday : holidayItems) {
                if (selectedId.equals(holiday.getId())) {
                    holidayTable.getSelectionModel().select(holiday);
                    break;
                }
            }
        }
        updateButtons();
    }

    private void loadSelection(Holiday holiday) {
        if (loading) {
            return;
        }
        if (holiday != null) {
            loading = true;
            try {
                holidayPicker.setValue(holiday.getDate());
                holidayNameField.setText(holiday.getName() == null ? "" : holiday.getName());
                annualCheck.setSelected(holiday.isAnnual());
                holidayTypeChoice.setValue(holiday.getType());
            } finally {
                loading = false;
            }
        }
        updateButtons();
    }

    private void updateButtons() {
        boolean selected = holidayTable.getSelectionModel().getSelectedItem() != null;
        editHolidayButton.setDisable(!selected);
        deleteHolidayButton.setDisable(!selected);
    }

    private void addHoliday() {
        LocalDate date = holidayPicker.getValue();
        String name = holidayNameField.getText();
        if (date == null) {
            alert.accept(I18n.get("common.error"), I18n.get("holidays.selectDate"));
            return;
        }
        if (name == null || name.isBlank()) {
            alert.accept(I18n.get("common.error"), I18n.get("holidays.nameRequired"));
            return;
        }
        WorkTimeService.HolidayResult result;
        try {
            result = workTimeService.addHoliday(
                    date, name, annualCheck.isSelected(), holidayTypeChoice.getValue());
        } catch (StorageException e) {
            alert.accept(I18n.get("common.error"),
                    I18n.get("error.save") + "\n" + Dialogs.reason(e));
            return;
        }
        switch (result) {
            case ADDED -> {
                alert.accept(I18n.get("common.success"),
                        I18n.format("holidays.added", dateText(date, annualCheck.isSelected())));
                holidayNameField.clear();
                holidayTable.getSelectionModel().clearSelection();
                refreshList();
            }
            case NAME_REQUIRED ->
                    alert.accept(I18n.get("common.error"), I18n.get("holidays.nameRequired"));
            case DUPLICATE ->
                    alert.accept(I18n.get("common.warning"), I18n.get("holidays.duplicate"));
            default -> {
                // ничего
            }
        }
    }

    private void editHoliday() {
        Holiday selected = holidayTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert.accept(I18n.get("common.warning"), I18n.get("holidays.selectForEdit"));
            return;
        }
        LocalDate date = holidayPicker.getValue();
        String name = holidayNameField.getText();
        if (date == null) {
            alert.accept(I18n.get("common.error"), I18n.get("holidays.selectDate"));
            return;
        }
        if (name == null || name.isBlank()) {
            alert.accept(I18n.get("common.error"), I18n.get("holidays.nameRequired"));
            return;
        }
        Holiday updated = new Holiday(date, name, annualCheck.isSelected(),
                holidayTypeChoice.getValue(), selected.getSource());
        updated.setId(selected.getId());
        WorkTimeService.HolidayResult result;
        try {
            result = workTimeService.updateHoliday(updated);
        } catch (StorageException e) {
            alert.accept(I18n.get("common.error"),
                    I18n.get("error.save") + "\n" + Dialogs.reason(e));
            return;
        }
        switch (result) {
            case UPDATED -> {
                alert.accept(I18n.get("common.success"), I18n.get("holidays.updated"));
                refreshList();
            }
            case NAME_REQUIRED ->
                    alert.accept(I18n.get("common.error"), I18n.get("holidays.nameRequired"));
            case DUPLICATE ->
                    alert.accept(I18n.get("common.warning"), I18n.get("holidays.duplicate"));
            case NOT_FOUND ->
                    alert.accept(I18n.get("common.warning"), I18n.get("holidays.selectForEdit"));
            default -> {
                // ничего
            }
        }
    }

    private void deleteHoliday() {
        Holiday selected = holidayTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert.accept(I18n.get("common.warning"), I18n.get("holidays.selectForDelete"));
            return;
        }
        if (!Dialogs.confirm(I18n.get("holidays.header"),
                I18n.get("holidays.deleteConfirm"),
                I18n.format("holidays.deleteConfirmText", dateText(selected), selected.getName()))) {
            return;
        }
        try {
            if (workTimeService.deleteHoliday(selected.getId())) {
                holidayTable.getSelectionModel().clearSelection();
                refreshList();
                alert.accept(I18n.get("common.success"),
                        I18n.format("holidays.deleted", dateText(selected)));
            }
        } catch (StorageException e) {
            alert.accept(I18n.get("common.error"),
                    I18n.get("error.save") + "\n" + Dialogs.reason(e));
        }
    }

    private void chooseCalendar() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("holidays.import.choose"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("yaml.filter"), "*.yaml", "*.yml"));
        Window owner = dialogPane.getScene() != null ? dialogPane.getScene().getWindow() : null;
        File file = chooser.showOpenDialog(owner);
        if (file == null) {
            return;
        }
        if (!FileValidation.isReadableYaml(file.toPath())) {
            alert.accept(I18n.get("common.error"), I18n.get("holidays.import.invalidFile"));
            return;
        }
        calendarPathField.setText(file.getAbsolutePath());
        AppSettings.setHolidayCalendarFile(file.getAbsolutePath());
    }

    private void createTemplate() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("holidays.import.template"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18n.get("yaml.filter"), "*.yaml", "*.yml"));
        chooser.setInitialFileName("holidays-template.yaml");
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
            Files.writeString(path, HolidayTemplate.content(), StandardCharsets.UTF_8);
            calendarPathField.setText(path.toString());
            AppSettings.setHolidayCalendarFile(path.toString());
            alert.accept(I18n.get("common.success"),
                    I18n.format("holidays.import.templateSaved", path.getFileName()));
        } catch (IOException e) {
            alert.accept(I18n.get("common.error"),
                    I18n.format("holidays.import.error", Dialogs.reason(e)));
        }
    }

    private void loadCalendar() {
        String rawPath = calendarPathField.getText();
        if (rawPath == null || rawPath.isBlank()) {
            alert.accept(I18n.get("common.error"), I18n.get("holidays.import.invalidFile"));
            return;
        }
        Path path;
        try {
            path = Path.of(rawPath);
        } catch (RuntimeException e) {
            alert.accept(I18n.get("common.error"), I18n.get("holidays.import.invalidFile"));
            return;
        }
        if (!FileValidation.isReadableYaml(path)) {
            alert.accept(I18n.get("common.error"), I18n.get("holidays.import.invalidFile"));
            return;
        }
        try {
            HolidayImporter.Result parsed = new HolidayImporter().importFrom(path);
            WorkTimeService.HolidayPreview preview = workTimeService.previewImport(parsed.holidays());
            if (!Dialogs.confirm(I18n.get("holidays.import.title"),
                    I18n.get("holidays.import.previewHeader"),
                    I18n.format("holidays.import.preview", preview.added(),
                            preview.skipped(), parsed.invalid()))) {
                return;
            }
            WorkTimeService.HolidayImportResult result =
                    workTimeService.importHolidays(parsed.holidays());
            refreshList();
            alert.accept(I18n.get("common.success"),
                    I18n.format("holidays.import.result", result.added(),
                            result.skipped(), parsed.invalid()));
        } catch (IOException e) {
            alert.accept(I18n.get("common.error"),
                    I18n.format("holidays.import.error", Dialogs.reason(e)));
        } catch (StorageException e) {
            alert.accept(I18n.get("common.error"),
                    I18n.get("error.save") + "\n" + Dialogs.reason(e));
        }
    }

    private String selectedId() {
        Holiday selected = holidayTable.getSelectionModel().getSelectedItem();
        return selected == null ? null : selected.getId();
    }

    private String dateText(Holiday holiday) {
        return holiday == null ? "" : dateText(holiday.getDate(), holiday.isAnnual());
    }

    private String dateText(LocalDate date, boolean annual) {
        if (date == null) {
            return "";
        }
        return annual ? ANNUAL_DATE.format(date) : DateUtils.formatDate(date);
    }
}
