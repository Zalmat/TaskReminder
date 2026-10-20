package com.reminder.ui.component;

import com.reminder.i18n.I18n;
import com.reminder.service.TaskService;
import com.reminder.settings.AppSettings;
import com.reminder.suggest.SuggestionKind;
import com.reminder.suggest.SuggestionService;
import com.reminder.ui.autocomplete.AutoCompleteSupport;
import com.reminder.ui.util.ComboCells;
import com.reminder.ui.util.NumericFields;
import com.reminder.util.TaskValidationUtils;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;

/** Общая форма ввода записи (проект/задача/тип/часы/комментарий). */
public class EntryForm {

    private final ComboBox<String> projectChoice;
    private final ComboBox<String> taskChoice;
    private final ComboBox<String> typeChoice;
    private final TextField hoursField;
    private final TextArea commentArea;
    private final AutoCompleteSupport projectSupport;
    private final AutoCompleteSupport taskSupport;
    private final Runnable dataChanged;

    public EntryForm(ComboBox<String> projectChoice, ComboBox<String> taskChoice,
                     ComboBox<String> typeChoice, TextField hoursField, TextArea commentArea,
                     SuggestionService suggestions, Runnable dataChanged) {
        this.projectChoice = projectChoice;
        this.taskChoice = taskChoice;
        this.typeChoice = typeChoice;
        this.hoursField = hoursField;
        this.commentArea = commentArea;
        this.dataChanged = dataChanged == null ? () -> { } : dataChanged;
        projectChoice.setCellFactory(list -> ComboCells.wrapping());
        taskChoice.setCellFactory(list -> ComboCells.wrapping());
        typeChoice.setCellFactory(list -> ComboCells.wrapping());
        NumericFields.digitsOnly(hoursField, 2);
        projectSupport = new AutoCompleteSupport(projectChoice, SuggestionKind.PROJECT,
                suggestions, null);
        taskSupport = new AutoCompleteSupport(taskChoice, SuggestionKind.TASK,
                suggestions, () -> projectChoice.getEditor().getText());
    }

    public void loadTypeChoices(TaskService taskService) {
        typeChoice.getItems().setAll(taskService.getWorkTypes());
        String defaultType = defaultType();
        if (!typeChoice.getItems().contains(defaultType)) {
            typeChoice.getItems().add(defaultType);
        }
        typeChoice.setValue(defaultType);
    }

    /** Перечитывает типы из каталога, сохраняя текущий выбор. */
    public void refreshTypeChoices(TaskService taskService) {
        String current = typeChoice.getValue();
        typeChoice.getItems().setAll(taskService.getWorkTypes());
        String value = current != null ? current : defaultType();
        if (!typeChoice.getItems().contains(value)) {
            typeChoice.getItems().add(value);
        }
        typeChoice.setValue(value);
    }

    /** Перечитывает подсказки после изменения истории/каталога. */
    public void refreshSuggestions() {
        dataChanged.run();
        refreshSuggestionsOnly();
    }

    /** Обновляет только списки, без запроса переиндексации истории. */
    public void refreshSuggestionsOnly() {
        projectSupport.refresh();
        taskSupport.refresh();
    }

    public void clear() {
        hoursField.clear();
        commentArea.clear();
        projectChoice.getEditor().clear();
        taskChoice.getEditor().clear();
        typeChoice.setValue(defaultType());
        projectChoice.setValue(null);
        taskChoice.setValue(null);
        projectChoice.setPromptText(I18n.get("main.prompt.project"));
        taskChoice.setPromptText(I18n.get("main.prompt.task"));
    }

    public String project() {
        return projectChoice.getEditor().getText().trim();
    }

    public String taskName() {
        return TaskValidationUtils.getSafeTaskName(taskChoice.getEditor().getText());
    }

    public String type() {
        return typeChoice.getValue();
    }

    public String hoursText() {
        return hoursField.getText().trim();
    }

    public String comment() {
        return commentArea.getText().trim();
    }

    public List<String> typeItems() {
        return typeChoice.getItems();
    }

    public void focusProject() {
        projectChoice.getEditor().requestFocus();
    }

    public void focusTask() {
        taskChoice.getEditor().requestFocus();
    }

    public void focusType() {
        typeChoice.requestFocus();
    }

    public void focusHours() {
        hoursField.requestFocus();
    }

    private static String defaultType() {
        String type = AppSettings.defaultType();
        return (type == null || type.isBlank()) ? I18n.get("worktype.development") : type;
    }
}