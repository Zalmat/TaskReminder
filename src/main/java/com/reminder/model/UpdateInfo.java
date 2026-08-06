package com.reminder.model;

/** Результат проверки наличия новой версии. */
public record UpdateInfo(String version, String releaseUrl, String downloadUrl,
                         String releaseName, boolean newer) {

    /** Пустой результат: релизов нет, ошибка сети или версия актуальна. */
    public static UpdateInfo none() {
        return new UpdateInfo("", "", "", "", false);
    }
}