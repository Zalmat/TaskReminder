package com.reminder.storage;

import com.reminder.model.WorkEntry;

import java.util.ArrayList;
import java.util.List;

/** JSON-реализация хранилища записей. */
public final class JsonWorkEntryRepository implements WorkEntryRepository {

    private static final String FILE = "work_entries.json";

    private final DataStore store;

    public JsonWorkEntryRepository(DataStore store) {
        this.store = store;
    }

    @Override
    public List<WorkEntry> load() {
        return store.loadList(FILE, WorkEntry.class);
    }

    @Override
    public void save(List<WorkEntry> entries) {
        store.saveList(FILE, new ArrayList<>(entries));
    }
}
