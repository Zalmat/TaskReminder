package com.reminder.catalog.importing;

import com.reminder.catalog.ImportResult;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Читает YAML с секцией {@code tasks:} и извлекает имена для каталога:
 * {@code project}, {@code taskName}, {@code type}. Отсутствующие поля просто
 * не попадают в свой набор (никаких значений по умолчанию).
 */
public class YamlCatalogImporter implements CatalogImporter {

    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_ROWS = 50_000;
    private static final int MAX_VALUES_PER_KIND = 20_000;

    @Override
    public ImportResult importFrom(Path file) throws IOException {
        if (Files.size(file) > MAX_FILE_BYTES) {
            throw new IOException("YAML-файл слишком большой (максимум 5 МБ)");
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
            return ImportResult.empty();
        }
        Object tasksValue = root.get("tasks");
        if (!(tasksValue instanceof List<?> rows)) {
            return ImportResult.empty();
        }
        if (rows.size() > MAX_ROWS) {
            throw new IOException("Слишком много строк в YAML (максимум " + MAX_ROWS + ")");
        }

        Set<String> projects = new LinkedHashSet<>();
        Set<String> taskNames = new LinkedHashSet<>();
        Set<String> types = new LinkedHashSet<>();
        int total = 0;
        int skipped = 0;

        for (Object row : rows) {
            total++;
            if (!(row instanceof Map<?, ?> map)) {
                skipped++;
                continue;
            }
            boolean any = false;
            String project = text(map.get("project"));
            if (!project.isEmpty()) {
                projects.add(project);
                any = true;
            }
            String taskName = text(map.get("taskName"));
            if (!taskName.isEmpty()) {
                taskNames.add(taskName);
                any = true;
            }
            String type = text(map.get("type"));
            if (!type.isEmpty()) {
                types.add(type);
                any = true;
            }
            if (!any) {
                skipped++;
            }
        }

        if (projects.size() > MAX_VALUES_PER_KIND || taskNames.size() > MAX_VALUES_PER_KIND
                || types.size() > MAX_VALUES_PER_KIND) {
            throw new IOException("Слишком много значений в YAML (максимум "
                    + MAX_VALUES_PER_KIND + " на категорию)");
        }

        return new ImportResult(new ArrayList<>(projects), new ArrayList<>(taskNames),
                new ArrayList<>(types), total, skipped);
    }

    private static String text(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }
}
