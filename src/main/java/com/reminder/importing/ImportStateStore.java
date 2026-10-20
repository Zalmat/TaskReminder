package com.reminder.importing;

import java.nio.file.Path;
import java.util.prefs.Preferences;

/** Хранит путь и sha256 последнего импортированного файла (в Preferences). */
public class ImportStateStore {

    private static final String KEY_SOURCE = "catalogImportSource";
    private static final String KEY_HASH = "catalogImportHash";

    private final Preferences prefs;

    public ImportStateStore() {
        this(Preferences.userRoot().node("com/reminder"));
    }

    public ImportStateStore(Preferences prefs) {
        this.prefs = prefs;
    }

    /** Изменился ли файл с прошлого импорта. */
    public boolean isUnchanged(Path file, String hash) {
        if (file == null || hash == null) {
            return false;
        }
        String path = normalize(file);
        return path.equals(prefs.get(KEY_SOURCE, ""))
                && hash.equalsIgnoreCase(prefs.get(KEY_HASH, ""));
    }

    public void markImported(Path file, String hash) {
        if (file == null || hash == null) {
            return;
        }
        prefs.put(KEY_SOURCE, normalize(file));
        prefs.put(KEY_HASH, hash);
    }

    public void reset() {
        prefs.remove(KEY_SOURCE);
        prefs.remove(KEY_HASH);
    }

    private static String normalize(Path file) {
        return file.toAbsolutePath().normalize().toString();
    }
}
