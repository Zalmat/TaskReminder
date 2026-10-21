package com.reminder.catalog;

import com.reminder.storage.DataStore;
import com.reminder.storage.FailingDataStore;
import com.reminder.storage.StorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void removesAddsAndClearsValues(@TempDir Path dir) {
        DataStore store = new DataStore(dir);
        TaskCatalogService catalog = new TaskCatalogService(store);
        catalog.merge(new ImportResult(List.of("P", "P2"), List.of("T"), List.of("Dev"), 2, 0));

        assertTrue(catalog.removeProject("P"));
        assertFalse(catalog.removeProject("P"));
        assertTrue(catalog.removeTaskName("T"));
        assertTrue(catalog.addType("QA"));
        assertFalse(catalog.addType("QA"));

        assertEquals(List.of("P2"), catalog.getProjects());
        assertEquals(List.of(), catalog.getTaskNames());
        assertEquals(List.of("Dev", "QA"), catalog.getTypes());

        TaskCatalogService reloaded = new TaskCatalogService(store);
        assertEquals(List.of("P2"), reloaded.getProjects());
        assertEquals(List.of("Dev", "QA"), reloaded.getTypes());

        assertEquals(1, catalog.clearProjects());
        assertEquals(2, catalog.clearTypes());
        assertEquals(List.of(), catalog.getProjects());
        assertEquals(List.of(), catalog.getTypes());
    }

    @Test
    void addsProjectsAndTasksManually(@TempDir Path dir) {
        DataStore store = new DataStore(dir);
        TaskCatalogService catalog = new TaskCatalogService(store);

        assertTrue(catalog.addProject("P"));
        assertFalse(catalog.addProject(" P "));
        assertTrue(catalog.addTaskName("T"));
        assertFalse(catalog.addTaskName(" "));

        TaskCatalogService reloaded = new TaskCatalogService(store);
        assertEquals(List.of("P"), reloaded.getProjects());
        assertEquals(List.of("T"), reloaded.getTaskNames());

    }

    @Test
    void failedMergeLeavesMemoryUnchanged(@TempDir Path dir) {
        FailingDataStore store = new FailingDataStore(dir);
        store.failSaves(true);
        TaskCatalogService catalog = new TaskCatalogService(store);

        assertThrows(StorageException.class, () -> catalog.merge(new ImportResult(
                List.of("P"), List.of("T"), List.of("Dev"), 1, 0)));

        assertTrue(catalog.getProjects().isEmpty());
        assertTrue(catalog.getTaskNames().isEmpty());
        assertTrue(catalog.getTypes().isEmpty());
        assertFalse(store.exists("task_catalog.json"));
    }
}
