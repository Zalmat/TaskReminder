package com.reminder.storage;

/** Ошибка чтения или записи данных приложения. */
public class StorageException extends RuntimeException {

    private final String fileName;

    public StorageException(String fileName, String message, Throwable cause) {
        super(message, cause);
        this.fileName = fileName;
    }

    public StorageException(String fileName, String message) {
        this(fileName, message, null);
    }

    public String getFileName() {
        return fileName;
    }
}
