package com.reminder.holiday;

import com.reminder.model.Holiday;
import com.reminder.model.HolidaySource;
import com.reminder.model.HolidayType;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Читает YAML-календарь, сформированный внешним парсером. */
public class HolidayImporter {

    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_ROWS = 10_000;
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public record Result(List<Holiday> holidays, int total, int invalid) {
    }

    public Result importFrom(Path file) throws IOException {
        if (Files.size(file) > MAX_FILE_BYTES) {
            throw new IOException("Файл календаря слишком большой (максимум 5 МБ)");
        }
        String content = Files.readString(file, StandardCharsets.UTF_8);
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }

        Object loaded;
        try {
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            options.setMaxAliasesForCollections(50);
            options.setNestingDepthLimit(50);
            options.setCodePointLimit((int) MAX_FILE_BYTES);
            loaded = new Yaml(new SafeConstructor(options)).load(content);
        } catch (RuntimeException e) {
            throw new IOException("Некорректный YAML: " + e.getMessage(), e);
        }
        if (!(loaded instanceof Map<?, ?> root)) {
            throw new IOException("Ожидался объект с секцией holidays");
        }
        Object rowsValue = root.get("holidays");
        if (!(rowsValue instanceof List<?> rows)) {
            throw new IOException("В файле нет секции holidays");
        }
        if (rows.size() > MAX_ROWS) {
            throw new IOException("Слишком много строк в календаре (максимум " + MAX_ROWS + ")");
        }

        List<Holiday> holidays = new ArrayList<>();
        int invalid = 0;
        for (Object row : rows) {
            Holiday holiday = parseRow(row);
            if (holiday == null) {
                invalid++;
            } else {
                holidays.add(holiday);
            }
        }
        return new Result(holidays, rows.size(), invalid);
    }

    private Holiday parseRow(Object row) {
        if (!(row instanceof Map<?, ?> map)) {
            return null;
        }
        LocalDate date = parseDate(text(map.get("date")));
        String name = text(map.get("name")).trim();
        if (date == null || name.isEmpty()) {
            return null;
        }
        Boolean annual = parseBoolean(map.get("annual"));
        if (annual == null) {
            return null;
        }
        HolidayType type = HolidayType.OFFICIAL;
        Object rawType = map.get("type");
        if (rawType != null && !text(rawType).isBlank()) {
            type = HolidayType.fromCode(text(rawType));
            if (type == null) {
                return null;
            }
        }
        return new Holiday(date, name, annual, type, HolidaySource.IMPORTED);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(value.trim(), DISPLAY_DATE);
            } catch (DateTimeParseException e) {
                return null;
            }
        }
    }

    private Boolean parseBoolean(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = text(value).trim();
        if (text.isEmpty()) {
            return true;
        }
        if (text.equalsIgnoreCase("true")) {
            return true;
        }
        if (text.equalsIgnoreCase("false")) {
            return false;
        }
        return null;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
