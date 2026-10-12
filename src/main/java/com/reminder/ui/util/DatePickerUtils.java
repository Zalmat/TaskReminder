package com.reminder.ui.util;

import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Единый конвертер дат dd.MM.yyyy для DatePicker. */
public final class DatePickerUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private DatePickerUtils() {
    }

    public static StringConverter<LocalDate> converter() {
        return new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? date.format(DATE_FORMATTER) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.isEmpty()) return null;
                try {
                    return LocalDate.parse(string, DATE_FORMATTER);
                } catch (Exception e) {
                    return null;
                }
            }
        };
    }

    public static void applyTo(DatePicker picker) {
        picker.setConverter(converter());
    }
}