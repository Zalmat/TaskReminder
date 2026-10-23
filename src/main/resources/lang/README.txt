Языковые пакеты TaskReminder
============================

В этой папке лежат внешние языковые пакеты. Приложение читает их при запуске
и показывает найденные языки в разделе «Настройки → Язык».

Как добавить новый язык
-----------------------
1. Скопируйте любой файл вида de_lang.yaml в новый, например fr_lang.yaml.
2. В поле _language укажите код языка (fr), в _name — название (Français).
3. Переведите строки в секции strings. Ключи менять нельзя.
4. Перезапустите приложение — язык появится в списке.

Формат файла
------------
_language: fr
_name: Français
strings:
  app.title: "TaskReminder"
  main.total: "Затрачено: {0} ч"

Правила
-------
- Кодировка: UTF-8.
- Значения со скобками {0} обязательно брать в кавычки.
- Если файл повреждён (ошибка YAML) — он не применяется и помечается как «Повреждён».
- Если часть ключей отсутствует — язык помечается как «Неполный (N/M)»
  и берёт недостающие строки из английского/русского.
- Ключи `settings.language.source.builtin` и `settings.language.source.file`
  необязательны: они используются только в подсказке к языку.
- Если внешний файл имеет код встроенного языка, его строки переопределяют
  встроенные, а отсутствующие ключи берутся из встроенного пакета.
- Локаль для форматирования чисел/дат определяется по `_language`.

Имя файла должно заканчиваться на _lang.yaml (можно _lang.yml).


============================================================
TaskReminder language packs (English)
============================================================

This folder contains external language packs. The application reads them
at startup and lists the found languages under "Settings → Language".

How to add a new language
-------------------------
1. Copy any file like de_lang.yaml to a new one, for example fr_lang.yaml.
2. Set _language to the language code (fr) and _name to the display name (Français).
3. Translate the strings in the "strings" section. Do not change the keys.
4. Restart the application — the language will appear in the list.

File format
-----------
_language: fr
_name: Français
strings:
  app.title: "TaskReminder"
  main.total: "Logged: {0} h"

Rules
-----
- Encoding: UTF-8.
- Values containing {0} must be quoted.
- If the file is broken (YAML error), it is not applied and is marked as "Damaged".
- If some keys are missing, the language is marked as "Incomplete (N/M)"
  and falls back to English/Russian for the missing strings.
- `settings.language.source.builtin` and `settings.language.source.file`
  are optional; they are used only in the language tooltip.
- If an external file uses the code of a built-in language, its strings override
  the built-in ones and missing keys are filled from the built-in package.
- Number/date formatting locale is derived from `_language`.

The file name must end with _lang.yaml (or _lang.yml).

