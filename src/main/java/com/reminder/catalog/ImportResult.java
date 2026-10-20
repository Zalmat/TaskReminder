package com.reminder.catalog;

import java.util.List;

/** Имена, полученные из источника (YAML и т.п.). */
public record ImportResult(List<String> projects, List<String> taskNames, List<String> types,
                           int totalRows, int skipped) {

    public static ImportResult empty() {
        return new ImportResult(List.of(), List.of(), List.of(), 0, 0);
    }

    public boolean isEmpty() {
        return projects.isEmpty() && taskNames.isEmpty() && types.isEmpty();
    }
}
