package com.reminder.storage;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Точка входа в хранилище: миграции и набор репозиториев. */
public final class StorageContext {

    private final Path directory;
    private final StorageRepositories repositories;

    private StorageContext(Path directory, StorageRepositories repositories) {
        this.directory = directory;
        this.repositories = repositories;
    }

    public static StorageContext openDefault() {
        return open(Paths.get("").toAbsolutePath().normalize());
    }

    public static StorageContext open(Path directory) {
        Path normalized = directory.toAbsolutePath().normalize();
        StorageMigrations.migrate(normalized);
        return new StorageContext(normalized, JsonStorageRepositories.open(normalized));
    }

    public Path directory() {
        return directory;
    }

    public StorageRepositories repositories() {
        return repositories;
    }
}
