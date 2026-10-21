package com.reminder.storage;

import com.reminder.model.Task;

import java.util.List;

/** Хранилище предопределённых задач для подсказок. */
public interface PredefinedTaskRepository {

    List<Task> load();

    void save(List<Task> tasks);
}
