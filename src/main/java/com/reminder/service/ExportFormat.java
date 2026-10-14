package com.reminder.service;

/** Форматы экспорта: подпись для UI, расширение файла и имя фильтра выбора файла. */
public enum ExportFormat {

    EXCEL("Excel (.xlsx)", "xlsx", "Excel файлы"),
    CSV("CSV (.csv)", "csv", "CSV файлы"),
    JSON("JSON (.json)", "json", "JSON файлы"),
    YAML("YAML (.yaml)", "yaml", "YAML файлы"),
    XML("XML (.xml)", "xml", "XML файлы");

    private final String label;
    private final String extension;
    private final String filterName;

    ExportFormat(String label, String extension, String filterName) {
        this.label = label;
        this.extension = extension;
        this.filterName = filterName;
    }

    public String label() {
        return label;
    }

    public String extension() {
        return extension;
    }

    public String filterName() {
        return filterName;
    }

    /** Возвращает формат по подписи; при null или неизвестной подписи — {@link #EXCEL}. */
    public static ExportFormat fromLabel(String label) {
        if (label != null) {
            for (ExportFormat format : values()) {
                if (format.label.equals(label)) {
                    return format;
                }
            }
        }
        return EXCEL;
    }
}