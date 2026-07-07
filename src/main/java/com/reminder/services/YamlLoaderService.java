package com.reminder.services;

import com.reminder.models.Task;
import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.util.*;

public class YamlLoaderService {

    @SuppressWarnings("unchecked")
    public List<Task> loadTasksFromYaml(String filePath) throws IOException {
        List<Task> tasks = new ArrayList<>();
        Yaml yaml = new Yaml();

        try (InputStream inputStream = new FileInputStream(filePath)) {
            Map<String, Object> data = yaml.load(inputStream);

            if (data != null && data.containsKey("tasks")) {
                List<Map<String, String>> taskList = (List<Map<String, String>>) data.get("tasks");
                for (Map<String, String> taskMap : taskList) {
                    Task task = new Task(
                            taskMap.getOrDefault("project", "Неизвестный проект"),
                            taskMap.getOrDefault("taskName", "Неизвестная задача"),
                            taskMap.getOrDefault("type", "Другое")
                    );
                    tasks.add(task);
                }
            }
        }
        return tasks;
    }

    public void saveTasksToYaml(String filePath, List<Task> tasks) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> data = new HashMap<>();
        List<Map<String, String>> taskList = new ArrayList<>();

        for (Task task : tasks) {
            Map<String, String> taskMap = new LinkedHashMap<>();
            taskMap.put("project", task.getProject());
            taskMap.put("taskName", task.getTaskName());
            taskMap.put("type", task.getType());
            taskList.add(taskMap);
        }

        data.put("tasks", taskList);

        try (Writer writer = new FileWriter(filePath)) {
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