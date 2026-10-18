package com.reminder.app;

import com.reminder.autostart.LauncherPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Запуск внешнего апдейтера из временной копии (чтобы он не блокировал свои файлы). */
public final class AppAutoUpdater {

    private static final String UPDATER_DIR = "updater";

    private AppAutoUpdater() {
    }

    /** Доступно ли автообновление (есть бинарник апдейтера в собранной версии). */
    public static boolean isAvailable() {
        return updaterBinary().isPresent();
    }

    /**
     * Копирует апдейтер во временный каталог и запускает его с аргументами.
     * Апдейтер дождётся завершения текущего процесса, заменит файлы и перезапустит приложение.
     */
    public static void start(Path archive) throws IOException {
        Path binary = updaterBinary().orElseThrow(() -> new IOException("Апдейтер не найден"));
        Path installDir = LauncherPath.programDirectory()
                .orElseThrow(() -> new IOException("Не удалось определить папку программы"));

        Path tempDir = Files.createTempDirectory("tr-autoupdater-");
        Path copy = tempDir.resolve(binaryName());
        Files.copy(binary, copy, StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.setPosixFilePermissions(copy, PosixFilePermissions.fromString("rwx------"));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Windows: POSIX-биты не поддерживаются — не критично
        }

        String exeName = LauncherPath.resolve()
                .map(path -> path.getFileName().toString())
                .orElse("");
        long pid = ProcessHandle.current().pid();

        List<String> command = new ArrayList<>();
        command.add(copy.toString());
        command.add("-pid");
        command.add(Long.toString(pid));
        command.add("-archive");
        command.add(archive.toString());
        command.add("-dir");
        command.add(installDir.toString());
        command.add("-exe");
        command.add(exeName);

        new ProcessBuilder(command).start();
    }

    private static Optional<Path> updaterBinary() {
        return LauncherPath.programDirectory()
                .map(dir -> dir.resolve(UPDATER_DIR).resolve(binaryName()))
                .filter(Files::isRegularFile);
    }

    private static String binaryName() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.startsWith("windows") ? "autoupdater.exe" : "autoupdater";
    }
}
