package com.reminder.service;

/** Форматы экспорта: подпись для UI, расширение файла и ключ имени фильтра выбора файла. */
public enum ExportFormat {

    EXCEL("Excel (.xlsx)", "xlsx", "format.filter.excel"),
    CSV("CSV (.csv)", "csv", "format.filter.csv"),
    JSON("JSON (.json)", "json", "format.filter.json"),
    YAML("YAML (.yaml)", "yaml", "format.filter.yaml"),
    XML("XML (.xml)", "xml", "format.filter.xml");

    private final String label;
    private final String extension;
    private final String filterNameKey;

    ExportFormat(String label, String extension, String filterNameKey) {
        this.label = label;
        this.extension = extension;
        this.filterNameKey = filterNameKey;
    }

    public String label() {
        return label;
    }

    public String extension() {
        return extension;
    }

    /** Ключ локализованного имени фильтра выбора файла. */
    public String filterNameKey() {
        return filterNameKey;
    }
}
