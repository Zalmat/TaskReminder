package com.reminder.autostart;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Определяет путь к лаунчеру приложения (TaskReminder.exe / TaskReminder) в app-image. */
public final class LauncherPath {

    private LauncherPath() {
    }

    /** Путь к лаунчеру текущего запуска или пусто, если приложение запущено не из app-image. */
    public static Optional<Path> resolve() {
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            Path path = Path.of(appPath);
            if (Files.isRegularFile(path)) {
                return Optional.of(path);
            }
        }
        Optional<Path> derived = resolveLauncher(
                Path.of(System.getProperty("java.home", "")),
                System.getProperty("os.name", ""));
        if (derived.isPresent() && Files.isRegularFile(derived.get())) {
            return derived;
        }
        return Optional.empty();
    }

    /**
     * Вычисляет путь к лаунчеру по структуре app-image (без проверки существования).
     * Windows: {@code <app>/runtime} → {@code <app>/<name>.exe}.
     * Linux: {@code <app>/lib/runtime} → {@code <app>/bin/<name>}.
     * Для обычного JDK (dev) возвращает пусто.
     */
    static Optional<Path> resolveLauncher(Path javaHome, String osName) {
        if (javaHome == null || osName == null) {
            return Optional.empty();
        }
        String os = osName.toLowerCase();
        if (os.startsWith("windows")) {
            if (!"runtime".equalsIgnoreCase(fileName(javaHome))) {
                return Optional.empty();
            }
            Path appDir = javaHome.getParent();
            if (appDir == null) {
                return Optional.empty();
            }
            String name = fileName(appDir);
            if (name.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(appDir.resolve(name + ".exe"));
        }

        if (os.contains("linux")) {
            if (!"runtime".equals(fileName(javaHome))) {
                return Optional.empty();
            }
            Path lib = javaHome.getParent();
            if (lib == null || !"lib".equals(fileName(lib))) {
                return Optional.empty();
            }
            Path appDir = lib.getParent();
            if (appDir == null) {
                return Optional.empty();
            }
            String name = fileName(appDir);
            if (name.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(appDir.resolve("bin").resolve(name));
        }

        return Optional.empty();
    }

    private static String fileName(Path path) {
        Path name = path.getFileName();
        return name == null ? "" : name.toString();
    }
}