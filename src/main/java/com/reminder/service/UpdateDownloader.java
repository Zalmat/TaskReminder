package com.reminder.service;

import com.reminder.model.UpdateInfo;
import com.reminder.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Скачивает архив обновления и проверяет контрольную сумму sha256.
 * Поддерживает {@code http(s):} и локальный путь / {@code file:} (удобно для тестов).
 */
public class UpdateDownloader {

    /** Прогресс загрузки; {@code total < 0}, если размер неизвестен. */
    public interface ProgressListener {
        void onProgress(long downloaded, long total);
    }

    private static final int BUFFER = 64 * 1024;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Скачивает архив во временный каталог и проверяет sha256. Возвращает путь к скачанному файлу. */
    public Path download(UpdateInfo update, Path tempDir, ProgressListener listener) throws IOException {
        String url = update.archiveUrl();
        if (url == null || url.isBlank()) {
            throw new IOException("Нет ссылки на архив обновления");
        }
        Files.createDirectories(tempDir);
        String name = update.archiveName() != null && !update.archiveName().isBlank()
                ? update.archiveName() : fileNameFrom(url);
        Path target = tempDir.resolve(sanitize(name));
        Log.log("Update", "download start: " + url);

        boolean local = isLocal(url);
        if (local) {
            Path source = localPath(url);
            long total = Files.size(source);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            if (listener != null) {
                listener.onProgress(total, total);
            }
        } else {
            downloadHttp(url, target, listener);
        }

        verifyChecksum(update, target, local);
        Log.log("Update", "download complete: " + target + " (bytes=" + Files.size(target) + ")");
        return target;
    }

    private void verifyChecksum(UpdateInfo update, Path target, boolean local) throws IOException {
        String expected = resolveChecksum(update);
        if (expected == null || expected.isBlank()) {
            if (local) {
                return; // локальный архив (тестовый режим) — проверять нечем
            }
            throw new IOException("Не удалось получить контрольную сумму обновления");
        }
        String actual = sha256(target);
        if (!actual.equalsIgnoreCase(expected.trim())) {
            Log.log("Update", "checksum mismatch");
            Files.deleteIfExists(target);
            throw new IOException("Контрольная сумма не совпадает");
        }
    }

    private String resolveChecksum(UpdateInfo update) throws IOException {
        if (update.checksum() != null && !update.checksum().isBlank()) {
            return update.checksum();
        }
        String url = update.checksumUrl();
        if (url == null || url.isBlank()) {
            return "";
        }
        String content = fetchText(url);
        String trimmed = content.trim();
        return trimmed.isEmpty() ? "" : trimmed.split("\\s+")[0];
    }

    private String fetchText(String url) throws IOException {
        if (isLocal(url)) {
            return Files.readString(localPath(url));
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "TaskReminder")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Загрузка контрольной суммы прервана", e);
        }
    }

    private void downloadHttp(String url, Path target, ProgressListener listener) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(15))
                .header("User-Agent", "TaskReminder")
                .GET()
                .build();
        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Загрузка прервана", e);
        }
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode());
        }
        long total = response.headers().firstValueAsLong("content-length").orElse(-1);
        try (InputStream in = response.body();
             OutputStream out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[BUFFER];
            long downloaded = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                downloaded += read;
                if (listener != null) {
                    listener.onProgress(downloaded, total);
                }
            }
        }
    }

    /** sha256 файла в hex (нижний регистр). */
    public static String sha256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 недоступен", e);
        }
    }

    public static boolean isLocal(String url) {
        if (url == null) {
            return false;
        }
        String lower = url.toLowerCase();
        return !lower.startsWith("http://") && !lower.startsWith("https://");
    }

    public static Path localPath(String url) {
        if (url.toLowerCase().startsWith("file:")) {
            return Paths.get(URI.create(url));
        }
        return Paths.get(url);
    }

    private static String fileNameFrom(String url) {
        String value = url;
        if (value.toLowerCase().startsWith("file:")) {
            value = localPath(url).getFileName().toString();
        } else {
            int slash = value.lastIndexOf('/');
            if (slash >= 0) {
                value = value.substring(slash + 1);
            }
        }
        return value.isBlank() ? "update-archive" : value;
    }

    private static String sanitize(String name) {
        String value = name.replace('\\', '_').replace('/', '_');
        return value.isBlank() ? "update-archive" : value;
    }
}
