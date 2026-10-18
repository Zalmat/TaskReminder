package com.reminder.service;

import com.reminder.model.UpdateInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateDownloaderTest {

    private static UpdateInfo updateFrom(Path archive, String checksum) {
        return new UpdateInfo("9.9.9", "", "dev", "",
                archive.toString(), archive.getFileName().toString(), checksum, "", true, false);
    }

    @Test
    void sha256OfKnownContent() throws IOException {
        Path file = Files.createTempFile("sha", ".txt");
        Files.writeString(file, "hello", StandardCharsets.UTF_8);
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                UpdateDownloader.sha256(file));
        Files.deleteIfExists(file);
    }

    @Test
    void detectsLocalAndRemote() {
        assertTrue(UpdateDownloader.isLocal("C:\\temp\\a.zip"));
        assertTrue(UpdateDownloader.isLocal("/tmp/a.zip"));
        assertTrue(UpdateDownloader.isLocal("file:///tmp/a.zip"));
        assertFalse(UpdateDownloader.isLocal("https://example.com/a.zip"));
        assertFalse(UpdateDownloader.isLocal("http://example.com/a.zip"));
    }

    @Test
    void downloadsLocalFileWithChecksum(@TempDir Path dir) throws IOException {
        Path source = dir.resolve("TaskReminder-9.9.9-win-x64.zip");
        Files.writeString(source, "archive-bytes", StandardCharsets.UTF_8);
        String checksum = UpdateDownloader.sha256(source);

        Path temp = Files.createTempDirectory("dl-");
        Path result = new UpdateDownloader().download(updateFrom(source, checksum), temp, null);

        assertTrue(Files.exists(result));
        assertEquals("archive-bytes", Files.readString(result, StandardCharsets.UTF_8));
    }

    @Test
    void rejectsWrongChecksum(@TempDir Path dir) throws IOException {
        Path source = dir.resolve("a.zip");
        Files.writeString(source, "bytes", StandardCharsets.UTF_8);
        Path temp = Files.createTempDirectory("dl-");
        assertThrows(IOException.class,
                () -> new UpdateDownloader().download(updateFrom(source, "00"), temp, null));
    }

    @Test
    void acceptsLocalFileWithoutChecksum(@TempDir Path dir) throws IOException {
        Path source = dir.resolve("a.zip");
        Files.writeString(source, "bytes", StandardCharsets.UTF_8);
        Path temp = Files.createTempDirectory("dl-");
        Path result = new UpdateDownloader().download(updateFrom(source, ""), temp, null);
        assertTrue(Files.exists(result));
    }
}
