package com.reminder.importing;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportStateStoreTest {

    @Test
    void tracksPathAndHash() {
        Preferences prefs = Preferences.userRoot().node("com/reminder-test-" + UUID.randomUUID());
        try {
            ImportStateStore state = new ImportStateStore(prefs);
            Path file = Path.of("some", "tasks.yaml");

            assertFalse(state.isUnchanged(file, "h1"));
            state.markImported(file, "h1");
            assertTrue(state.isUnchanged(file, "h1"));
            assertTrue(state.isUnchanged(file, "H1"));
            assertFalse(state.isUnchanged(file, "h2"));
            assertFalse(state.isUnchanged(Path.of("other.yaml"), "h1"));

            state.reset();
            assertFalse(state.isUnchanged(file, "h1"));
        } finally {
            try {
                prefs.removeNode();
            } catch (Exception ignored) {
                // узел тестовых настроек
            }
        }
    }
}
