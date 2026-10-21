package com.reminder.storage;

/** Метаданные профиля данных: версия схемы и дата создания. */
public class StorageMeta {

    public int schemaVersion;
    public String createdAt;

    public StorageMeta() {
    }

    public StorageMeta(int schemaVersion, String createdAt) {
        this.schemaVersion = schemaVersion;
        this.createdAt = createdAt;
    }
}
