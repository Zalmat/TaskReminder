package com.reminder.catalog;

import com.reminder.storage.DataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskCatalogServiceTest {

    @Test
    void mergeAddsAndDeduplicates(@TempDir Path dir) {
        TaskCatalogService catalog = new TaskCatalogService(new DataStore(dir));

        CatalogAddResult first = catalog.merge(new ImportResult(
                List.of("P", "P2", "P"), List.of("T"), List.of("Dev", "Dev"), 2, 0));
        assertEquals(2, first.projects());
        assertEquals(1, first.taskNames());
        assertEquals(1, first.types());

        CatalogAddResult second = catalog.merge(new ImportResult(
                List.of("P"), List.of("T", "T2"), List.of("Dev"), 2, 0));
        assertEquals(0, second.projects());
        assertEquals(1, second.taskNames());
        assertEquals(0, second.types());

        assertEquals(List.of("P", "P2"), catalog.getProjects());
    }

    @Test
    void keepsCaseVariants(@TempDir Path dir) {
        TaskCatalogService catalog = new TaskCatalogService(new DataStore(dir));
        catalog.merge(new ImportResult(List.of("Project", "project"), List.of(), List.of(), 1, 0));
        assertEquals(2, catalog.getProjects().size());
    }

    @Test
    void persistsBetweenInstances(@TempDir Path dir) {
        DataStore store = new DataStore(dir);
        TaskCatalogService first = new TaskCatalogService(store);
        first.merge(new ImportResult(List.of("P"), List.of("T"), List.of("Dev"), 1, 0));

        TaskCatalogService second = new TaskCatalogService(store);
        assertEquals(List.of("P"), second.getProjects());
        assertEquals(List.of("T"), second.getTaskNames());
        assertEquals(List.of("Dev"), second.getTypes());
    }

    @Test
    void seedsDefaultTypesOnlyOnce(@TempDir Path dir) {
        TaskCatalogService catalog = new TaskCatalogService(new DataStore(dir));
        catalog.ensureDefaultTypes(List.of("A", "B"));
        catalog.ensureDefaultTypes(List.of("C"));
        assertEquals(List.of("A", "B"), catalog.getTypes());
    }
}
