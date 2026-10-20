package com.reminder.suggest;

import com.reminder.catalog.TaskCatalogService;
import com.reminder.model.WorkEntry;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Строит снимки подсказок из каталога (быстро) и истории (порциями). */
public final class SuggestionIndexBuilder {

    private static final int CHUNK_SIZE = 500;
    private static final long CHUNK_PAUSE_MS = 10;

    private SuggestionIndexBuilder() {
    }

    /** Быстрый снимок только из каталога. */
    public static SuggestionIndex catalog(TaskCatalogService catalog) {
        Map<SuggestionKind, List<Suggestion>> values = new EnumMap<>(SuggestionKind.class);
        values.put(SuggestionKind.PROJECT, catalogValues(catalog.getProjects()));
        values.put(SuggestionKind.TASK, catalogValues(catalog.getTaskNames()));
        return new SuggestionIndex(values, Map.of());
    }

    /**
     * Снимок истории за указанное число месяцев (0 — вся история).
     * Возвращает {@code null}, если построение отменено.
     */
    public static SuggestionIndex history(List<WorkEntry> entries, int months,
                                          ProgressListener progress, BooleanSupplier cancelled) {
        if (entries == null || entries.isEmpty()) {
            update(progress, 0, 0);
            return new SuggestionIndex(Map.of(), Map.of());
        }

        LocalDate cutoff = months > 0 ? LocalDate.now().minusMonths(months) : null;
        Map<String, Agg> projects = new HashMap<>();
        Map<String, Agg> tasks = new HashMap<>();
        Map<String, Set<String>> taskProjects = new HashMap<>();

        int total = entries.size();
        for (int i = 0; i < total; i++) {
            WorkEntry entry = entries.get(i);
            if (entry != null) {
                LocalDate date = entry.getDate();
                boolean inWindow = cutoff == null || date == null || !date.isBefore(cutoff);
                if (inWindow) {
                    String project = trim(entry.getProject());
                    String task = trim(entry.getTaskName());
                    if (!project.isEmpty()) {
                        add(projects, project, date);
                    }
                    if (!task.isEmpty()) {
                        add(tasks, task, date);
                    }
                    if (!project.isEmpty() && !task.isEmpty()) {
                        taskProjects.computeIfAbsent(TextNormalizer.key(task), key -> new HashSet<>())
                                .add(TextNormalizer.key(project));
                    }
                }
            }
            int processed = i + 1;
            if (processed % CHUNK_SIZE == 0 || processed == total) {
                if (cancelled != null && cancelled.getAsBoolean()) {
                    return null;
                }
                update(progress, processed, total);
                if (processed < total) {
                    try {
                        Thread.sleep(CHUNK_PAUSE_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        if (cancelled != null && cancelled.getAsBoolean()) {
            return null;
        }

        Map<SuggestionKind, List<Suggestion>> values = new EnumMap<>(SuggestionKind.class);
        values.put(SuggestionKind.PROJECT, toSuggestions(projects));
        values.put(SuggestionKind.TASK, toSuggestions(tasks));
        return new SuggestionIndex(values, taskProjects);
    }

    private static List<Suggestion> catalogValues(List<String> source) {
        Map<String, Suggestion> unique = new LinkedHashMap<>();
        for (String raw : source) {
            String value = trim(raw);
            if (value.isEmpty()) {
                continue;
            }
            String key = TextNormalizer.key(value);
            unique.putIfAbsent(key, new Suggestion(value, key,
                    SuggestionSource.CATALOG, 0, null));
        }
        return new ArrayList<>(unique.values());
    }

    private static List<Suggestion> toSuggestions(Map<String, Agg> aggregates) {
        List<Suggestion> result = new ArrayList<>(aggregates.size());
        aggregates.forEach((key, agg) ->
                result.add(new Suggestion(agg.display, key, SuggestionSource.HISTORY,
                        agg.uses, agg.lastUsed)));
        result.sort((a, b) -> a.normalized().compareTo(b.normalized()));
        return result;
    }

    private static void add(Map<String, Agg> target, String value, LocalDate date) {
        String key = TextNormalizer.key(value);
        Agg agg = target.computeIfAbsent(key, ignored -> new Agg(value));
        agg.uses++;
        if (date != null && (agg.lastUsed == null || date.isAfter(agg.lastUsed))) {
            agg.lastUsed = date;
            agg.display = value;
        }
    }

    private static void update(ProgressListener progress, int processed, int total) {
        if (progress != null) {
            progress.update(processed, total);
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /** Колбэк прогресса: processed из total. */
    public interface ProgressListener {
        void update(int processed, int total);
    }

    private static final class Agg {
        private String display;
        private int uses;
        private LocalDate lastUsed;

        private Agg(String display) {
            this.display = display;
        }
    }
}
