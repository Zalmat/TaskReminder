package com.reminder.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/** Полный снимок данных перед миграцией и его восстановление. */
final class StorageBackup {

    private static final String BACKUP_DIR = "backups";
    private static final String MANIFEST = "manifest.txt";
    private static final int KEEP_BACKUPS = 5;
    private static final Set<String> CORE_FILES = Set.of(
            "work_entries.json",
            "predefined_tasks.json",
            "reminders.json",
            "holidays.json",
            "holidays.json.bak",
            "task_catalog.json",
            "storage.meta.json");

    private StorageBackup() {
    }

    static Path create(Path dataDir, int fromVersion) {
        Path root = dataDir.resolve(BACKUP_DIR);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String base = "pre-migration-v" + fromVersion + "-" + timestamp;
        try {
            Files.createDirectories(root);
            Path backup = root.resolve(base);
            for (int i = 2; Files.exists(backup); i++) {
                backup = root.resolve(base + "-" + i);
            }
            Files.createDirectory(backup);

            List<String> names = new ArrayList<>();
            try (Stream<Path> stream = Files.list(dataDir)) {
                List<Path> files = stream
                        .filter(Files::isRegularFile)
                        .filter(StorageBackup::isDataFile)
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .toList();
                for (Path file : files) {
                    String name = file.getFileName().toString();
                    Files.copy(file, backup.resolve(name));
                    names.add(name);
                }
            }
            Files.write(backup.resolve(MANIFEST), names, StandardCharsets.UTF_8);
            return backup;
        } catch (IOException e) {
            throw new StorageException("storage.meta.json",
                    "Не удалось создать backup перед миграцией", e);
        }
    }

    static void restore(Path dataDir, Path backup) throws IOException {
        Path manifest = backup.resolve(MANIFEST);
        if (!Files.isRegularFile(manifest)) {
            throw new IOException("Backup manifest not found: " + manifest);
        }
        Set<String> names = new HashSet<>(Files.readAllLines(manifest, StandardCharsets.UTF_8));
        try (Stream<Path> stream = Files.list(dataDir)) {
            List<Path> toDelete = stream
                    .filter(Files::isRegularFile)
                    .filter(StorageBackup::isDataFile)
                    .filter(path -> !names.contains(path.getFileName().toString()))
                    .toList();
            for (Path path : toDelete) {
                Files.deleteIfExists(path);
            }
        }
        for (String name : names) {
            Path source = backup.resolve(name);
            if (!Files.isRegularFile(source)) {
                throw new IOException("Backup file not found: " + source);
            }
            Files.copy(source, dataDir.resolve(name),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static void prune(Path dataDir) {
        Path root = dataDir.resolve(BACKUP_DIR);
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> stream = Files.list(root)) {
            List<Path> backups = stream
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("pre-migration-"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
            for (int i = 0; i < backups.size() - KEEP_BACKUPS; i++) {
                deleteRecursively(backups.get(i));
            }
        } catch (IOException ignored) {
            // Чистка backup не должна мешать успешной миграции.
        }
    }

    private static boolean isDataFile(Path path) {
        String name = path.getFileName().toString();
        return CORE_FILES.contains(name)
                || name.equals(SqliteDatabase.FILE)
                || name.startsWith(SqliteDatabase.FILE + "-")
                || name.equals(SqliteDatabase.FILE + ".new")
                || name.endsWith(".last-good")
                || name.endsWith(".dat")
                || name.endsWith(".dat.legacy")
                || name.contains(".corrupt-");
    }

    private static void deleteRecursively(Path path) throws IOException {
        try (Stream<Path> stream = Files.walk(path)) {
            List<Path> files = stream.sorted(Comparator.reverseOrder()).toList();
            for (Path file : files) {
                Files.deleteIfExists(file);
            }
        }
    }
}
