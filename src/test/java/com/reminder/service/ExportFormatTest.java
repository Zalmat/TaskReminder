package com.reminder.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportFormatTest {

    @Test
    void extensionsAndFilterNamesAreSet() {
        assertEquals("xlsx", ExportFormat.EXCEL.extension());
        assertEquals("csv", ExportFormat.CSV.extension());
        assertEquals("json", ExportFormat.JSON.extension());
        assertEquals("yaml", ExportFormat.YAML.extension());
        assertEquals("xml", ExportFormat.XML.extension());
        assertEquals("format.filter.excel", ExportFormat.EXCEL.filterNameKey());
    }

    @Test
    void labelsAreSet() {
        for (ExportFormat format : ExportFormat.values()) {
            assertTrue(format.label() != null && !format.label().isBlank());
            assertTrue(format.filterNameKey().startsWith("format.filter."));
        }
    }
}
