package com.reminder.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Надёжное JSON-хранилище в кодировке UTF-8.
 * Запись выполняется через временный файл и атомарный перенос — данные защищены от повреждения.
 */
public class DataStore {

    private final Path directory;
    private final Gson gson;

    public DataStore() {
        this(Paths.get("").toAbsolutePath().normalize());
    }

    public DataStore(Path directory) {
        this.directory = directory;
        this.gson = createGson();
    }

    private static Gson createGson() {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class,
                        (JsonSerializer<LocalDate>) (src, t, ctx) -> new JsonPrimitive(src.format(dateFmt)))
                .registerTypeAdapter(LocalDate.class,
                        (JsonDeserializer<LocalDate>) (json, t, ctx) -> LocalDate.parse(json.getAsString(), dateFmt))
                .registerTypeAdapter(LocalTime.class,
                        (JsonSerializer<LocalTime>) (src, t, ctx) -> new JsonPrimitive(src.format(timeFmt)))
                .registerTypeAdapter(LocalTime.class,
                        (JsonDeserializer<LocalTime>) (json, t, ctx) -> LocalTime.parse(json.getAsString(), timeFmt))
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonSerializer<LocalDateTime>) (src, t, ctx) -> new JsonPrimitive(src.format(dateTimeFmt)))
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonDeserializer<LocalDateTime>) (json, t, ctx) ->
                                LocalDateTime.parse(json.getAsString(), dateTimeFmt))
                .create();
    }

    public Path resolve(String fileName) {
        return directory.resolve(fileName);
    }

    public boolean exists(String fileName) {
        return Files.isRegularFile(resolve(fileName));
    }

    public <T> List<T> loadList(String fileName, Class<T> elementType) {
        Type type = TypeToken.getParameterized(List.class, elementType).getType();
        List<T> data = load(fileName, type);
        return data != null ? data : new ArrayList<>();
    }

    public <T> T loadObject(String fileName, Class<T> type) {
        return load(fileName, type);
    }

    public <T> void saveObject(String fileName, T value) {
        if (value == null) {
            return;
        }
        save(fileName, value, value.getClass());
    }

    public <T> void saveList(String fileName, List<T> list) {
        List<T> safe = list != null ? list : new ArrayList<>();
        Type type;
        if (!safe.isEmpty()) {
            type = TypeToken.getParameterized(List.class, safe.get(0).getClass()).getType();
        } else {
            type = TypeToken.getParameterized(List.class, Object.class).getType();
        }
        save(fileName, safe, type);
    }

    private static final String BACKUP_SUFFIX = ".last-good";
    private static final String CORRUPT_SUFFIX = ".corrupt-";

    @SuppressWarnings("unchecked")
    private <T> T load(String fileName, Type type) {
        Path file = resolve(fileName);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return readJson(file, type);
        } catch (IOException | RuntimeException e) {
            return recover(fileName, file, type, e);
        }
    }

    private <T> T recover(String fileName, Path target, Type type, Exception cause) {
        Path backup = resolve(fileName + BACKUP_SUFFIX);
        T recovered = null;
        if (Files.isRegularFile(backup)) {
            try {
                recovered = readJson(backup, type);
            } catch (IOException | RuntimeException backupError) {
                System.err.println("Cannot read backup " + backup.getFileName()
                        + ": " + backupError.getMessage());
            }
        }
        if (recovered == null) {
            throw new StorageException(fileName,
                    "Файл повреждён, резервная копия не найдена: " + fileName, cause);
        }
        try {
            Path corrupt = quarantine(target, fileName);
            restore(backup, target);
            System.err.println("Recovered " + fileName + " from " + backup.getFileName()
                    + "; damaged file moved to " + corrupt.getFileName());
        } catch (IOException e) {
            throw new StorageException(fileName,
                    "Файл повреждён; не удалось восстановить из " + backup.getFileName(), e);
        }
        return recovered;
    }

    @SuppressWarnings("unchecked")
    private <T> T readJson(Path file, Type type) throws IOException {
        String json = Files.readString(file, StandardCharsets.UTF_8);
        if (json.isBlank()) {
            throw new IOException("Файл пуст");
        }
        Object parsed = gson.fromJson(json, type);
        if (parsed == null) {
            throw new IOException("JSON содержит null");
        }
        return (T) parsed;
    }

    private Path quarantine(Path target, String fileName) throws IOException {
        String base = fileName + CORRUPT_SUFFIX
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
        Path corrupt = resolve(base);
        for (int i = 2; Files.exists(corrupt); i++) {
            corrupt = resolve(base + "-" + i);
        }
        try {
            Files.move(target, corrupt, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(target, corrupt);
        }
        return corrupt;
    }

    private void restore(Path backup, Path target) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".restore.tmp");
        try {
            Files.copy(backup, tmp, StandardCopyOption.REPLACE_EXISTING);
            force(tmp);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            deleteQuietly(tmp);
        }
    }

    private void copyAtomic(Path source, Path target) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.copy(source, tmp, StandardCopyOption.REPLACE_EXISTING);
            force(tmp);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            deleteQuietly(tmp);
        }
    }

    private static void force(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    private void save(String fileName, Object value, Type type) {
        Path target = resolve(fileName);
        Path tmp = resolve(fileName + ".tmp");
        Path backup = resolve(fileName + BACKUP_SUFFIX);
        try {
            Files.createDirectories(directory);
            String json = gson.toJson(value, type);
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            force(tmp);

            if (Files.isRegularFile(target)) {
                boolean validTarget = true;
                try {
                    readJson(target, type);
                } catch (IOException | RuntimeException invalidTarget) {
                    validTarget = false;
                    System.err.println("Existing " + fileName + " is not valid; backup not updated: "
                            + invalidTarget.getMessage());
                }
                if (validTarget) {
                    try {
                        copyAtomic(target, backup);
                    } catch (IOException backupError) {
                        System.err.println("Cannot update backup " + backup.getFileName()
                                + ": " + backupError.getMessage());
                    }
                }
            }

            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                throw new StorageException(fileName,
                        "Атомарная замена файла не поддерживается: " + fileName, e);
            }
        } catch (IOException | RuntimeException e) {
            deleteQuietly(tmp);
            if (e instanceof StorageException storageException) {
                throw storageException;
            }
            throw new StorageException(fileName, "Не удалось сохранить " + fileName, e);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Временный файл не критичен для целостности основного файла.
        }
    }
}