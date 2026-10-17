package com.reminder.service;

import com.reminder.i18n.I18n;
import com.reminder.model.WorkEntry;
import com.reminder.settings.AppSettings;
import com.reminder.settings.DecimalSeparator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportServiceTest {

    @BeforeAll
    static void initI18n() {
        I18n.init("ru");
    }

    @Test
    void exportsCsvJsonYamlXmlWithData(@TempDir Path dir) throws IOException {
        ExportService svc = new ExportService();
        WorkEntry e = new WorkEntry("Проект Альфа", "Разработка бэкенда", "Разработка", 4);
        e.setDate(LocalDate.of(2026, 10, 12));
        e.setComment("ок");
        List<WorkEntry> entries = List.of(e);

        Path csv = dir.resolve("r.csv");
        svc.exportToCsv(entries, csv.toString());
        String csvText = Files.readString(csv, StandardCharsets.UTF_8);
        assertTrue(csvText.contains("Проект Альфа"));
        assertTrue(csvText.contains("Часы"));

        Path json = dir.resolve("r.json");
        svc.exportToJson(entries, json.toString());
        String jsonText = Files.readString(json, StandardCharsets.UTF_8);
        assertTrue(jsonText.contains("\"project\""));
        assertTrue(jsonText.contains("Проект Альфа"));

        Path yaml = dir.resolve("r.yaml");
        svc.exportToYaml(entries, yaml.toString());
        assertTrue(Files.readString(yaml, StandardCharsets.UTF_8).contains("Проект Альфа"));

        Path xml = dir.resolve("r.xml");
        svc.exportToXml(entries, xml.toString());
        assertTrue(Files.readString(xml, StandardCharsets.UTF_8)
                .contains("<project>Проект Альфа</project>"));
    }

    @Test
    void dispatcherExportsSelectedFormat(@TempDir Path dir) throws IOException {
        ExportService svc = new ExportService();
        WorkEntry e = new WorkEntry("Проект Альфа", "Разработка бэкенда", "Разработка", 3);
        e.setDate(LocalDate.of(2026, 10, 12));
        List<WorkEntry> entries = List.of(e);

        Path csv = dir.resolve("d.csv");
        svc.export(entries, ExportFormat.CSV, csv.toString());
        assertTrue(Files.readString(csv, StandardCharsets.UTF_8).contains("Проект Альфа"));

        Path json = dir.resolve("d.json");
        svc.export(entries, ExportFormat.JSON, json.toString());
        assertTrue(Files.readString(json, StandardCharsets.UTF_8).contains("\"project\""));

        Path xml = dir.resolve("d.xml");
        svc.export(entries, ExportFormat.XML, xml.toString());
        assertTrue(Files.readString(xml, StandardCharsets.UTF_8).contains("workReport"));
    }

    @Test
    void csvQuotesFieldsAndUsesConfiguredSeparator(@TempDir Path dir) throws IOException {
        DecimalSeparator saved = AppSettings.decimalSeparator();
        try {
            AppSettings.setDecimalSeparator(DecimalSeparator.COMMA);
            ExportService svc = new ExportService();
            WorkEntry e = new WorkEntry("A,B", "Task", "Type", 1.5);
            e.setDate(LocalDate.of(2026, 10, 12));
            e.setComment("line1\nline2");
            Path csv = dir.resolve("q.csv");
            svc.exportToCsv(List.of(e), csv.toString());
            String text = Files.readString(csv, StandardCharsets.UTF_8);
            assertTrue(text.contains("\"A,B\""), text);
            assertTrue(text.contains("1,5"), text);
            assertTrue(text.contains("\"line1\nline2\""), text);
        } finally {
            AppSettings.setDecimalSeparator(saved);
        }
    }
}