package com.reminder.storage;

import java.util.List;

/** Снимок каталога, независимый от формата хранения. */
public record TaskCatalogSnapshot(List<String> projects, List<String> taskNames, List<String> types) {

    public TaskCatalogSnapshot {
        projects = projects == null ? List.of() : List.copyOf(projects);
        taskNames = taskNames == null ? List.of() : List.copyOf(taskNames);
        types = types == null ? List.of() : List.copyOf(types);
    }
}
