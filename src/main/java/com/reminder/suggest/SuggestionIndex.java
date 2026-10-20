package com.reminder.suggest;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Неизменяемый снимок подсказок: значения по видам и связи «задача → проекты». */
public final class SuggestionIndex {

    private static final SuggestionIndex EMPTY = new SuggestionIndex(Map.of(), Map.of());

    private final Map<SuggestionKind, List<Suggestion>> values;
    private final Map<String, Set<String>> taskProjects;

    public SuggestionIndex(Map<SuggestionKind, List<Suggestion>> values,
                           Map<String, Set<String>> taskProjects) {
        Map<SuggestionKind, List<Suggestion>> copy = new EnumMap<>(SuggestionKind.class);
        if (values != null) {
            values.forEach((kind, list) -> copy.put(kind,
                    list == null ? List.of() : List.copyOf(list)));
        }
        this.values = copy;

        Map<String, Set<String>> projects = new HashMap<>();
        if (taskProjects != null) {
            taskProjects.forEach((task, set) -> projects.put(task,
                    set == null ? Set.of() : Set.copyOf(set)));
        }
        this.taskProjects = projects;
    }

    public static SuggestionIndex empty() {
        return EMPTY;
    }

    public List<Suggestion> values(SuggestionKind kind) {
        return values.getOrDefault(kind, List.of());
    }

    public Set<String> projectsForTask(String normalizedTask) {
        if (normalizedTask == null) {
            return Set.of();
        }
        return taskProjects.getOrDefault(normalizedTask, Set.of());
    }

    Map<String, Set<String>> taskProjects() {
        return Map.copyOf(taskProjects);
    }
}
