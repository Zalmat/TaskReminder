package com.reminder.storage;

import com.reminder.model.Holiday;
import com.reminder.model.HolidaySource;
import com.reminder.model.WorkEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageMigrationsTest {

    @Test
    void emptyDirectoryCreatesMetaWithoutBackup(@TempDir Path dir) {
        try (StorageContext ignored = StorageContext.open(dir)) {
            // База создаётся при первом открытии.
        }

        StorageMeta meta = new DataStore(dir).loadObject(
                StorageMigrations.META_FILE, StorageMeta.class);
        assertEquals(StorageMigrations.CURRENT_VERSION, meta.schemaVersion);
        assertTrue(Files.isRegularFile(dir.resolve(SqliteDatabase.FILE)));
        assertFalse(Files.exists(dir.resolve("backups")));
    }

    @Test
    void existingDataWithoutMetaGetsBackupAndMeta(@TempDir Path dir) throws Exception {
        DataStore store = new DataStore(dir);
        WorkEntry entry = new WorkEntry("P", "T", "Dev", 2);
        entry.setDate(LocalDate.of(2026, 10, 21));
        store.saveList("work_entries.json", List.of(entry));

        try (StorageContext context = StorageContext.open(dir)) {
            assertEquals(1, context.repositories().workEntries().load().size());
        }
        assertTrue(Files.isRegularFile(dir.resolve(StorageMigrations.META_FILE)));
        Path backup = onlyBackup(dir);
        assertTrue(Files.isRegularFile(backup.resolve("work_entries.json")));
        assertTrue(Files.readString(backup.resolve("manifest.txt"), StandardCharsets.UTF_8)
                .contains("work_entries.json"));
    }

    @Test
    void v1JsonIsMigratedToSqlite(@TempDir Path dir) throws Exception {
        DataStore store = new DataStore(dir);
        WorkEntry entry = new WorkEntry("P", "T", "Dev", 2);
        entry.setDate(LocalDate.of(2026, 10, 21));
        store.saveList("work_entries.json", List.of(entry));
        store.saveObject(StorageMigrations.META_FILE, new StorageMeta(1, "x"));

        try (StorageContext context = StorageContext.open(dir)) {
            assertEquals(1, context.repositories().workEntries().load().size());
        }

        assertTrue(Files.isRegularFile(dir.resolve(SqliteDatabase.FILE)));
        assertTrue(Files.isRegularFile(dir.resolve("work_entries.json")));
        assertEquals(StorageMigrations.CURRENT_VERSION,
                new DataStore(dir).loadObject(StorageMigrations.META_FILE, StorageMeta.class).schemaVersion);
        assertTrue(Files.isRegularFile(onlyBackup(dir).resolve("work_entries.json")));
    }

    @Test
    void currentMetaDoesNotCreateAnotherBackup(@TempDir Path dir) {
        try (StorageContext ignored = StorageContext.open(dir)) {
            // Первый запуск создаёт схему.
        }
        try (StorageContext ignored = StorageContext.open(dir)) {
            // Повторный запуск не должен мигрировать.
        }

        assertFalse(Files.exists(dir.resolve("backups")));
    }

    @Test
    void futureVersionFailsWithoutChanges(@TempDir Path dir) throws Exception {
        Path meta = dir.resolve(StorageMigrations.META_FILE);
        String content = "{\"schemaVersion\":99,\"createdAt\":\"x\"}";
        Files.writeString(meta, content, StandardCharsets.UTF_8);

        assertThrows(StorageException.class, () -> StorageContext.open(dir));
        assertEquals(content, Files.readString(meta, StandardCharsets.UTF_8));
        assertFalse(Files.exists(dir.resolve("backups")));
    }

    @Test
    void corruptMetaFailsWithoutChanges(@TempDir Path dir) throws Exception {
        Path meta = dir.resolve(StorageMigrations.META_FILE);
        Files.writeString(meta, "{broken", StandardCharsets.UTF_8);

        assertThrows(StorageException.class, () -> StorageContext.open(dir));
        assertEquals("{broken", Files.readString(meta, StandardCharsets.UTF_8));
        assertFalse(Files.exists(dir.resolve("backups")));
    }

    @Test
    void legacyHolidaysAreNormalizedInV1(@TempDir Path dir) throws Exception {
        Path holidays = dir.resolve("holidays.json");
        Files.writeString(holidays, "[\"01.01.2026\", \"09.05.2026\"]",
                StandardCharsets.UTF_8);

        try (StorageContext context = StorageContext.open(dir)) {
            List<Holiday> loaded = context.repositories().holidays().load();
            assertEquals(2, loaded.size());
            assertEquals(List.of("Новый год", "День Победы"),
                    loaded.stream().map(Holiday::getName).toList());
            assertTrue(loaded.stream().allMatch(Holiday::isAnnual));
            assertTrue(loaded.stream().allMatch(h -> h.getSource() == HolidaySource.DEFAULT));
        }
        assertTrue(Files.readString(holidays, StandardCharsets.UTF_8).trim().startsWith("{"));
        assertTrue(Files.isRegularFile(dir.resolve("holidays.json.bak")));
        assertTrue(Files.isRegularFile(dir.resolve(StorageMigrations.META_FILE)));
        assertTrue(Files.isRegularFile(onlyBackup(dir).resolve("holidays.json")));
    }

    @Test
    void legacyDatIsMigratedBeforeMeta(@TempDir Path dir) throws Exception {
        Path legacy = dir.resolve("work_entries.dat");
        WorkEntry entry = new WorkEntry("P", "T", "Dev", 3);
        entry.setDate(LocalDate.of(2026, 10, 21));
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(legacy))) {
            out.writeObject(List.of(entry));
        }

        try (StorageContext context = StorageContext.open(dir)) {
            assertEquals(1, context.repositories().workEntries().load().size());
        }
        assertTrue(Files.isRegularFile(dir.resolve("work_entries.json")));
        assertTrue(Files.isRegularFile(dir.resolve("work_entries.dat.legacy")));
        assertTrue(Files.isRegularFile(dir.resolve(StorageMigrations.META_FILE)));
        assertTrue(Files.isRegularFile(onlyBackup(dir).resolve("work_entries.dat")));
    }

    @Test
    void failedMigrationRestoresSnapshotAndDoesNotWriteMeta(@TempDir Path dir) throws Exception {
        Path holidays = dir.resolve("holidays.json");
        Files.writeString(holidays, "[broken", StandardCharsets.UTF_8);

        assertThrows(StorageException.class, () -> StorageContext.open(dir));

        assertEquals("[broken", Files.readString(holidays, StandardCharsets.UTF_8));
        assertFalse(Files.exists(dir.resolve(StorageMigrations.META_FILE)));
        assertTrue(Files.isRegularFile(onlyBackup(dir).resolve("holidays.json")));
    }

    @Test
    void corruptSqliteFailsClosed(@TempDir Path dir) throws Exception {
        try (StorageContext ignored = StorageContext.open(dir)) {
            // Создаём валидную базу и meta v2.
        }
        Path database = dir.resolve(SqliteDatabase.FILE);
        Files.writeString(database, "not a database", StandardCharsets.UTF_8);

        assertThrows(StorageException.class, () -> StorageContext.open(dir));
        assertEquals("not a database", Files.readString(database, StandardCharsets.UTF_8));
    }

    @Test
    void missingSqliteFailsClosed(@TempDir Path dir) throws Exception {
        try (StorageContext ignored = StorageContext.open(dir)) {
            // Создаём валидную базу и meta v2.
        }
        Files.delete(dir.resolve(SqliteDatabase.FILE));

        assertThrows(StorageException.class, () -> StorageContext.open(dir));
    }

    @Test
    void pruneKeepsFiveNewestBackups(@TempDir Path dir) throws Exception {
        Path backups = dir.resolve("backups");
        for (int i = 1; i <= 6; i++) {
            Files.createDirectories(backups.resolve("pre-migration-v0-000" + i));
        }

        StorageBackup.prune(dir);

        try (var stream = Files.list(backups)) {
            List<String> names = stream.map(path -> path.getFileName().toString()).sorted().toList();
            assertEquals(5, names.size());
            assertFalse(names.contains("pre-migration-v0-0001"));
            assertTrue(names.contains("pre-migration-v0-0006"));
        }
    }

    private static Path onlyBackup(Path dir) {
        try (var stream = Files.list(dir.resolve("backups"))) {
            return stream.filter(Files::isDirectory).findFirst().orElseThrow();
        } catch (Exception e) {
            throw new AssertionError("backup not found", e);
        }
    }
}
