package com.reminder.suggest;

import com.reminder.catalog.TaskCatalogService;
import com.reminder.model.WorkEntry;
import com.reminder.service.TaskService;
import com.reminder.settings.AppSettings;
import com.reminder.util.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Фасад подсказок: быстрый снимок каталога, фоновый снимок истории и их объединение.
 * Не знает о JavaFX; вся политика «когда индексировать» — снаружи.
 */
public class SuggestionService {

    private final TaskService taskService;
    private final TaskCatalogService catalog;
    private final ExecutorService executor;

    private volatile SuggestionIndex catalogIndex;
    private volatile SuggestionIndex historyIndex = SuggestionIndex.empty();
    private volatile SuggestionIndex mergedIndex;

    private final Object buildLock = new Object();
    private Pending pending;
    private boolean workerScheduled;

    private volatile boolean building;
    private volatile double progress;
    private volatile long startedAtMillis;
    private volatile long finishedAtMillis;
    private volatile int indexedEntries;

    public SuggestionService(TaskService taskService) {
        this.taskService = taskService;
        this.catalog = taskService.getCatalog();
        this.catalogIndex = SuggestionIndexBuilder.catalog(catalog);
        this.mergedIndex = merge(catalogIndex, historyIndex);
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "suggestion-index");
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
    }

    /** Перечитывает каталог и сразу обновляет объединённый снимок. */
    public void reloadCatalog() {
        catalogIndex = SuggestionIndexBuilder.catalog(catalog);
        synchronized (buildLock) {
            rebuildMerged();
        }
        Log.log("Suggest", "catalog reloaded: projects=" + catalogIndex.values(SuggestionKind.PROJECT).size()
                + ", tasks=" + catalogIndex.values(SuggestionKind.TASK).size());
    }

    /**
     * Запрашивает пересчёт истории. Снимок записей снимается здесь,
     * поэтому вызывать нужно с FX-потока.
     */
    public void requestHistoryRebuild() {
        List<WorkEntry> entries = taskService.getAllEntries();
        int months = AppSettings.suggestionHistoryMonths();
        synchronized (buildLock) {
            pending = new Pending(entries, months);
            if (workerScheduled) {
                return;
            }
            workerScheduled = true;
        }
        executor.submit(this::workerLoop);
    }

    public List<SuggestionMatch> matches(SuggestionKind kind, String query,
                                         String selectedProject, int limit) {
        return SuggestionRanker.rank(mergedIndex, kind, query, selectedProject, limit);
    }

    public List<Suggestion> stats(SuggestionKind kind) {
        return mergedIndex.values(kind);
    }

    /** Статистика типов: каталог + использование в истории (типы из истории не добавляются). */
    public List<Suggestion> typeStats() {
        LocalDate cutoff = historyCutoff();
        Map<String, Integer> uses = new HashMap<>();
        Map<String, LocalDate> lastUsed = new HashMap<>();
        for (WorkEntry entry : taskService.getAllEntries()) {
            if (entry == null || entry.getType() == null) {
                continue;
            }
            LocalDate date = entry.getDate();
            if (cutoff != null && date != null && date.isBefore(cutoff)) {
                continue;
            }
            String key = TextNormalizer.key(entry.getType());
            if (key.isEmpty()) {
                continue;
            }
            uses.merge(key, 1, Integer::sum);
            if (date != null && (lastUsed.get(key) == null || date.isAfter(lastUsed.get(key)))) {
                lastUsed.put(key, date);
            }
        }

        List<Suggestion> result = new ArrayList<>();
        for (String type : catalog.getTypes()) {
            String key = TextNormalizer.key(type);
            if (key.isEmpty()) {
                continue;
            }
            int count = uses.getOrDefault(key, 0);
            result.add(new Suggestion(type, key,
                    count > 0 ? SuggestionSource.BOTH : SuggestionSource.CATALOG,
                    count, lastUsed.get(key)));
        }
        result.sort(Comparator.comparing(Suggestion::normalized));
        return result;
    }

    public boolean isIndexing() {
        return building;
    }

    public double progress() {
        return Math.max(0, Math.min(1, progress));
    }

    public long indexingStartedAtMillis() {
        return startedAtMillis;
    }

    public long lastBuildFinishedAtMillis() {
        return finishedAtMillis;
    }

    public int indexedEntries() {
        return indexedEntries;
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private void workerLoop() {
        try {
            while (true) {
                Pending request;
                synchronized (buildLock) {
                    request = pending;
                    pending = null;
                    if (request == null) {
                        workerScheduled = false;
                        return;
                    }
                }

                building = true;
                startedAtMillis = System.currentTimeMillis();
                progress = 0;

                SuggestionIndex built = SuggestionIndexBuilder.history(
                        request.entries, request.months,
                        (processed, total) -> progress = total == 0 ? 1.0 : (double) processed / total,
                        () -> {
                            synchronized (buildLock) {
                                return pending != null;
                            }
                        });

                synchronized (buildLock) {
                    if (pending != null) {
                        // Пришёл более свежий снимок — результат устарел.
                        continue;
                    }
                    if (built != null) {
                        historyIndex = built;
                        rebuildMerged();
                        indexedEntries = request.entries.size();
                        finishedAtMillis = System.currentTimeMillis();
                        Log.log("Suggest", "history indexed: entries=" + indexedEntries
                                + ", projects=" + built.values(SuggestionKind.PROJECT).size()
                                + ", tasks=" + built.values(SuggestionKind.TASK).size()
                                + ", " + (finishedAtMillis - startedAtMillis) + " ms");
                    }
                    building = false;
                    progress = built != null ? 1.0 : progress;
                    workerScheduled = false;
                    return;
                }
            }
        } catch (Throwable t) {
            Log.log("Suggest", "history indexing failed: " + t.getMessage(), t);
            boolean retry;
            synchronized (buildLock) {
                building = false;
                retry = pending != null;
                workerScheduled = retry;
            }
            if (retry && !executor.isShutdown()) {
                executor.submit(this::workerLoop);
            }
        }
    }

    private void rebuildMerged() {
        mergedIndex = merge(catalogIndex, historyIndex);
    }

    private static SuggestionIndex merge(SuggestionIndex catalogIndex, SuggestionIndex historyIndex) {
        Map<SuggestionKind, List<Suggestion>> values = new EnumMap<>(SuggestionKind.class);
        for (SuggestionKind kind : SuggestionKind.values()) {
            Map<String, Suggestion> byKey = new LinkedHashMap<>();
            for (Suggestion suggestion : catalogIndex.values(kind)) {
                byKey.put(suggestion.normalized(), suggestion);
            }
            for (Suggestion history : historyIndex.values(kind)) {
                Suggestion catalogValue = byKey.get(history.normalized());
                if (catalogValue == null) {
                    byKey.put(history.normalized(), history);
                } else {
                    byKey.put(history.normalized(), new Suggestion(
                            catalogValue.value(), catalogValue.normalized(),
                            SuggestionSource.BOTH, history.uses(), history.lastUsed()));
                }
            }
            List<Suggestion> list = new ArrayList<>(byKey.values());
            list.sort(Comparator.comparing(Suggestion::normalized));
            values.put(kind, list);
        }
        return new SuggestionIndex(values, historyIndex.taskProjects());
    }

    private LocalDate historyCutoff() {
        int months = AppSettings.suggestionHistoryMonths();
        return months > 0 ? LocalDate.now().minusMonths(months) : null;
    }

    private record Pending(List<WorkEntry> entries, int months) {
    }
}
