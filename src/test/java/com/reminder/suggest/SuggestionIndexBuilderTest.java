package com.reminder.suggest;

import com.reminder.catalog.ImportResult;
import com.reminder.catalog.TaskCatalogService;
import com.reminder.model.WorkEntry;
import com.reminder.storage.DataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuggestionIndexBuilderTest {

    @Test
    void catalogSnapshotUsesCatalogSource(@TempDir java.nio.file.Path dir) {
        TaskCatalogService catalog = new TaskCatalogService(new DataStore(dir));
        catalog.merge(new ImportResult(List.of("P"), List.of("T"), List.of("Dev"), 1, 0));

        SuggestionIndex index = SuggestionIndexBuilder.catalog(catalog);

        assertEquals(1, index.values(SuggestionKind.PROJECT).size());
        assertEquals(SuggestionSource.CATALOG,
                index.values(SuggestionKind.PROJECT).get(0).source());
        assertEquals(0, index.values(SuggestionKind.PROJECT).get(0).uses());
    }

    @Test
    void historyAggregatesUsesAndLinksTasksToProjects() {
        LocalDate recent = LocalDate.now().minusMonths(1);
        LocalDate older = LocalDate.now().minusMonths(2);
        List<WorkEntry> entries = List.of(
                entry("P1", "T1", "Dev", recent),
                entry("P1", "T1", "Dev", older));

        SuggestionIndex index = SuggestionIndexBuilder.history(entries, 12, (a, b) -> { }, () -> false);

        Suggestion project = index.values(SuggestionKind.PROJECT).get(0);
        Suggestion task = index.values(SuggestionKind.TASK).get(0);
        assertEquals(2, project.uses());
        assertEquals(recent, project.lastUsed());
        assertEquals(2, task.uses());
        assertEquals(SuggestionSource.HISTORY, task.source());
        assertTrue(index.projectsForTask("t1").contains("p1"));
    }

    @Test
    void oldEntriesAreExcludedByHistoryWindow() {
        LocalDate now = LocalDate.now();
        List<WorkEntry> entries = List.of(
                entry("P1", "T1", "Dev", now.minusMonths(1)),
                entry("P2", "T2", "Dev", now.minusMonths(30)));

        SuggestionIndex recent = SuggestionIndexBuilder.history(entries, 12, (a, b) -> { }, () -> false);
        assertEquals(1, recent.values(SuggestionKind.PROJECT).size());
        assertEquals("P1", recent.values(SuggestionKind.PROJECT).get(0).value());

        SuggestionIndex all = SuggestionIndexBuilder.history(entries, 0, (a, b) -> { }, () -> false);
        assertEquals(2, all.values(SuggestionKind.PROJECT).size());
    }

    @Test
    void reportsProgressAndHonoursCancellation() {
        List<WorkEntry> entries = List.of(
                entry("P1", "T1", "Dev", LocalDate.now()),
                entry("P2", "T2", "Dev", LocalDate.now()));
        AtomicInteger updates = new AtomicInteger();
        SuggestionIndex index = SuggestionIndexBuilder.history(entries, 12,
                (processed, total) -> updates.incrementAndGet(), () -> false);
        assertEquals(2, index.values(SuggestionKind.PROJECT).size());
        assertTrue(updates.get() > 0);

        assertNull(SuggestionIndexBuilder.history(entries, 12, (a, b) -> { }, () -> true));
    }

    private static WorkEntry entry(String project, String task, String type, LocalDate date) {
        WorkEntry entry = new WorkEntry(project, task, type, 1);
        entry.setDate(date);
        return entry;
    }
}
