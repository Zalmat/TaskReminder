package com.reminder.app;

import com.reminder.autostart.LauncherPath;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** Перезапуск приложения (доступен только в собранном app-image). */
public final class AppRestart {

    private AppRestart() {
    }

    /** Доступен ли перезапуск (только из собранного app-image). */
    public static boolean isAvailable() {
        return LauncherPath.resolve().isPresent();
    }

    /**
     * Запускает новый экземпляр приложения и освобождает блокировку текущего,
     * чтобы новый экземпляр мог её захватить. Возвращает true, если запуск начался.
     */
    public static boolean restart() {
        Optional<Path> launcher = LauncherPath.resolve();
        if (launcher.isEmpty()) {
            return false;
        }
        SingleInstanceLock.release();
        try {
            new ProcessBuilder(launcher.get().toString()).start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
