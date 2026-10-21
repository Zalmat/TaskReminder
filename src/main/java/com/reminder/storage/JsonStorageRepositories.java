package com.reminder.storage;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Фабрика JSON-репозиториев с общим DataStore. */
public final class JsonStorageRepositories {

    private JsonStorageRepositories() {
    }

    public static StorageRepositories openDefault() {
        return open(Paths.get("").toAbsolutePath().normalize());
    }

    public static StorageRepositories open(Path directory) {
        return open(new DataStore(directory));
    }

    public static StorageRepositories open(DataStore store) {
        return new StorageRepositories(
                new JsonWorkEntryRepository(store),
                new JsonPredefinedTaskRepository(store),
                new JsonReminderRepository(store),
                new JsonHolidayRepository(store),
                new JsonTaskCatalogRepository(store));
    }
}
