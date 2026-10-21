package com.reminder.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Загрузка шаблона YAML-календаря праздников. */
public final class HolidayTemplate {

    private static final String RESOURCE = "/templates/holidays-template.yaml";

    private HolidayTemplate() {
    }

    public static String content() throws IOException {
        try (InputStream in = HolidayTemplate.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IOException("Шаблон календаря не найден: " + RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
