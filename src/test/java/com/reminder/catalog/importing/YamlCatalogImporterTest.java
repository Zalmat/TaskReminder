package com.reminder.catalog.importing;

import com.reminder.catalog.ImportResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YamlCatalogImporterTest {

    @Test
    void extractsNamesAndHandlesMissingFields(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("tasks.yaml");
        Files.writeString(file, """
                tasks:
                  - project: "P"
                    taskName: "T"
                    type: "Разработка"
                  - project: "P2"
                  - taskName: "T2"
                """, StandardCharsets.UTF_8);

        ImportResult result = new YamlCatalogImporter().importFrom(file);

        assertEquals(List.of("P", "P2"), result.projects());
        assertEquals(List.of("T", "T2"), result.taskNames());
        assertEquals(List.of("Разработка"), result.types());
        assertEquals(3, result.totalRows());
        assertEquals(0, result.skipped());
    }

    @Test
    void skipsRowsWithoutKnownFields(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("tasks.yaml");
        Files.writeString(file, """
                tasks:
                  - comment: "no names"
                  - 123
                  - project: "P"
                """, StandardCharsets.UTF_8);

        ImportResult result = new YamlCatalogImporter().importFrom(file);

        assertEquals(List.of("P"), result.projects());
        assertEquals(3, result.totalRows());
        assertEquals(2, result.skipped());
    }

    @Test
    void handlesUtf8Bom(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("tasks.yaml");
        Files.writeString(file, "\uFEFFtasks:\n  - project: \"P\"\n", StandardCharsets.UTF_8);
        assertEquals(List.of("P"), new YamlCatalogImporter().importFrom(file).projects());
    }

    @Test
    void nonMapDocumentReturnsEmpty(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("tasks.yaml");
        Files.writeString(file, "- a\n- b\n", StandardCharsets.UTF_8);
        assertEquals(ImportResult.empty().projects(), new YamlCatalogImporter().importFrom(file).projects());
    }

    @Test
    void invalidYamlThrowsIOException(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("tasks.yaml");
        Files.writeString(file, "tasks: [unclosed", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> new YamlCatalogImporter().importFrom(file));
    }

    @Test
    void rejectsCustomTags(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("tasks.yaml");
        Files.writeString(file, "tasks: !!java.lang.ProcessBuilder [\"calc\"]\n", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> new YamlCatalogImporter().importFrom(file));
    }
}
