package com.reminder.ui.component;

import com.reminder.i18n.I18n;
import com.reminder.service.TaskService;
import com.reminder.settings.AppSettings;
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

    public EntryForm(ComboBox<String> projectChoice, ComboBox<String> taskChoice,
                     ComboBox<String> typeChoice, TextField hoursField, TextArea commentArea) {
        this.projectChoice = projectChoice;
        this.taskChoice = taskChoice;
        this.typeChoice = typeChoice;
        this.hoursField = hoursField;
        this.commentArea = commentArea;
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

    public void refreshChoices(TaskService taskService) {
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
        taskChoice.getItems().addAll(TaskValidationUtils.filterTaskNames(taskService.getAllTaskNames()));
        if (!currentTask.isEmpty()) {
            taskChoice.getEditor().setText(currentTask);
        }
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