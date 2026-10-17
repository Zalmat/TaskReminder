package com.reminder.ui.util;

import com.reminder.autostart.LauncherPath;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** Открытие папки с программой в системном файловом менеджере. */
public final class ProgramFolder {

    private ProgramFolder() {
    }

    /** Доступно ли открытие папки (только для собранного app-image). */
    public static boolean isAvailable() {
        return LauncherPath.programDirectory().isPresent();
    }

    /** Открывает корень app-image. В dev-режиме сообщает, что недоступно. */
    public static void open() {
        Optional<Path> directory = LauncherPath.programDirectory();
        if (directory.isEmpty()) {
            Dialogs.warn("Недоступно", "Доступно только в собранной версии");
            return;
        }
        Path folder = directory.get();
        if (openWithDesktop(folder)) {
            return;
        }
        if (openWithCommand(folder)) {
            return;
        }
        Dialogs.error("Ошибка", "Не удалось открыть папку с программой");
    }

    private static boolean openWithDesktop(Path folder) {
        try {
            Desktop desktop = Desktop.getDesktop();
            if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.OPEN)) {
                desktop.open(folder.toFile());
                return true;
            }
        } catch (Exception ignored) {
            // перейдём к fallback-команде
        }
        return false;
    }

    private static boolean openWithCommand(Path folder) {
        String os = System.getProperty("os.name", "").toLowerCase();
        String[] command = os.startsWith("windows")
                ? new String[]{"explorer.exe", folder.toString()}
                : new String[]{"xdg-open", folder.toString()};
        try {
            new ProcessBuilder(command).start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}