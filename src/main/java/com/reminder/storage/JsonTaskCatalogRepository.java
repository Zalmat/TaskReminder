package com.reminder.storage;

import java.util.ArrayList;

/** JSON-реализация хранилища каталога подстановки. */
public final class JsonTaskCatalogRepository implements TaskCatalogRepository {

    private static final String FILE = "task_catalog.json";

    private final DataStore store;

    public JsonTaskCatalogRepository(DataStore store) {
        this.store = store;
    }

    @Override
    public TaskCatalogSnapshot load() {
        TaskCatalogData data = store.loadObject(FILE, TaskCatalogData.class);
        if (data == null) {
            return new TaskCatalogSnapshot(null, null, null);
        }
        return new TaskCatalogSnapshot(data.projects, data.taskNames, data.types);
    }

    @Override
    public void save(TaskCatalogSnapshot snapshot) {
        TaskCatalogData data = new TaskCatalogData();
        data.projects = new ArrayList<>(snapshot.projects());
        data.taskNames = new ArrayList<>(snapshot.taskNames());
        data.types = new ArrayList<>(snapshot.types());
        store.saveObject(FILE, data);
    }
}
