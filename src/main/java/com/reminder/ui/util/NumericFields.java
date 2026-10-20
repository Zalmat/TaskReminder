package com.reminder.ui.util;

import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import java.util.Locale;

/** Ограничения ввода для числовых полей и спиннеров. */
public final class NumericFields {

    private NumericFields() {
    }

    /** Разрешает только цифры, не более {@code maxDigits}. */
    public static void digitsOnly(TextField field, int maxDigits) {
        String pattern = "\\d{0," + maxDigits + "}";
        field.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches(pattern) ? change : null));
    }

    /**
     * Спиннер времени: не более двух цифр, значение не больше {@code max},
     * отображение с ведущим нулём (04, 05).
     */
    public static void timeSpinner(Spinner<Integer> spinner, int max) {
        spinner.setEditable(true);
        spinner.getEditor().setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getControlNewText();
            if (text.isEmpty()) {
                return change;
            }
            if (!text.matches("\\d{0,2}")) {
                return null;
            }
            return Integer.parseInt(text) <= max ? change : null;
        }));
        spinner.getValueFactory().setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : String.format(Locale.ROOT, "%02d", value);
            }

            @Override
            public Integer fromString(String text) {
                if (text == null || text.isBlank()) {
                    return spinner.getValue();
                }
                try {
                    int value = Integer.parseInt(text.trim());
                    return Math.max(0, Math.min(max, value));
                } catch (NumberFormatException e) {
                    return spinner.getValue();
                }
            }
        });
    }

    /** Спиннер целых чисел: только цифры, не более {@code maxDigits}. */
    public static void numberSpinner(Spinner<Integer> spinner, int maxDigits) {
        spinner.setEditable(true);
        spinner.getEditor().setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0," + maxDigits + "}") ? change : null));
        SpinnerValueFactory<Integer> factory = spinner.getValueFactory();
        if (factory instanceof SpinnerValueFactory.IntegerSpinnerValueFactory integerFactory) {
            int min = integerFactory.getMin();
            int max = integerFactory.getMax();
            factory.setConverter(new StringConverter<>() {
                @Override
                public String toString(Integer value) {
                    return value == null ? "" : value.toString();
                }

                @Override
                public Integer fromString(String text) {
                    if (text == null || text.isBlank()) {
                        return spinner.getValue();
                    }
                    try {
                        int value = Integer.parseInt(text.trim());
                        return Math.max(min, Math.min(max, value));
                    } catch (NumberFormatException e) {
                        return spinner.getValue();
                    }
                }
            });
        }
    }
}
