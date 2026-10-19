package com.reminder.app;

import com.reminder.autostart.AutoStartService;
import com.reminder.autostart.LauncherPath;
import com.reminder.i18n.I18n;
import com.reminder.settings.AppSettings;
import com.reminder.util.VersionInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Логирование в файл. Включается любым из способов:
 * файл-маркер {@code log.flag} в рабочем каталоге, аргумент {@code --log}
 * или системное свойство {@code -Dreminder.log=true}.
 *
 * <p>Пишет в каталог {@code <рабочий каталог>/logs} (при недоступности — в temp),
 * файл {@code TaskReminder-yyyy-MM-dd_HH-mm-ss.log}. Все выводы {@code System.out/err}
 * перенаправляются в тот же файл (и дублируются в исходные потоки). Реализация
 * «безопасная»: любые сбои логирования не влияют на работу приложения.
 */
public final class LogConfig {

    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final int KEEP_FILES = 100;
    private static final String FILE_PREFIX = "TaskReminder-";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter NAME_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final Object LOCK = new Object();

    private static boolean enabled;
    private static PrintStream originalOut;
    private static Path logsDir;
    private static PrintStream fileStream;
    private static long fileBytes;

    private LogConfig() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** Инициализация до старта приложения (первым делом в main). */
    public static void init(List<String> args) {
        boolean byArg = args != null && args.contains("--log");
        boolean on = byArg || Boolean.getBoolean("reminder.log") || Files.exists(Path.of("log.flag"));
        if (!on) {
            return;
        }
        originalOut = System.out;
        try {
            logsDir = resolveLogsDir();
            if (logsDir == null) {
                return;
            }
            cleanupOldFiles();
            openNewFile();
            enabled = true;
            System.setOut(wrap(originalOut));
            System.setErr(wrap(System.err));
            writeHeader();
            Runtime.getRuntime().addShutdownHook(new Thread(LogConfig::close));
        } catch (Throwable t) {
            // Сбой логирования не должен ломать приложение.
            enabled = false;
        }
    }

    /** Дополнительная информация после инициализации I18n/DevMode. */
    public static void logStartupInfo() {
        if (!enabled) {
            return;
        }
        writeLine("devmode  : " + DevMode.isEnabled());
        writeLine("language : " + I18n.currentCode());
        writeLine("theme    : " + AppSettings.theme().name().toLowerCase(Locale.ROOT));
        writeLine("updater  : " + (AppAutoUpdater.isAvailable() ? "available" : "none"));
        writeLine("autostart: " + autostartStatus());
        writeLine("================================");
    }

    private static String autostartStatus() {
        if (!AutoStartService.isSupported()) {
            return "unsupported";
        }
        return AutoStartService.isEnabled() ? "enabled" : "disabled";
    }

    /** Отметка времени в формате лога. */
    public static String timestamp() {
        return LocalDateTime.now().format(TS);
    }

    /** Записывает строку в файл лога (без отметки времени; её добавляет вызывающий). */
    public static void writeLine(String line) {
        if (!enabled) {
            return;
        }
        synchronized (LOCK) {
            if (fileStream == null) {
                return;
            }
            long size = line.getBytes(StandardCharsets.UTF_8).length + 1;
            if (fileBytes + size > MAX_FILE_BYTES) {
                try {
                    fileStream.flush();
                    fileStream.close();
                    openNewFile();
                } catch (Exception e) {
                    return;
                }
            }
            try {
                fileStream.print(line);
                fileStream.print('\n');
            } catch (Exception ignored) {
                // запись не удалась — не считаем это ошибкой приложения
            }
            fileBytes += size;
        }
    }

    /** Дублирует строку в исходный stdout (для запуска из терминала). */
    public static void mirror(String line) {
        if (originalOut != null) {
            originalOut.println(line);
        }
    }

    public static void close() {
        synchronized (LOCK) {
            if (fileStream != null) {
                try {
                    fileStream.flush();
                    fileStream.close();
                } catch (Exception ignored) {
                    // ничего
                }
                fileStream = null;
            }
        }
    }

    // ==================== внутреннее ====================

    private static PrintStream wrap(PrintStream mirror) {
        return new PrintStream(new LineForwardingStream(mirror), true, StandardCharsets.UTF_8);
    }

    private static Path resolveLogsDir() {
        Path work = Path.of("").toAbsolutePath().resolve("logs");
        if (ensureDir(work)) {
            return work;
        }
        Path tmp = Path.of(System.getProperty("java.io.tmpdir", "."), "TaskReminder-logs");
        if (ensureDir(tmp)) {
            return tmp;
        }
        return null;
    }

    private static boolean ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
            return Files.isDirectory(dir) && Files.isWritable(dir);
        } catch (Exception e) {
            return false;
        }
    }

    private static void cleanupOldFiles() {
        try (Stream<Path> stream = Files.list(logsDir)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith(FILE_PREFIX))
                    .sorted(Comparator.comparingLong(LogConfig::lastModified).reversed())
                    .toList();
            for (int i = KEEP_FILES; i < files.size(); i++) {
                try {
                    Files.deleteIfExists(files.get(i));
                } catch (IOException ignored) {
                    // пропускаем
                }
            }
        } catch (IOException ignored) {
            // каталог недоступен
        }
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static void openNewFile() throws IOException {
        String base = FILE_PREFIX + LocalDateTime.now().format(NAME_TS);
        Path file = logsDir.resolve(base + ".log");
        for (int i = 2; Files.exists(file); i++) {
            file = logsDir.resolve(base + "-" + i + ".log");
        }
        fileStream = new PrintStream(Files.newOutputStream(file), true, StandardCharsets.UTF_8);
        fileBytes = 0;
    }

    private static void writeHeader() {
        VersionInfo version = VersionInfo.load();
        writeLine("================================ TaskReminder log ================================");
        writeLine("started  : " + timestamp());
        writeLine("version  : " + version.version() + " (numeric=" + version.numeric()
                + ", commit=" + version.commit() + ")");
        writeLine("os       : " + System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " (" + System.getProperty("os.arch") + ")");
        writeLine("java     : " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        writeLine("locale   : " + Locale.getDefault());
        writeLine("user.dir : " + Path.of("").toAbsolutePath());
        LauncherPath.programDirectory().ifPresent(dir -> writeLine("app.dir  : " + dir));
        writeLine("logs.dir : " + logsDir);
        writeLine("================================");
    }

    /** OutputStream, который построчно пишет в лог-файл и дублирует в исходный поток. */
    private static final class LineForwardingStream extends OutputStream {

        private final PrintStream mirror;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(256);

        LineForwardingStream(PrintStream mirror) {
            this.mirror = mirror;
        }

        @Override
        public synchronized void write(int b) {
            mirror.write(b);
            if (b == '\n') {
                flushLine();
            } else {
                buffer.write(b);
            }
        }

        @Override
        public synchronized void write(byte[] bytes, int off, int len) {
            mirror.write(bytes, off, len);
            for (int i = 0; i < len; i++) {
                int b = bytes[off + i] & 0xFF;
                if (b == '\n') {
                    flushLine();
                } else {
                    buffer.write(b);
                }
            }
        }

        @Override
        public synchronized void flush() {
            mirror.flush();
            if (buffer.size() > 0) {
                flushLine();
            }
        }

        @Override
        public synchronized void close() {
            flush();
            mirror.close();
        }

        private void flushLine() {
            String line = buffer.toString(StandardCharsets.UTF_8);
            buffer.reset();
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            writeLine("[" + timestamp() + "] " + line);
        }
    }
}
