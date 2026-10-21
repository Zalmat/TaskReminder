package com.reminder.holiday;

import com.reminder.model.Holiday;
import com.reminder.model.HolidayType;
import com.reminder.util.HolidayTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HolidayImporterTest {

    @Test
    void parsesValidRowsAndCountsInvalid(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("holidays.yaml");
        Files.writeString(file, """
                holidays:
                  - date: "2026-01-01"
                    name: "Новый год"
                    annual: true
                    type: "official"
                  - date: "09.05.2026"
                    name: "День Победы"
                    annual: false
                  - date: "bad"
                    name: "Ошибка"
                  - date: "2026-03-08"
                    name: "Без типа"
                    type: "unknown"
                """, StandardCharsets.UTF_8);

        HolidayImporter.Result result = new HolidayImporter().importFrom(file);

        assertEquals(4, result.total());
        assertEquals(2, result.invalid());
        assertEquals(2, result.holidays().size());
        Holiday first = result.holidays().get(0);
        assertEquals(LocalDate.of(2026, 1, 1), first.getDate());
        assertEquals("Новый год", first.getName());
        assertTrue(first.isAnnual());
        assertEquals(HolidayType.OFFICIAL, first.getType());
        Holiday second = result.holidays().get(1);
        assertFalse(second.isAnnual());
        assertEquals(HolidayType.OFFICIAL, second.getType());
    }

    @Test
    void templateIsAvailableAndParsable(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("holidays.yaml");
        Files.writeString(file, HolidayTemplate.content(), StandardCharsets.UTF_8);
        HolidayImporter.Result result = new HolidayImporter().importFrom(file);
        assertEquals(2, result.holidays().size());
        assertEquals(0, result.invalid());
    }
}
