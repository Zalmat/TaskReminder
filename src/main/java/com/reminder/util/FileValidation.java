package com.reminder.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Проверки файлов, выбираемых пользователем. */
public final class FileValidation {

    private FileValidation() {
    }

    public static boolean isYaml(Path file) {
        if (file == null || file.getFileName() == null) {
            return false;
        }
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".yaml") || name.endsWith(".yml");
    }

    public static boolean isReadableYaml(Path file) {
        return file != null && Files.isRegularFile(file) && Files.isReadable(file) && isYaml(file);
    }
}
