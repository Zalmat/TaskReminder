package com.reminder.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileValidationTest {

    @Test
    void acceptsExistingYaml(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("tasks.YML");
        Files.writeString(file, "tasks: []");
        assertTrue(FileValidation.isReadableYaml(file));
    }

    @Test
    void rejectsOtherExtensions(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("tasks.txt");
        Files.writeString(file, "tasks: []");
        assertFalse(FileValidation.isReadableYaml(file));
    }

    @Test
    void rejectsMissingFile(@TempDir Path dir) {
        assertFalse(FileValidation.isReadableYaml(dir.resolve("missing.yaml")));
    }

    @Test
    void rejectsDirectory(@TempDir Path dir) {
        assertFalse(FileValidation.isReadableYaml(dir));
    }
}
