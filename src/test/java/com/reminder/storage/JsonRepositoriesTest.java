package com.reminder.storage;

import com.reminder.model.Holiday;
import com.reminder.model.HolidaySource;
import com.reminder.model.HolidayType;
import com.reminder.model.Reminder;
import com.reminder.model.Task;
import com.reminder.model.WorkEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonRepositoriesTest {

    @Test
    void allRepositoriesRoundTrip(@TempDir Path dir) {
        StorageRepositories repositories = JsonStorageRepositories.open(dir);

        WorkEntry entry = new WorkEntry("P", "T", "Dev", 2);
        entry.setDate(LocalDate.of(2026, 10, 21));
        repositories.workEntries().save(List.of(entry));
        repositories.predefinedTasks().save(List.of(new Task("P", "T", "Dev")));
        repositories.reminders().save(List.of(new Reminder(LocalTime.of(9, 0), "test")));
        repositories.holidays().save(List.of(new Holiday(LocalDate.of(2026, 1, 1),
                "New Year", true, HolidayType.OFFICIAL, HolidaySource.MANUAL)));
        repositories.catalog().save(new TaskCatalogSnapshot(
                List.of("P"), List.of("T"), List.of("Dev")));

        StorageRepositories reloaded = JsonStorageRepositories.open(dir);
        assertEquals(1, reloaded.workEntries().load().size());
        assertEquals(1, reloaded.predefinedTasks().load().size());
        assertEquals(1, reloaded.reminders().load().size());
        assertTrue(reloaded.holidays().exists());
        assertEquals(1, reloaded.holidays().load().size());
        assertEquals(List.of("P"), reloaded.catalog().load().projects());
        assertEquals(List.of("T"), reloaded.catalog().load().taskNames());
        assertEquals(List.of("Dev"), reloaded.catalog().load().types());
    }
}
