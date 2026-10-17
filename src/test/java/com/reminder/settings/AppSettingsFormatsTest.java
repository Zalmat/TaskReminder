package com.reminder.settings;

import com.reminder.service.ExportFormat;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppSettingsFormatsTest {

    @Test
    void parsesFormatsIgnoringUnknownAndBlanks() {
        assertEquals(EnumSet.of(ExportFormat.CSV, ExportFormat.EXCEL, ExportFormat.JSON),
                AppSettings.parseFormats(" CSV, EXCEL ,,UNKNOWN, JSON "));
    }

    @Test
    void blankParsesToEmpty() {
        assertTrue(AppSettings.parseFormats("").isEmpty());
        assertTrue(AppSettings.parseFormats(null).isEmpty());
    }

    @Test
    void joinsSortedNames() {
        assertEquals("CSV,JSON,YAML",
                AppSettings.joinFormats(EnumSet.of(ExportFormat.YAML, ExportFormat.CSV, ExportFormat.JSON)));
        assertEquals("", AppSettings.joinFormats(EnumSet.noneOf(ExportFormat.class)));
    }
}
