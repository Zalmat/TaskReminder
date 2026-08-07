package com.reminder.model;

/** Результат проверки наличия новой версии. */
public record UpdateInfo(String version, String releaseUrl, String downloadUrl,
                         String releaseName, boolean newer, boolean failed) {

    /** Пустой результат: релизов нет или версия актуальна (не ошибка). */
    public static UpdateInfo none() {
        return new UpdateInfo("", "", "", "", false, false);
    }

    /** Сбой проверки: нет сети, проблемы с TLS/API и т.п. */
    public static UpdateInfo error() {
        return new UpdateInfo("", "", "", "", false, true);
    }
}