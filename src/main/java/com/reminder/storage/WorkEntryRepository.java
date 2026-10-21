package com.reminder.storage;

import com.reminder.model.WorkEntry;

import java.util.List;

/** Хранилище записей о работе. */
public interface WorkEntryRepository {

    List<WorkEntry> load();

    void save(List<WorkEntry> entries);
}
