package com.reminder.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Переносит данные из старых бинарных .dat файлов (пакет com.reminder.models)
 * в новый JSON-формат (пакет com.reminder.model). При переименовании пакетов
 * полные имена классов меняются, поэтому имена сопоставляются вручную.
 */
public final class LegacyMigrator {

    private static final Map<String, String> CLASS_MAP = new HashMap<>();
    static {
        CLASS_MAP.put("com.reminder.models.WorkEntry", "com.reminder.model.WorkEntry");
        CLASS_MAP.put("com.reminder.models.Task", "com.reminder.model.Task");
        CLASS_MAP.put("com.reminder.models.Reminder", "com.reminder.model.Reminder");
        CLASS_MAP.put("com.reminder.models.WeekEntry", "com.reminder.model.WeekEntry");
    }

    private final DataStore store;

    public LegacyMigrator(DataStore store) {
        this.store = store;
    }

    public boolean migrate(String legacyFile, String jsonFile) {
        if (store.exists(jsonFile) || !new File(legacyFile).isFile()) {
            return false;
        }
        Object data;
        try (ObjectInputStream ois = new ClassMappingStream(new FileInputStream(legacyFile))) {
            data = ois.readObject();
        } catch (Exception e) {
            System.err.println("Skipping migration of " + legacyFile + ": " + e.getMessage());
            return false;
        }

        if (data == null) {
            return false;
        }

        if (data instanceof java.util.List<?> list) {
            if (!list.isEmpty()) {
                store.saveList(jsonFile, list);
            }
        }

        // После успешного переноса помечаем старый файл, чтобы не читать его снова.
        try {
            Path legacyPath = new File(legacyFile).toPath();
            Path backup = legacyPath.resolveSibling(legacyFile + ".legacy");
            Files.move(legacyPath, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Не критично — JSON уже записан, старый файл просто останется как есть.
        }
        return true;
    }

    /** ObjectInputStream, который подменяет старые имена классов на новые. */
    private static final class ClassMappingStream extends ObjectInputStream {
        ClassMappingStream(java.io.InputStream in) throws IOException {
            super(in);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            String name = desc.getName();
            String mapped = CLASS_MAP.getOrDefault(name, name);
            try {
                return Class.forName(mapped);
            } catch (ClassNotFoundException e) {
                return super.resolveClass(desc);
            }
        }
    }
}