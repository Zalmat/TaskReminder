package com.reminder.storage;

import java.nio.file.Path;
import java.util.List;

/** Тестовый DataStore с управляемой ошибкой записи. */
public class FailingDataStore extends DataStore {

    private boolean failSaves;

    public FailingDataStore(Path directory) {
        super(directory);
    }

    public void failSaves(boolean failSaves) {
        this.failSaves = failSaves;
    }

    @Override
    public <T> void saveObject(String fileName, T value) {
        if (failSaves) {
            throw new StorageException(fileName, "simulated save failure");
        }
        super.saveObject(fileName, value);
    }

    @Override
    public <T> void saveList(String fileName, List<T> list) {
        if (failSaves) {
            throw new StorageException(fileName, "simulated save failure");
        }
        super.saveList(fileName, list);
    }
}
