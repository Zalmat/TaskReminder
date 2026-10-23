package com.reminder.i18n;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Загружает языковые пакеты: встроенные из ресурсов jar и внешние из каталога
 * {@code language/} рабочего каталога. Внешний файл с тем же кодом перекрывает встроенный.
 */
public final class LanguageManager {

    /** Коды встроенных языков (лежат в jar). */
    private static final List<String> BUILT_IN = List.of("ru", "en", "de", "bg");

    /** Пакеты, которые дополнительно распаковываются во внешний каталог при первом запуске. */
    private static final List<String> EXTRACT = List.of("de", "bg");

    private static final String LANG_DIR = "language";
    private static final String RESOURCE_DIR = "/lang/";
    private static final String README_FILE = "README.txt";

    private static final String BASE_CODE = "ru";

    /** Технические подсказки источника языка не обязательны для сторонних пакетов. */
    private static final Set<String> OPTIONAL_KEYS = Set.of(
            "settings.language.source.builtin",
            "settings.language.source.file");

    private final Path directory;

    public LanguageManager() {
        this(Path.of(LANG_DIR).toAbsolutePath().normalize());
    }

    public LanguageManager(Path directory) {
        this.directory = directory;
    }

    /** Загружает все пакеты: встроенные + внешние, с вычислением состояния. */
    public List<Language> load() {
        ensureExternalFiles();

        Map<String, RawLanguage> raw = new LinkedHashMap<>();
        for (String code : BUILT_IN) {
            RawLanguage lang = readResource(code);
            if (lang != null) {
                raw.put(code, lang);
            }
        }
        for (RawLanguage lang : readDirectory()) {
            RawLanguage builtIn = raw.get(lang.code);
            raw.put(lang.code, builtIn == null ? lang : merge(builtIn, lang));
        }

        Map<String, String> base = raw.containsKey(BASE_CODE)
                ? raw.get(BASE_CODE).strings
                : Map.of();

        List<Language> result = new ArrayList<>();
        for (RawLanguage lang : raw.values()) {
            result.add(toLanguage(lang, base));
        }
        result.sort(Comparator.comparingInt(l -> order(l.code())));
        return result;
    }

    /** Извлекает во внешний каталог пакеты, которых там ещё нет, и README. */
    public void ensureExternalFiles() {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            return;
        }
        for (String code : EXTRACT) {
            Path target = directory.resolve(code + "_lang.yaml");
            if (Files.exists(target)) {
                continue;
            }
            try (InputStream in = LanguageManager.class.getResourceAsStream(RESOURCE_DIR + code + "_lang.yaml")) {
                if (in != null) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException ignored) {
                // извлечение не критично для запуска
            }
        }
        // README всегда перезаписываем, чтобы инструкция оставалась актуальной.
        Path readme = directory.resolve(README_FILE);
        try (InputStream in = LanguageManager.class.getResourceAsStream(RESOURCE_DIR + README_FILE)) {
            if (in != null) {
                Files.copy(in, readme, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            // не критично
        }
    }

    private RawLanguage readResource(String code) {
        try (InputStream in = LanguageManager.class.getResourceAsStream(RESOURCE_DIR + code + "_lang.yaml")) {
            if (in == null) {
                return null;
            }
            Map<String, Object> root = parse(new InputStreamReader(in, StandardCharsets.UTF_8));
            return build(code, root, "built-in");
        } catch (Exception e) {
            return null;
        }
    }

    private List<RawLanguage> readDirectory() {
        List<RawLanguage> result = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return result;
        }
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                String name = file.getFileName().toString();
                if (!name.endsWith("_lang.yaml") && !name.endsWith("_lang.yml")) {
                    continue;
                }
                String code = name.substring(0, name.indexOf("_lang."));
                result.add(readFile(code, file));
            }
        } catch (IOException ignored) {
            // каталог недоступен — вернём то, что есть
        }
        return result;
    }

    private RawLanguage readFile(String code, Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, Object> root = parse(reader);
            return build(code, root, file.toString());
        } catch (Exception e) {
            return RawLanguage.damaged(code, file.toString(), e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(Reader reader) {
        Object loaded = new Yaml().load(reader);
        if (!(loaded instanceof Map)) {
            throw new IllegalArgumentException("корневой элемент не является словарём");
        }
        return (Map<String, Object>) loaded;
    }

    @SuppressWarnings("unchecked")
    private static RawLanguage build(String fallbackCode, Map<String, Object> root, String source) {
        Object codeValue = root.get("_language");
        String code = codeValue != null ? String.valueOf(codeValue).trim() : fallbackCode;
        Object nameValue = root.get("_name");
        String name = nameValue != null ? String.valueOf(nameValue).trim() : code;
        Object stringsValue = root.get("strings");
        if (!(stringsValue instanceof Map)) {
            return RawLanguage.damaged(code, source, "отсутствует секция strings");
        }
        Map<String, String> strings = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : ((Map<String, Object>) stringsValue).entrySet()) {
            strings.put(String.valueOf(entry.getKey()), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }
        return RawLanguage.ok(code, name, strings, source);
    }

    private static RawLanguage merge(RawLanguage builtIn, RawLanguage external) {
        if (external.status == LanguageStatus.DAMAGED) {
            return external;
        }
        Map<String, String> strings = new LinkedHashMap<>(builtIn.strings);
        strings.putAll(external.strings);
        String name = external.name;
        if (name == null || name.isBlank() || name.equals(external.code)) {
            name = builtIn.name;
        }
        return RawLanguage.ok(external.code, name, strings, external.source);
    }

    private static Language toLanguage(RawLanguage raw, Map<String, String> base) {
        if (raw.status == LanguageStatus.DAMAGED) {
            return new Language(raw.code, raw.name, Map.of(), LanguageStatus.DAMAGED,
                    raw.source, raw.detail, 0, base.size());
        }
        int present = 0;
        int total = 0;
        for (String key : base.keySet()) {
            if (OPTIONAL_KEYS.contains(key)) {
                continue;
            }
            total++;
            if (raw.strings.containsKey(key)) {
                present++;
            }
        }
        LanguageStatus status = (present == total) ? LanguageStatus.OK : LanguageStatus.INCOMPLETE;
        return new Language(raw.code, raw.name, raw.strings, status,
                raw.source, "", present, total);
    }

    private static int order(String code) {
        int idx = BUILT_IN.indexOf(code);
        return idx >= 0 ? idx : BUILT_IN.size();
    }

    /** Внутреннее представление до вычисления состояния. */
    private static final class RawLanguage {
        String code;
        String name;
        Map<String, String> strings = Map.of();
        LanguageStatus status;
        String source;
        String detail;

        static RawLanguage ok(String code, String name, Map<String, String> strings, String source) {
            RawLanguage lang = new RawLanguage();
            lang.code = code;
            lang.name = name;
            lang.strings = strings;
            lang.status = LanguageStatus.OK;
            lang.source = source;
            return lang;
        }

        static RawLanguage damaged(String code, String source, String detail) {
            RawLanguage lang = new RawLanguage();
            lang.code = code;
            lang.name = code;
            lang.status = LanguageStatus.DAMAGED;
            lang.source = source;
            lang.detail = detail;
            return lang;
        }
    }
}
