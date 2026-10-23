package com.reminder.storage;

import com.reminder.holiday.DefaultHolidays;
import com.reminder.model.Holiday;
import com.reminder.model.HolidaySource;
import com.reminder.model.HolidayType;
import com.reminder.model.HolidaysData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Версия схемы, backup и последовательные миграции данных. */
public final class StorageMigrations {

    public static final int CURRENT_VERSION = 2;

    static final String META_FILE = "storage.meta.json";

    private StorageMigrations() {
    }

    public static int migrate(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new StorageException(META_FILE, "Не удалось создать каталог данных", e);
        }

        DataStore store = new DataStore(directory);
        Path metaFile = directory.resolve(META_FILE);
        if (Files.isRegularFile(metaFile)) {
            StorageMeta meta = store.loadObject(META_FILE, StorageMeta.class);
            if (meta == null) {
                throw new StorageException(META_FILE, "Файл версии схемы пуст или повреждён");
            }
            validateVersion(meta.schemaVersion);
            if (meta.schemaVersion == CURRENT_VERSION) {
                return meta.schemaVersion;
            }
            migrateFrom(directory, store, meta.schemaVersion);
            return CURRENT_VERSION;
        }

        if (Files.isRegularFile(directory.resolve(SqliteDatabase.FILE))) {
            recoverDatabaseWithoutMeta(directory, store);
            return CURRENT_VERSION;
        }

        if (!hasData(directory)) {
            initializeEmpty(directory, store);
            return CURRENT_VERSION;
        }

        migrateFrom(directory, store, 0);
        return CURRENT_VERSION;
    }

    private static void recoverDatabaseWithoutMeta(Path directory, DataStore store) {
        try (SqliteDatabase database = new SqliteDatabase(directory.resolve(SqliteDatabase.FILE))) {
            if (database.version() != SqliteDatabase.SCHEMA_VERSION) {
                throw new StorageException(SqliteDatabase.FILE,
                        "SQLite-база существует, но версия схемы не совпадает");
            }
        } catch (Exception e) {
            if (e instanceof StorageException storageException) {
                throw storageException;
            }
            throw new StorageException(SqliteDatabase.FILE,
                    "SQLite-база существует, но не читается", e);
        }
        writeMeta(store);
    }

    private static void initializeEmpty(Path directory, DataStore store) {
        try (SqliteDatabase ignored = new SqliteDatabase(directory.resolve(SqliteDatabase.FILE))) {
            // Создаём пустую базу v2.
        }
        writeMeta(store);
    }

    private static void validateVersion(int version) {
        if (version > CURRENT_VERSION) {
            throw new StorageException(META_FILE,
                    "Данные созданы более новой версией приложения (schemaVersion=" + version + ")");
        }
        if (version < 0) {
            throw new StorageException(META_FILE, "Некорректная версия схемы данных: " + version);
        }
    }

    private static void migrateFrom(Path directory, DataStore store, int fromVersion) {
        Path backup = StorageBackup.create(directory, fromVersion);
        try {
            if (fromVersion < 1) {
                migrateV0ToV1(directory, store);
            }
            if (fromVersion < 2) {
                migrateV1ToV2(directory);
            }
            verifyRepositories(directory);
            writeMeta(store);
            StorageBackup.prune(directory);
        } catch (Exception e) {
            try {
                StorageBackup.restore(directory, backup);
            } catch (IOException restoreError) {
                e.addSuppressed(restoreError);
            }
            throw new StorageException(META_FILE,
                    "Миграция данных не удалась; сохранён backup: " + backup, e);
        }
    }

    private static void migrateV0ToV1(Path directory, DataStore store) throws IOException {
        LegacyMigrator migrator = new LegacyMigrator(store);
        migrator.migrate("work_entries.dat", "work_entries.json");
        migrator.migrate("predefined_tasks.dat", "predefined_tasks.json");
        normalizeLegacyHolidays(directory, store);
    }

    private static void migrateV1ToV2(Path directory) throws IOException {
        Path target = directory.resolve(SqliteDatabase.FILE);
        Path temp = directory.resolve(SqliteDatabase.FILE + ".new");
        if (Files.exists(target)) {
            throw new StorageException(SqliteDatabase.FILE,
                    "SQLite-база уже существует до миграции");
        }
        Files.deleteIfExists(temp);

        try {
            StorageRepositories json = JsonStorageRepositories.open(directory);
            try (SqliteDatabase database = new SqliteDatabase(temp)) {
                StorageRepositories sqlite = SqliteStorageRepositories.create(database);
                sqlite.workEntries().save(json.workEntries().load());
                sqlite.predefinedTasks().save(json.predefinedTasks().load());
                sqlite.reminders().save(json.reminders().load());
                sqlite.catalog().save(json.catalog().load());
                if (json.holidays().exists()) {
                    sqlite.holidays().save(json.holidays().load());
                }
            }
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target);
            }
        } catch (Exception e) {
            Files.deleteIfExists(temp);
            if (e instanceof StorageException storageException) {
                throw storageException;
            }
            throw new StorageException(SqliteDatabase.FILE, "Не удалось создать SQLite-базу", e);
        }
    }

    private static void normalizeLegacyHolidays(Path directory, DataStore store) throws IOException {
        Path file = directory.resolve("holidays.json");
        if (!Files.isRegularFile(file)) {
            return;
        }
        String text = Files.readString(file, StandardCharsets.UTF_8).trim();
        if (!text.startsWith("[")) {
            if (store.loadObject("holidays.json", HolidaysData.class) == null) {
                throw new StorageException("holidays.json", "Файл праздников повреждён");
            }
            return;
        }

        Path backup = directory.resolve("holidays.json.bak");
        if (!Files.exists(backup)) {
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        }
        List<LocalDate> dates = store.loadList("holidays.json", LocalDate.class);
        HolidaysData data = new HolidaysData();
        data.holidays = new ArrayList<>();
        for (LocalDate date : dates) {
            if (date == null) {
                continue;
            }
            Holiday holiday = DefaultHolidays.find(date)
                    .map(entry -> DefaultHolidays.create(date, entry))
                    .orElseGet(() -> new Holiday(date, "", false, HolidayType.OFFICIAL,
                            HolidaySource.MANUAL));
            data.holidays.add(holiday);
        }
        store.saveObject("holidays.json", data);
    }

    private static void verifyRepositories(Path directory) {
        try (SqliteDatabase database = new SqliteDatabase(directory.resolve(SqliteDatabase.FILE))) {
            StorageRepositories repositories = SqliteStorageRepositories.create(database);
            repositories.workEntries().load();
            repositories.predefinedTasks().load();
            repositories.reminders().load();
            repositories.holidays().load();
            repositories.catalog().load();
        }
    }

    private static void writeMeta(DataStore store) {
        store.saveObject(META_FILE, new StorageMeta(CURRENT_VERSION, Instant.now().toString()));
    }

    private static boolean hasData(Path directory) {
        String[] names = {
                "work_entries.json", "predefined_tasks.json", "reminders.json",
                "holidays.json", "holidays.json.bak", "task_catalog.json",
                "work_entries.dat", "predefined_tasks.dat"
        };
        for (String name : names) {
            if (Files.isRegularFile(directory.resolve(name))) {
                return true;
            }
        }
        return false;
    }
}
