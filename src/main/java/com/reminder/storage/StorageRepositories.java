package com.reminder.storage;

/** Набор репозиториев одного профиля данных. */
public record StorageRepositories(WorkEntryRepository workEntries,
                                  PredefinedTaskRepository predefinedTasks,
                                  ReminderRepository reminders,
                                  HolidayRepository holidays,
                                  TaskCatalogRepository catalog) {
}
