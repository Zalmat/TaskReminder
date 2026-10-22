package com.reminder.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Точка входа в хранилище: миграции и набор репозиториев. */
public final class StorageContext implements AutoCloseable {

    private final Path directory;
    private final StorageRepositories repositories;
    private final SqliteDatabase database;

    private StorageContext(Path directory, StorageRepositories repositories, SqliteDatabase database) {
        this.directory = directory;
        this.repositories = repositories;
        this.database = database;
    }

    public static StorageContext openDefault() {
        return open(Paths.get("").toAbsolutePath().normalize());
    }

    public static StorageContext open(Path directory) {
        Path normalized = directory.toAbsolutePath().normalize();
        int version = StorageMigrations.migrate(normalized);
        if (version >= 2) {
            Path databaseFile = normalized.resolve(SqliteDatabase.FILE);
            if (!Files.isRegularFile(databaseFile)) {
                throw new StorageException(SqliteDatabase.FILE,
                        "SQLite-база не найдена после миграции");
            }
            SqliteDatabase database = new SqliteDatabase(databaseFile);
            return new StorageContext(normalized,
                    SqliteStorageRepositories.create(database), database);
        }
        return new StorageContext(normalized, JsonStorageRepositories.open(normalized), null);
    }

    public Path directory() {
        return directory;
    }

    public StorageRepositories repositories() {
        return repositories;
    }

    @Override
    public void close() {
        if (database != null) {
            database.close();
        }
    }
}
