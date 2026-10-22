package com.reminder.storage;

/** Фабрика SQLite-репозиториев с общим соединением. */
public final class SqliteStorageRepositories {

    private SqliteStorageRepositories() {
    }

    public static StorageRepositories create(SqliteDatabase database) {
        return new StorageRepositories(
                new SqliteWorkEntryRepository(database),
                new SqlitePredefinedTaskRepository(database),
                new SqliteReminderRepository(database),
                new SqliteHolidayRepository(database),
                new SqliteTaskCatalogRepository(database));
    }
}
