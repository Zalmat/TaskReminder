package com.reminder.service;

import com.reminder.model.Task;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YamlLoaderService {

    @SuppressWarnings("unchecked")
    public List<Task> loadTasksFromYaml(String filePath) throws IOException {
        List<Task> tasks = new ArrayList<>();
        Yaml yaml = new Yaml();

        try (InputStream inputStream = new FileInputStream(filePath)) {
            Object loaded = yaml.load(inputStream);
            if (!(loaded instanceof Map<?, ?> data)) {
                return tasks;
            }
            Object tasksObj = data.get("tasks");
            if (tasksObj instanceof List<?> taskList) {
                for (Object item : taskList) {
                    if (!(item instanceof Map<?, ?> map)) {
                        continue;
                    }
                    String project = asString(map.get("project"), "Неизвестный проект");
                    String taskName = asString(map.get("taskName"), "Неизвестная задача");
                    String type = asString(map.get("type"), "Другое");
                    tasks.add(new Task(project, taskName, type));
                }
            }
            tasks.removeIf(t -> t.getTaskName() == null && t.getProject() == null);
        }
        return tasks;
    }

    private static String asString(Object value, String fallback) {
        return value != null ? String.valueOf(value) : fallback;
    }

    public void saveTasksToYaml(String filePath, List<Task> tasks) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> data = new LinkedHashMap<>();
        List<Map<String, String>> taskList = new ArrayList<>();

        for (Task task : tasks) {
            Map<String, String> taskMap = new LinkedHashMap<>();
            taskMap.put("project", task.getProject());
            taskMap.put("taskName", task.getTaskName());
            taskMap.put("type", task.getType());
            taskList.add(taskMap);
        }

        data.put("tasks", taskList);

        try (Writer writer = new OutputStreamWriter(
                Files.newOutputStream(Paths.get(filePath)), StandardCharsets.UTF_8)) {
            yaml.dump(data, writer);
        }
    }

    public String getYamlTemplate() {
        return """
            # Шаблон YAML файла для загрузки задач
            tasks:
              - project: "Проект А"
                taskName: "Разработка модуля"
                type: "Разработка"
              - project: "Проект А"
                taskName: "Тестирование"
                type: "Тестирование"
              - project: "Проект Б"
                taskName: "Дизайн"
                type: "Дизайн"
              - project: "Проект Б"
                taskName: "Встречи"
                type: "Коммуникация"
            """;
    }
}