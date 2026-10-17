package com.reminder.app;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Защита от запуска второй копии приложения.
 * Берёт эксклюзивную блокировку на файл {@value #LOCK_FILE} в рабочем каталоге.
 * ОС освобождает блокировку при завершении процесса, поэтому "залипших" локов не бывает.
 */
public final class SingleInstanceLock {

    private static final String LOCK_FILE = ".taskreminder.lock";

    private static FileChannel channel;
    private static FileLock lock;

    private SingleInstanceLock() {
    }

    /** Пытается захватить блокировку в текущем рабочем каталоге. */
    public static boolean acquire() {
        return acquire(Paths.get("").toAbsolutePath().normalize());
    }

    /** Пытается захватить блокировку в указанном каталоге (для тестов). */
    static boolean acquire(Path directory) {
        FileChannel candidateChannel = null;
        try {
            Files.createDirectories(directory);
            candidateChannel = FileChannel.open(directory.resolve(LOCK_FILE),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock candidateLock = candidateChannel.tryLock();
            if (candidateLock == null) {
                closeQuietly(candidateChannel);
                return false;
            }
            channel = candidateChannel;
            lock = candidateLock;
            return true;
        } catch (OverlappingFileLockException e) {
            closeQuietly(candidateChannel);
            return false;
        } catch (IOException e) {
            // Не блокируем запуск при проблемах с лок-файлом (fail-open).
            System.err.println("Cannot acquire single-instance lock: " + e.getMessage());
            closeQuietly(candidateChannel);
            return true;
        }
    }

    /** Освобождает блокировку (обычно не требуется — ОС сделает это при выходе). */
    public static void release() {
        if (lock != null) {
            try {
                lock.release();
            } catch (IOException ignored) {
                // нечего делать
            }
            lock = null;
        }
        if (channel != null) {
            closeQuietly(channel);
            channel = null;
        }
    }

    private static void closeQuietly(FileChannel fileChannel) {
        if (fileChannel == null) {
            return;
        }
        try {
            fileChannel.close();
        } catch (IOException ignored) {
            // нечего делать
        }
    }
}