package com.reminder.ui.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportUniqueFileTest {

    @Test
    void returnsSameNameWhenFree(@TempDir Path dir) {
        assertEquals("report.csv", ExportDialogController.uniqueFile(dir.toFile(), "report.csv").getName());
    }

    @Test
    void addsSuffixOnCollision(@TempDir Path dir) throws IOException {
        File dirFile = dir.toFile();
        new File(dirFile, "report.csv").createNewFile();
        new File(dirFile, "report (1).csv").createNewFile();
        assertEquals("report (2).csv", ExportDialogController.uniqueFile(dirFile, "report.csv").getName());
    }

    @Test
    void handlesNameWithoutExtension(@TempDir Path dir) throws IOException {
        File dirFile = dir.toFile();
        new File(dirFile, "report").createNewFile();
        assertEquals("report (1)", ExportDialogController.uniqueFile(dirFile, "report").getName());
    }
}
