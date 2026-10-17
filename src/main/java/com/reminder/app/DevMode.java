package com.reminder.app;

import com.reminder.model.UpdateInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Режим разработки: включает отладочные/демонстрационные функции во время тестирования.
 * Включается любым из способов:
 * <ul>
 *     <li>аргумент запуска {@code --dev} или {@code --updateDemo};</li>
 *     <li>системное свойство {@code -Dreminder.dev=true};</li>
 *     <li>файл-маркер {@code dev.flag} в рабочем каталоге рядом с программой.</li>
 * </ul>
 * В обычном запуске (без всего перечисленного) режим выключен.
 */
public final class DevMode {

    private static final String FLAG_FILE = "dev.flag";
    private static final String DEV_ARG = "--dev";
    private static final String UPDATE_DEMO_ARG = "--updateDemo";

    private static boolean enabled;

    private DevMode() {
    }

    /** Инициализация до старта приложения. */
    public static void init(List<String> args) {
        boolean byArg = args != null && (args.contains(DEV_ARG) || args.contains(UPDATE_DEMO_ARG));
        enabled = byArg || Boolean.getBoolean("reminder.dev") || Files.exists(Path.of(FLAG_FILE));
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** Демо-данные для диалога обновления (показываются только в режиме разработки). */
    public static UpdateInfo demoUpdate() {
        return new UpdateInfo("1.5.0-demo", "https://github.com/Zalmat/TaskReminder/releases",
                "", "TaskReminder demo",
                "Что нового (пример):\n- Мультиязычный интерфейс (ru/en/de/bg)\n"
                        + "- Настройки языка и папки для отчётов\n"
                        + "- Описание релиза раскрывается по ссылке «Что нового»\n"
                        + "- Пункт 4: проверка прокрутки длинного описания\n"
                        + "- Пункт 5\n- Пункт 6\n- Пункт 7\n- Пункт 8\n- Пункт 9\n- Пункт 10\n"
                        + "- Пункт 11\n- Пункт 12\n- Пункт 13\n- Пункт 14\n- Пункт 15\n"
                        + "И длинная строка без пробелов для проверки переноса: "
                        + "aaaaabbbbbcccccdddddeeeeefffffggggghhhhhiiiiijjjjjkkkkklllllmmmmmnnnnnooooo",
                true, false);
    }
}
