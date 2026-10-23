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
     * Запускает новый экземпляр приложения. Блокировка single-instance должна быть
     * освобождена вызывающей стороной до вызова этого метода.
     */
    public static boolean start() {
        Optional<Path> launcher = LauncherPath.resolve();
        if (launcher.isEmpty()) {
            return false;
        }
        ProcessBuilder builder = new ProcessBuilder(launcher.get().toString(), "--restart");
        Path directory = launcher.get().getParent();
        if (directory != null) {
            builder.directory(directory.toFile());
        }
        try {
            builder.start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
