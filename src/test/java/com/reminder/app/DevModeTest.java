package com.reminder.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevModeTest {

    @Test
    void parsesFileWithBom(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("dev.flag");
        Files.writeString(file,
                "\uFEFFupdateFrom=C:\\temp\\a.zip\nupdateVersion=9.9.9\n",
                StandardCharsets.UTF_8);
        Map<String, String> properties = DevMode.parseFlag(file);
        assertEquals("C:\\temp\\a.zip", properties.get("updateFrom"));
        assertEquals("9.9.9", properties.get("updateVersion"));
    }

    @Test
    void parsesFileWithoutBom(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("dev.flag");
        Files.writeString(file, "updateFrom=/tmp/a.zip\n", StandardCharsets.UTF_8);
        assertEquals("/tmp/a.zip", DevMode.parseFlag(file).get("updateFrom"));
    }

    @Test
    void emptyFileGivesEmptyMap(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("dev.flag");
        Files.writeString(file, "", StandardCharsets.UTF_8);
        assertTrue(DevMode.parseFlag(file).isEmpty());
    }

    @Test
    void missingFileGivesEmptyMap(@TempDir Path dir) {
        assertTrue(DevMode.parseFlag(dir.resolve("absent.flag")).isEmpty());
    }
}
