package com.reminder.storage;

import com.reminder.model.WorkEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataStoreTest {

    @Test
    void savesAndLoadsEntriesWithDatesAndCyrillic(@TempDir Path dir) {
        DataStore store = new DataStore(dir);
        WorkEntry entry = new WorkEntry("Линейное развитие", "Общепроектные работы", "Разработка", 4);
        entry.setDate(LocalDate.of(2026, 10, 12));
        entry.setComment("тест");

        store.saveList("work_entries.json", List.of(entry));

        List<WorkEntry> loaded = store.loadList("work_entries.json", WorkEntry.class);
        assertEquals(1, loaded.size());
        WorkEntry l = loaded.get(0);
        assertEquals("Линейное развитие", l.getProject());
        assertEquals("Общепроектные работы", l.getTaskName());
        assertEquals("Разработка", l.getType());
        assertEquals(LocalDate.of(2026, 10, 12), l.getDate());
        assertEquals(4.0, l.getHours());
        assertEquals("тест", l.getComment());
    }

    @Test
    void missingFileReturnsEmptyList(@TempDir Path dir) {
        DataStore store = new DataStore(dir);
        assertTrue(store.loadList("nope.json", WorkEntry.class).isEmpty());
        assertFalse(store.exists("nope.json"));
    }

    @Test
    void noTmpFileLeftAfterSave(@TempDir Path dir) {
        DataStore store = new DataStore(dir);
        store.saveList("x.json", List.of());
        assertFalse(Files.exists(dir.resolve("x.json.tmp")));
        assertTrue(store.exists("x.json"));
    }
}