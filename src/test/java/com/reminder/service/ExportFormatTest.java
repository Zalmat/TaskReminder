package com.reminder.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportFormatTest {

    @Test
    void fromLabelMapsEveryLabel() {
        for (ExportFormat format : ExportFormat.values()) {
            assertEquals(format, ExportFormat.fromLabel(format.label()));
        }
    }

    @Test
    void fromLabelFallsBackToExcel() {
        assertEquals(ExportFormat.EXCEL, ExportFormat.fromLabel(null));
        assertEquals(ExportFormat.EXCEL, ExportFormat.fromLabel("неизвестный формат"));
    }

    @Test
    void extensionsAndFilterNamesAreSet() {
        assertEquals("xlsx", ExportFormat.EXCEL.extension());
        assertEquals("csv", ExportFormat.CSV.extension());
        assertEquals("json", ExportFormat.JSON.extension());
        assertEquals("yaml", ExportFormat.YAML.extension());
        assertEquals("xml", ExportFormat.XML.extension());
        assertEquals("Excel файлы", ExportFormat.EXCEL.filterName());
    }
}