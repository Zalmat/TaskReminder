package com.reminder.storage;

import com.reminder.model.Holiday;
import com.reminder.model.HolidaySource;
import com.reminder.model.HolidayType;
import com.reminder.model.HolidaysData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Версия схемы, backup и последовательные миграции данных. */
public final class StorageMigrations {

    public static final int CURRENT_VERSION = 1;

    static final String META_FILE = "storage.meta.json";

    private StorageMigrations() {
    }

    public static void migrate(Path directory) {
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
                return;
            }
            migrateFrom(directory, store, meta.schemaVersion);
            return;
        }

        if (!hasData(directory)) {
            writeMeta(store);
            return;
        }

        migrateFrom(directory, store, 0);
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
            if (date != null) {
                data.holidays.add(new Holiday(date, "", false, HolidayType.OFFICIAL,
                        HolidaySource.MANUAL));
            }
        }
        store.saveObject("holidays.json", data);
    }

    private static void verifyRepositories(Path directory) {
        StorageRepositories repositories = JsonStorageRepositories.open(directory);
        repositories.workEntries().load();
        repositories.predefinedTasks().load();
        repositories.reminders().load();
        repositories.holidays().load();
        repositories.catalog().load();
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
