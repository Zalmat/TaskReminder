package com.reminder.service;

import com.reminder.model.Task;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YamlLoaderService {

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
}