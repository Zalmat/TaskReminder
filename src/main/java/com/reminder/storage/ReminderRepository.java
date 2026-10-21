package com.reminder.storage;

import com.reminder.model.Reminder;

import java.util.List;

/** Хранилище напоминаний. */
public interface ReminderRepository {

    List<Reminder> load();

    void save(List<Reminder> reminders);
}
