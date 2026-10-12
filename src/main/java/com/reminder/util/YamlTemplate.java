package com.reminder.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Содержимое шаблона YAML-файла для загрузки задач (ресурс /templates/tasks-template.yaml). */
public final class YamlTemplate {

    private static final String RESOURCE = "/templates/tasks-template.yaml";

    private YamlTemplate() {
    }

    /** Возвращает текст шаблона. Бросет IOException, если ресурс недоступен. */
    public static String content() throws IOException {
        try (InputStream in = YamlTemplate.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IOException("Шаблон YAML не найден: " + RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}