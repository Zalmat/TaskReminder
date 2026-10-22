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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteRepositoriesTest {

    @Test
    void allRepositoriesRoundTrip(@TempDir Path dir) {
        try (SqliteDatabase database = new SqliteDatabase(dir.resolve(SqliteDatabase.FILE))) {
            StorageRepositories repositories = SqliteStorageRepositories.create(database);

            WorkEntry entry = new WorkEntry("P", "T", "Dev", 2.5);
            entry.setDate(LocalDate.of(2026, 10, 21));
            entry.setComment("comment");
            entry.setCreatedAt(LocalDateTime.of(2026, 10, 21, 12, 30));
            repositories.workEntries().save(List.of(entry));

            repositories.predefinedTasks().save(List.of(new Task("P", "T", "Dev", 1, "x")));

            Reminder reminder = new Reminder(LocalTime.of(9, 15), "test", true);
            reminder.setActive(false);
            reminder.setSnoozeUntil(LocalDateTime.of(2026, 10, 22, 10, 0));
            repositories.reminders().save(List.of(reminder));

            Holiday holiday = new Holiday(LocalDate.of(2026, 1, 1), "New Year", true,
                    HolidayType.OFFICIAL, HolidaySource.MANUAL);
            repositories.holidays().save(List.of(holiday));

            repositories.catalog().save(new TaskCatalogSnapshot(
                    List.of("P", "P2"), List.of("T"), List.of("Dev", "QA")));

            assertEquals(1, repositories.workEntries().load().size());
            assertEquals(1, repositories.predefinedTasks().load().size());
            assertEquals(1, repositories.reminders().load().size());
            assertTrue(repositories.holidays().exists());
            assertEquals(1, repositories.holidays().load().size());
            assertEquals(List.of("P", "P2"), repositories.catalog().load().projects());
            assertEquals(List.of("Dev", "QA"), repositories.catalog().load().types());
        }

        try (SqliteDatabase database = new SqliteDatabase(dir.resolve(SqliteDatabase.FILE))) {
            StorageRepositories repositories = SqliteStorageRepositories.create(database);
            assertEquals(2.5, repositories.workEntries().load().get(0).getHours());
            assertEquals(LocalDate.of(2026, 10, 21),
                    repositories.workEntries().load().get(0).getDate());
            assertEquals("test", repositories.reminders().load().get(0).getText());
            assertFalse(repositories.reminders().load().get(0).isActive());
            assertEquals("New Year", repositories.holidays().load().get(0).getName());
        }
    }

    @Test
    void failedSaveRollsBackWholeList(@TempDir Path dir) {
        try (SqliteDatabase database = new SqliteDatabase(dir.resolve(SqliteDatabase.FILE))) {
            SqliteReminderRepository repository = new SqliteReminderRepository(database);
            Reminder reminder = new Reminder(LocalTime.of(9, 0), "one");
            repository.save(List.of(reminder));

            assertThrows(StorageException.class,
                    () -> repository.save(List.of(reminder, reminder)));
            assertEquals(1, repository.load().size());
        }
    }

    @Test
    void holidayInitializationFlagIsPersisted(@TempDir Path dir) {
        try (SqliteDatabase database = new SqliteDatabase(dir.resolve(SqliteDatabase.FILE))) {
            SqliteHolidayRepository repository = new SqliteHolidayRepository(database);
            assertFalse(repository.exists());
            repository.save(List.of());
            assertTrue(repository.exists());
        }
    }
}
