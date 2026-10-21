package com.reminder.storage;

import com.reminder.model.WorkEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyMigratorTest {

    @Test
    void doesNotMoveLegacyFileWhenJsonSaveFails(@TempDir Path dir) throws Exception {
        Path legacy = dir.resolve("work_entries.dat");
        WorkEntry entry = new WorkEntry("P", "T", "Dev", 2);
        entry.setDate(LocalDate.of(2026, 10, 12));
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(legacy))) {
            out.writeObject(List.of(entry));
        }

        FailingDataStore store = new FailingDataStore(dir);
        store.failSaves(true);
        LegacyMigrator migrator = new LegacyMigrator(store);

        assertFalse(migrator.migrate("work_entries.dat", "work_entries.json"));
        assertTrue(Files.isRegularFile(legacy));
        assertFalse(Files.exists(dir.resolve("work_entries.dat.legacy")));
        assertFalse(Files.exists(dir.resolve("work_entries.json")));
    }
}
