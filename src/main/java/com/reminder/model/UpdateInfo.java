package com.reminder.model;

/**
 * Результат проверки наличия новой версии.
 * {@code archiveUrl}/{@code archiveName} — архив под текущую ОС; {@code checksum} — sha256 (hex),
 * может быть пустым, тогда его нужно взять из {@code checksumUrl} (ассет .sha256).
 */
public record UpdateInfo(String version, String releaseUrl, String releaseName, String body,
                         String archiveUrl, String archiveName, String checksum, String checksumUrl,
                         boolean newer, boolean failed) {

    /** Пустой результат: релизов нет или версия актуальна (не ошибка). */
    public static UpdateInfo none() {
        return new UpdateInfo("", "", "", "", "", "", "", "", false, false);
    }

    /** Сбой проверки: нет сети, проблемы с TLS/API и т.п. */
    public static UpdateInfo error() {
        return new UpdateInfo("", "", "", "", "", "", "", "", false, true);
    }

    /** Есть ли архив для автообновления. */
    public boolean hasArchive() {
        return archiveUrl != null && !archiveUrl.isBlank();
    }
}
