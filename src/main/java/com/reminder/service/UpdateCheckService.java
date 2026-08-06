package com.reminder.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.reminder.model.UpdateInfo;
import com.reminder.util.VersionInfo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** Проверка наличия новых версий через GitHub Releases API. */
public class UpdateCheckService {

    private final HttpClient client;
    private final String currentVersion;
    private final String repository;

    public UpdateCheckService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        VersionInfo info = VersionInfo.load();
        this.currentVersion = info.version();
        this.repository = info.repository();
    }

    /**
     * Асинхронно проверяет последний релиз.
     * Не бросает исключений: при любой ошибке возвращает UpdateInfo.none().
     */
    public CompletableFuture<UpdateInfo> checkLatest() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.github.com/repos/" + repository + "/releases/latest"))
                        .timeout(Duration.ofSeconds(10))
                        .header("User-Agent", "TaskReminder/" + currentVersion)
                        .header("Accept", "application/vnd.github+json")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 404) {
                    return UpdateInfo.none();
                }
                if (response.statusCode() != 200) {
                    return UpdateInfo.none();
                }

                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String tag = json.has("tag_name") ? json.get("tag_name").getAsString() : "";
                String releaseUrl = json.has("html_url") ? json.get("html_url").getAsString() : "";
                String name = json.has("name") ? json.get("name").getAsString() : "";
                String downloadUrl = findWinZipUrl(json);

                String latest = normalize(tag);
                boolean newer = !latest.isEmpty()
                        && latest.compareToIgnoreCase(currentVersion) != 0
                        && compareVersions(latest, normalize(currentVersion)) > 0;

                return new UpdateInfo(latest, releaseUrl, downloadUrl, name, newer);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return UpdateInfo.none();
            } catch (IOException | RuntimeException e) {
                return UpdateInfo.none();
            }
        });
    }

    /** Ищет в списке assets zip c суффиксом -win-x64.zip. */
    private String findWinZipUrl(JsonObject json) {
        if (!json.has("assets")) {
            return "";
        }
        JsonArray assets = json.getAsJsonArray("assets");
        for (JsonElement el : assets) {
            JsonObject asset = el.getAsJsonObject();
            if (asset.has("name") && asset.get("name").getAsString().contains("-win-x64.zip")) {
                if (asset.has("browser_download_url")) {
                    return asset.get("browser_download_url").getAsString();
                }
            }
        }
        return "";
    }

    /** Приводит тег/версию к виду "X.Y.Z": убирает ведущий v и суффикс после '-'. */
    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        if (v.startsWith("v") || v.startsWith("V")) {
            v = v.substring(1);
        }
        int dash = v.indexOf('-');
        if (dash >= 0) {
            v = v.substring(0, dash);
        }
        return v;
    }

    /** Сравнение семантических версий X.Y.Z. Возвращает >0, если a > b. */
    static int compareVersions(String a, String b) {
        String[] pa = (a.isEmpty() ? "0" : a).split("\\.");
        String[] pb = (b.isEmpty() ? "0" : b).split("\\.");
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            int va = parseIntSafe(pa.length > i ? pa[i] : "0");
            int vb = parseIntSafe(pb.length > i ? pb[i] : "0");
            if (va != vb) {
                return Integer.compare(va, vb);
            }
        }
        return 0;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}