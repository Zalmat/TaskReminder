package com.reminder.storage;

import com.reminder.model.Task;

import java.util.ArrayList;
import java.util.List;

/** JSON-реализация хранилища предопределённых задач. */
public final class JsonPredefinedTaskRepository implements PredefinedTaskRepository {

    private static final String FILE = "predefined_tasks.json";

    private final DataStore store;

    public JsonPredefinedTaskRepository(DataStore store) {
        this.store = store;
    }

    @Override
    public List<Task> load() {
        return store.loadList(FILE, Task.class);
    }

    @Override
    public void save(List<Task> tasks) {
        store.saveList(FILE, new ArrayList<>(tasks));
    }
}
