package com.reminder.catalog.importing;

import com.reminder.catalog.TaskCatalogService;
import com.reminder.importing.ImportStateStore;
import com.reminder.storage.DataStore;
import com.reminder.storage.FailingDataStore;
import com.reminder.storage.StorageException;
import com.reminder.util.FileHashes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogImportServiceTest {

    @Test
    void skipsUnchangedAndImportsChanged(@TempDir Path dir) throws IOException {
        Preferences prefs = Preferences.userRoot().node("com/reminder-test-" + UUID.randomUUID());
        try {
            TaskCatalogService catalog = new TaskCatalogService(new DataStore(dir.resolve("data")));
            CatalogImportService service = new CatalogImportService(
                    new YamlCatalogImporter(), catalog, new ImportStateStore(prefs));

            Path yaml = dir.resolve("tasks.yaml");
            Files.writeString(yaml, "tasks:\n  - project: \"P\"\n", StandardCharsets.UTF_8);

            ImportOutcome first = service.importFile(yaml, false);
            assertFalse(first.unchangedFile());
            assertEquals(1, first.added().projects());

            ImportOutcome second = service.importFile(yaml, false);
            assertTrue(second.unchangedFile());

            Files.writeString(yaml, "tasks:\n  - project: \"P\"\n  - taskName: \"T\"\n",
                    StandardCharsets.UTF_8);
            ImportOutcome third = service.importFile(yaml, false);
            assertFalse(third.unchangedFile());
            assertEquals(1, third.added().taskNames());

            ImportOutcome forced = service.importFile(yaml, true);
            assertFalse(forced.unchangedFile());
            assertFalse(forced.hasNew());
        } finally {
            try {
                prefs.removeNode();
            } catch (Exception ignored) {
                // временный узел
            }
        }
    }

    @Test
    void failedSaveDoesNotMarkImportState(@TempDir Path dir) throws Exception {
        Preferences prefs = Preferences.userRoot().node("com/reminder-test-" + UUID.randomUUID());
        try {
            FailingDataStore store = new FailingDataStore(dir.resolve("data"));
            store.failSaves(true);
            TaskCatalogService catalog = new TaskCatalogService(store);
            CatalogImportService service = new CatalogImportService(
                    new YamlCatalogImporter(), catalog, new ImportStateStore(prefs));

            Path yaml = dir.resolve("tasks.yaml");
            Files.writeString(yaml, "tasks:\n  - project: \"P\"\n", StandardCharsets.UTF_8);
            String hash = FileHashes.sha256(yaml);

            assertThrows(StorageException.class, () -> service.importFile(yaml, false));

            assertFalse(new ImportStateStore(prefs).isUnchanged(yaml, hash));
            assertTrue(catalog.getProjects().isEmpty());
            assertFalse(store.exists("task_catalog.json"));
        } finally {
            try {
                prefs.removeNode();
            } catch (Exception ignored) {
                // временный узел
            }
        }
    }
}
