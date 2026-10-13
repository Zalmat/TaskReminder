package com.reminder.service;

import com.reminder.model.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlLoaderServiceTest {

    @Test
    void loadsValidTemplate(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("t.yaml");
        Files.writeString(f, """
                tasks:
                  - project: "P"
                    taskName: "T"
                    type: "Разработка"
                """, StandardCharsets.UTF_8);

        List<Task> tasks = new YamlLoaderService().loadTasksFromYaml(f.toString());
        assertEquals(1, tasks.size());
        assertEquals("P", tasks.get(0).getProject());
        assertEquals("T", tasks.get(0).getTaskName());
        assertEquals("Разработка", tasks.get(0).getType());
    }

    @Test
    void missingKeysUseFallbacks(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("t.yaml");
        Files.writeString(f, "tasks:\n  - project: \"P\"\n", StandardCharsets.UTF_8);

        List<Task> tasks = new YamlLoaderService().loadTasksFromYaml(f.toString());
        assertEquals(1, tasks.size());
        assertEquals("Неизвестная задача", tasks.get(0).getTaskName());
        assertEquals("Другое", tasks.get(0).getType());
    }

    @Test
    void nonMapDocumentReturnsEmpty(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("t.yaml");
        Files.writeString(f, "- a\n- b\n", StandardCharsets.UTF_8);
        assertTrue(new YamlLoaderService().loadTasksFromYaml(f.toString()).isEmpty());
    }
}