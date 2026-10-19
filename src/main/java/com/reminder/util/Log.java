package com.reminder.util;

import com.reminder.app.LogConfig;

/**
 * Фасад логирования: {@code [время] [тег] (поток) сообщение}.
 * Безопасен при выключенном логировании (просто ничего не делает).
 */
public final class Log {

    private Log() {
    }

    public static void log(String tag, String message) {
        write(tag, message, null, null);
    }

    public static void log(String tag, String message, Throwable error) {
        write(tag, message, error, null);
    }

    /** Сообщение с идентификатором сценария (например, потока обновления). */
    public static void log(String tag, String message, String scenarioId) {
        write(tag, message, null, scenarioId);
    }

    public static void log(String tag, String message, Throwable error, String scenarioId) {
        write(tag, message, error, scenarioId);
    }

    private static void write(String tag, String message, Throwable error, String scenarioId) {
        if (!LogConfig.isEnabled()) {
            return;
        }
        String head = "[" + LogConfig.timestamp() + "] [" + tag
                + (scenarioId != null && !scenarioId.isBlank() ? "#" + scenarioId : "")
                + "] (" + Thread.currentThread().getName() + ") " + message;
        LogConfig.writeLine(head);
        LogConfig.mirror(head);
        if (error != null) {
            for (StackTraceElement element : error.getStackTrace()) {
                LogConfig.writeLine("\tat " + element);
            }
            if (error.getCause() != null && error.getCause() != error) {
                LogConfig.writeLine("\tCaused by: " + error.getCause());
            }
        }
    }
}
