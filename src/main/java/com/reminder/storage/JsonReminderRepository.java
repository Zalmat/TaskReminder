package com.reminder.storage;

import com.reminder.model.Reminder;

import java.util.ArrayList;
import java.util.List;

/** JSON-реализация хранилища напоминаний. */
public final class JsonReminderRepository implements ReminderRepository {

    private static final String FILE = "reminders.json";

    private final DataStore store;

    public JsonReminderRepository(DataStore store) {
        this.store = store;
    }

    @Override
    public List<Reminder> load() {
        return store.loadList(FILE, Reminder.class);
    }

    @Override
    public void save(List<Reminder> reminders) {
        store.saveList(FILE, new ArrayList<>(reminders));
    }
}
