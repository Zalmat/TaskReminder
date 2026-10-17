package com.reminder.i18n;

import com.reminder.settings.AppSettings;

import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Фасад локализации: текущий язык, строки, подстановки и локаль форматирования.
 * Инициализируется в {@code main()} до старта JavaFX.
 */
public final class I18n {

    private static Map<String, String> current = Map.of();
    private static Map<String, String> english = Map.of();
    private static Map<String, String> base = Map.of();
    private static Locale locale = Locale.forLanguageTag("ru-RU");
    private static String activeCode = "ru";
    private static List<Language> available = List.of();
    private static boolean startupWarning = false;
    private static String startupWarningDetail = "";

    private I18n() {
    }

    /** Инициализация: читает сохранённый язык, применяет локаль по умолчанию. */
    public static void init() {
        initInternal(AppSettings.language());
    }

    /** Инициализация с принудительным языком (для тестов и служебных вызовов). */
    public static void init(String code) {
        initInternal(code);
    }

    private static void initInternal(String wanted) {
        List<Language> all = new LanguageManager().load();
        available = all;

        Map<String, Language> byCode = new java.util.LinkedHashMap<>();
        for (Language lang : all) {
            byCode.put(lang.code(), lang);
        }
        base = byCode.containsKey("ru") ? byCode.get("ru").strings() : Map.of();
        english = byCode.containsKey("en") ? byCode.get("en").strings() : Map.of();

        Language chosen = byCode.get(wanted);
        if (chosen == null || chosen.status() == LanguageStatus.DAMAGED) {
            if (wanted != null && !wanted.isBlank() && !wanted.equals("ru")) {
                startupWarning = true;
                startupWarningDetail = chosen != null ? chosen.source() : wanted;
            }
            chosen = byCode.get("ru");
        }
        if (chosen == null) {
            chosen = all.isEmpty() ? null : all.get(0);
        }
        activeCode = chosen != null ? chosen.code() : "ru";
        current = chosen != null ? chosen.strings() : Map.of();
        locale = localeFor(activeCode);
        Locale.setDefault(locale);
    }

    /** Текущий язык (код). */
    public static String currentCode() {
        return activeCode;
    }

    /** Локаль для форматирования (совпадает с языком интерфейса). */
    public static Locale locale() {
        return locale;
    }

    public static List<Language> available() {
        return available;
    }

    /** Строка по ключу с откатом: текущий → en → ru → сам ключ. */
    public static String get(String key) {
        if (key == null) {
            return "";
        }
        String value = current.get(key);
        if (value == null) {
            value = english.get(key);
        }
        if (value == null) {
            value = base.get(key);
        }
        return value != null ? value : key;
    }

    /** Подстановка: {@code {0}}, {@code {1}} заменяются на строковые представления аргументов. */
    public static String format(String key, Object... args) {
        String pattern = get(key);
        for (int i = 0; i < args.length; i++) {
            pattern = pattern.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return pattern;
    }

    /** Форматирует число с заданным количеством знаков после запятой в локали интерфейса. */
    public static String number(double value, int fractionDigits) {
        return String.format(locale, "%." + fractionDigits + "f", value);
    }

    /** ResourceBundle для FXML ({@code %key}). */
    public static ResourceBundle bundle() {
        return new I18nResourceBundle();
    }

    /** Была ли проблема с сохранённым языком при старте (повреждён/отсутствует). */
    public static boolean hasStartupWarning() {
        return startupWarning;
    }

    public static String startupWarningDetail() {
        return startupWarningDetail;
    }

    /** Все известные ключи (для тестов и отладки). */
    public static Set<String> keys() {
        Set<String> all = new LinkedHashSet<>();
        all.addAll(base.keySet());
        all.addAll(english.keySet());
        all.addAll(current.keySet());
        return all;
    }

    private static Locale localeFor(String code) {
        if ("en".equals(code)) {
            // en-GB: неделя начинается с понедельника, как и в остальных языках проекта.
            return Locale.forLanguageTag("en-GB");
        }
        if ("de".equals(code)) {
            return Locale.forLanguageTag("de-DE");
        }
        if ("bg".equals(code)) {
            return Locale.forLanguageTag("bg-BG");
        }
        return Locale.forLanguageTag("ru-RU");
    }

    /** Адаптер {@link ResourceBundle} для применения строк в FXML. */
    private static final class I18nResourceBundle extends ResourceBundle {
        @Override
        protected Object handleGetObject(String key) {
            return I18n.get(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            return java.util.Collections.enumeration(I18n.keys());
        }
    }
}
