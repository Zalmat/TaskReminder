package com.reminder.app;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

import com.reminder.util.Log;

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
        return acquire(Paths.get("").toAbsolutePath().normalize(), Duration.ZERO);
    }

    /**
     * Пытается захватить блокировку в текущем рабочем каталоге, ожидая её освобождения
     * не дольше указанного времени. Нужно для перезапуска приложения.
     */
    public static boolean acquire(Duration timeout) {
        return acquire(Paths.get("").toAbsolutePath().normalize(), timeout);
    }

    /** Пытается захватить блокировку в указанном каталоге (для тестов). */
    static boolean acquire(Path directory) {
        return acquire(directory, Duration.ZERO);
    }

    static boolean acquire(Path directory, Duration timeout) {
        long waitNanos = timeout == null ? 0L : Math.max(0L, timeout.toNanos());
        long deadline = System.nanoTime() + waitNanos;
        while (true) {
            FileChannel candidateChannel = null;
            try {
                Files.createDirectories(directory);
                candidateChannel = FileChannel.open(directory.resolve(LOCK_FILE),
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock candidateLock = candidateChannel.tryLock();
                if (candidateLock != null) {
                    channel = candidateChannel;
                    lock = candidateLock;
                    Log.log("SingleInstanceLock", "acquired");
                    return true;
                }
                closeQuietly(candidateChannel);
            } catch (OverlappingFileLockException e) {
                Log.log("SingleInstanceLock", "another instance is already running");
                closeQuietly(candidateChannel);
                return false;
            } catch (IOException e) {
                // Не блокируем запуск при проблемах с лок-файлом (fail-open).
                Log.log("SingleInstanceLock", "lock error (fail-open): " + e.getMessage());
                System.err.println("Cannot acquire single-instance lock: " + e.getMessage());
                closeQuietly(candidateChannel);
                return true;
            }

            if (System.nanoTime() >= deadline) {
                Log.log("SingleInstanceLock", "another instance is still running");
                return false;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
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