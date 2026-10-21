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

    @SuppressWarnings("unchecked")
    private <T> T load(String fileName, Type type) {
        Path file = resolve(fileName);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            if (json.isBlank()) {
                return null;
            }
            return (T) gson.fromJson(json, type);
        } catch (IOException | RuntimeException e) {
            System.err.println("Cannot read " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    private void save(String fileName, Object value, Type type) {
        Path target = resolve(fileName);
        Path tmp = resolve(fileName + ".tmp");
        try {
            Files.createDirectories(directory);
            String json = gson.toJson(value, type);
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.WRITE)) {
                channel.force(true);
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