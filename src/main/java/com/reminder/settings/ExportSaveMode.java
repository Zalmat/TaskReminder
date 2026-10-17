package com.reminder.settings;

/** Режим использования указанной папки для отчётов. */
public enum ExportSaveMode {

    /** Предлагать папку в диалоге сохранения (по умолчанию). */
    SUGGEST("suggest"),
    /** Сохранять всегда в указанную папку без диалога. */
    ALWAYS("always");

    private final String key;

    ExportSaveMode(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static ExportSaveMode fromKeyOrDefault(String key) {
        if (key != null) {
            for (ExportSaveMode mode : values()) {
                if (mode.key.equals(key)) {
                    return mode;
                }
            }
        }
        return SUGGEST;
    }
}
