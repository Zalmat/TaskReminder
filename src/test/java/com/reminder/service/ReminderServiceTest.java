package com.reminder.service;

import com.reminder.model.Reminder;
import com.reminder.storage.FailingDataStore;
import com.reminder.storage.JsonStorageRepositories;
import com.reminder.storage.StorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReminderServiceTest {

    @Test
    void failedAddKeepsListEmpty(@TempDir Path dir) {
        FailingDataStore store = new FailingDataStore(dir);
        store.failSaves(true);
        ReminderService service = new ReminderService(JsonStorageRepositories.open(store).reminders());

        assertThrows(StorageException.class,
                () -> service.addReminder(new Reminder(LocalTime.of(10, 0), "test")));
        assertTrue(service.getReminders().isEmpty());
    }

    @Test
    void failedReplaceKeepsOriginal(@TempDir Path dir) {
        FailingDataStore store = new FailingDataStore(dir);
        ReminderService service = new ReminderService(JsonStorageRepositories.open(store).reminders());
        Reminder original = new Reminder(LocalTime.of(10, 0), "old");
        service.addReminder(original);

        Reminder updated = original.copy();
        updated.setText("new");
        store.failSaves(true);

        assertThrows(StorageException.class,
                () -> service.replaceReminder(original, updated));
        assertEquals("old", service.getReminders().get(0).getText());
    }
}
