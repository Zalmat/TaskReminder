package com.reminder.service;

import com.reminder.model.Holiday;
import com.reminder.model.HolidaySource;
import com.reminder.model.HolidayType;
import com.reminder.storage.DataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkTimeServiceHolidayTest {

    private WorkTimeService emptyService(Path dir) throws Exception {
        Files.writeString(dir.resolve("holidays.json"), "{\"holidays\":[]}",
                StandardCharsets.UTF_8);
        return new WorkTimeService(new DataStore(dir));
    }

    @Test
    void seedsDefaultAnnualHolidaysOnFreshProfile(@TempDir Path dir) {
        WorkTimeService service = new WorkTimeService(new DataStore(dir));
        List<Holiday> entries = service.getHolidayEntries();
        assertEquals(8, entries.size());
        assertTrue(entries.stream().allMatch(Holiday::isAnnual));
        assertTrue(entries.stream().allMatch(h -> h.getName().isEmpty()));
        assertTrue(entries.stream().allMatch(h -> h.getType() == HolidayType.OFFICIAL));
        assertTrue(entries.stream().allMatch(h -> h.getSource() == HolidaySource.DEFAULT));
        assertTrue(service.isHoliday(LocalDate.of(2035, 1, 1)));
    }

    @Test
    void addRequiresNameAndRejectsCaseInsensitiveDuplicate(@TempDir Path dir) throws Exception {
        WorkTimeService service = emptyService(dir);
        LocalDate date = LocalDate.of(2026, 12, 31);

        assertEquals(WorkTimeService.HolidayResult.NAME_REQUIRED,
                service.addHoliday(date, "  ", false, HolidayType.OFFICIAL));
        assertEquals(WorkTimeService.HolidayResult.ADDED,
                service.addHoliday(date, "Корпоратив", false, HolidayType.CORPORATE));
        assertEquals(WorkTimeService.HolidayResult.DUPLICATE,
                service.addHoliday(date, " корпоратив ", false, HolidayType.CORPORATE));
        assertEquals(WorkTimeService.HolidayResult.ADDED,
                service.addHoliday(date, "ДР Ивана", false, HolidayType.PERSONAL));

        assertEquals(2, service.getHolidayEvents(date).size());
    }

    @Test
    void annualMatchesEveryYearButOneOffDoesNot(@TempDir Path dir) throws Exception {
        WorkTimeService service = emptyService(dir);
        service.addHoliday(LocalDate.of(2026, 1, 1), "Новый год", true, HolidayType.OFFICIAL);
        service.addHoliday(LocalDate.of(2026, 3, 9), "Перенос", false, HolidayType.OFFICIAL);

        assertEquals(1, service.getHolidayEvents(LocalDate.of(2030, 1, 1)).size());
        assertTrue(service.getHolidayEvents(LocalDate.of(2030, 3, 9)).isEmpty());
        assertEquals(1, service.getHolidayEvents(LocalDate.of(2026, 3, 9)).size());
    }

    @Test
    void updateNamesLegacyEntryAndDeleteWorks(@TempDir Path dir) throws Exception {
        WorkTimeService service = emptyService(dir);
        LocalDate date = LocalDate.of(2026, 5, 9);
        service.addHoliday(date, "Старое", false, HolidayType.OFFICIAL);
        Holiday existing = service.getHolidayEvents(date).get(0);

        Holiday updated = new Holiday(date, "День Победы", false,
                HolidayType.OFFICIAL, existing.getSource());
        updated.setId(existing.getId());
        assertEquals(WorkTimeService.HolidayResult.UPDATED, service.updateHoliday(updated));
        assertEquals("День Победы", service.getHolidayEvents(date).get(0).getName());

        assertTrue(service.deleteHoliday(existing.getId()));
        assertFalse(service.isHoliday(date));
    }

    @Test
    void migratesOldDateListAsOneOffWithBackup(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("holidays.json");
        Files.writeString(file, "[\"01.01.2026\", \"09.05.2026\"]", StandardCharsets.UTF_8);

        WorkTimeService service = new WorkTimeService(new DataStore(dir));
        List<Holiday> entries = service.getHolidayEntries();
        assertEquals(2, entries.size());
        assertTrue(entries.stream().noneMatch(Holiday::isAnnual));
        assertTrue(entries.stream().allMatch(h -> h.getName().isEmpty()));
        assertTrue(Files.isRegularFile(dir.resolve("holidays.json.bak")));

        String rewritten = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(rewritten.trim().startsWith("{"));
        assertEquals(2, new WorkTimeService(new DataStore(dir)).getHolidayEntries().size());
    }

    @Test
    void previewAndImportSkipDuplicates(@TempDir Path dir) throws Exception {
        WorkTimeService service = emptyService(dir);
        service.addHoliday(LocalDate.of(2026, 1, 1), "Новый год", true, HolidayType.OFFICIAL);

        List<Holiday> imported = List.of(
                new Holiday(LocalDate.of(2026, 1, 1), "новый год", true,
                        HolidayType.OFFICIAL, HolidaySource.IMPORTED),
                new Holiday(LocalDate.of(2026, 3, 9), "Перенос", false,
                        HolidayType.OFFICIAL, HolidaySource.IMPORTED),
                new Holiday(LocalDate.of(2026, 3, 9), "Перенос", false,
                        HolidayType.OFFICIAL, HolidaySource.IMPORTED));

        WorkTimeService.HolidayPreview preview = service.previewImport(imported);
        assertEquals(1, preview.added());
        assertEquals(2, preview.skipped());
        WorkTimeService.HolidayImportResult result = service.importHolidays(imported);
        assertEquals(1, result.added());
        assertEquals(2, result.skipped());
        assertEquals(1, service.getHolidayEvents(LocalDate.of(2026, 3, 9)).size());
    }
}
