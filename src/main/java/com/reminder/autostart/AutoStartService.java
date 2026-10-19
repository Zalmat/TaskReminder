package com.reminder.autostart;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.reminder.util.Log;

/**
 * Автозагрузка при входе в систему (только текущий пользователь, без прав администратора).
 * Windows: ключ HKCU\...\Run. Linux: ~/.config/autostart/taskreminder.desktop.
 */
public final class AutoStartService {

    private static final String APP_NAME = "TaskReminder";
    private static final String RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String DESKTOP_FILE = "taskreminder.desktop";
    private static final String MINIMIZED_ARG = "--minimized";

    private AutoStartService() {
    }

    /** Поддерживается ли автозагрузка (ОС + запуск из собранного app-image). */
    public static boolean isSupported() {
        return (isWindows() || isLinux()) && LauncherPath.resolve().isPresent();
    }

    public static boolean isEnabled() {
        if (LauncherPath.resolve().isEmpty()) {
            return false;
        }
        if (isWindows()) {
            return runCommand(List.of("reg", "query", RUN_KEY, "/v", APP_NAME)) == 0;
        }
        if (isLinux()) {
            return Files.isRegularFile(desktopFile());
        }
        return false;
    }

    public static void setEnabled(boolean enabled) {
        Optional<Path> launcher = LauncherPath.resolve();
        if (launcher.isEmpty()) {
            return;
        }
        Log.log("AutoStart", (enabled ? "enable" : "disable") + " launcher=" + launcher.get());
        if (isWindows()) {
            if (enabled) {
                runCommand(List.of("reg", "add", RUN_KEY, "/v", APP_NAME,
                        "/t", "REG_SZ", "/d", windowsRunValue(launcher.get().toString()), "/f"));
            } else {
                runCommand(List.of("reg", "delete", RUN_KEY, "/v", APP_NAME, "/f"));
            }
        } else if (isLinux()) {
            Path file = desktopFile();
            try {
                if (enabled) {
                    Files.createDirectories(file.getParent());
                    Files.writeString(file, desktopEntry(launcher.get().toString()), StandardCharsets.UTF_8);
                } else {
                    Files.deleteIfExists(file);
                }
            } catch (IOException e) {
                Log.log("AutoStart", "change failed: " + e.getMessage(), e);
                System.err.println("Autostart change failed: " + e.getMessage());
            }
        }
    }

    /** Если автозагрузка включена — обновляет в ней путь к текущему лаунчеру. */
    public static void syncPath() {
        if (isEnabled()) {
            setEnabled(true);
        }
    }

    static String windowsRunValue(String launcherPath) {
        return "\"" + launcherPath + "\" " + MINIMIZED_ARG;
    }

    static String desktopEntry(String launcherPath) {
        return "[Desktop Entry]\n"
                + "Type=Application\n"
                + "Name=" + APP_NAME + "\n"
                + "Exec=\"" + launcherPath + "\" " + MINIMIZED_ARG + "\n"
                + "Terminal=false\n"
                + "X-GNOME-Autostart-enabled=true\n";
    }

    static Path desktopFile() {
        return Path.of(System.getProperty("user.home", "."), ".config", "autostart", DESKTOP_FILE);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().startsWith("windows");
    }

    private static boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase().contains("linux");
    }

    private static int runCommand(List<String> command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            process.getInputStream().readAllBytes();
            return process.waitFor();
        } catch (IOException e) {
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }
}