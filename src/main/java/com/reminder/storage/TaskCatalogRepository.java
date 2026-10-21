package com.reminder.storage;

/** Хранилище каталога подстановки. */
public interface TaskCatalogRepository {

    TaskCatalogSnapshot load();

    void save(TaskCatalogSnapshot snapshot);
}
