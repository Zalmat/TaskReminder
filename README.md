# 📋 Учет рабочего времени (В базе создано через ИИ)

Десктопное приложение для учета рабочего времени с поддержкой дневного и недельного режимов.

## 🚀 Основные возможности

### 📅 Дневной режим
- Быстрое добавление задач с указанием часов
- Автоматический контроль дневного лимита (8 часов)
- Редактирование и удаление записей
- Визуальная индикация оставшегося времени

### 📊 Недельный режим
- Обзор всех задач за неделю
- Редактирование часов двойным кликом по ячейке
- Автоматический подсчет итогов по дням и неделе
- Поддержка праздничных дней (нередактируемые)
- Визуальный контроль лимитов (48 часов в неделю)

### ⏰ Напоминания
- Создание напоминаний с указанием времени
- Настройка ежедневного повторения
- Управление активностью напоминаний
- Редактирование и удаление существующих напоминаний

### 🔍 Проверка обновлений
- Кнопка «Проверить обновления» проверяет наличие новых версий через GitHub Releases
- Показывает ссылку на скачивание новой версии

### 📂 Загрузка задач из YAML
- Импорт списка задач из YAML файла
- Автоматическое добавление в список задач
- Кнопка «Скачать шаблон YAML» сохраняет пример файла для заполнения

### 📊 Экспорт данных
- Поддерживаемые форматы: Excel (.xlsx), CSV (.csv), JSON, YAML, XML
- Экспорт за выбранный период
- Автоматическое формирование имени файла с датой

### 🪟 Системный трей
- Окно сворачивается в системный трей кнопкой «минимизировать» (иконка генерируется автоматически)
- Выход — через диалог окна или меню трея («Открыть» / «Выход»)
- Двойной клик по иконке трея разворачивает окно

### 🎨 Настройки и оформление
- Кнопка-меню (☰) в левом нижнем углу → «Настройки»
- Переключаемая тема: Светлая / Тёмная (применяется к окну и всем диалогам)
- «Тип работы по умолчанию» для формы ввода
- Автозапуск при входе в систему (свёрнутым в трей) — в собранной версии (Windows/Linux)
- Кнопка «Открыть папку с программой» (в настройках и в диалоге обновления)
- Настройки сохраняются между запусками

## 🛠️ Технологии

- **Java 17+**
- **JavaFX** - графический интерфейс
- **Gradle** - сборка проекта
- **SnakeYAML** - работа с YAML
- **Apache POI** - экспорт в Excel
- **Gson** - работа с JSON

## 📁 Структура проекта

```
src/main/java/com/reminder/
├── App.java                      # Точка входа
├── model/                        # Модели данных
│   ├── Task.java
│   ├── WorkEntry.java
│   ├── WeekEntry.java
│   ├── Reminder.java
│   └── UpdateInfo.java
├── settings/                     # Пользовательские настройки
│   ├── AppSettings.java          # тема, тип работы по умолчанию (Preferences)
│   └── Theme.java                # enum LIGHT/DARK
├── service/                      # Бизнес-логика
│   ├── TaskService.java
│   ├── WorkTimeService.java
│   ├── ReminderService.java
│   ├── ExportService.java
│   ├── ExportFormat.java         # enum форматов экспорта
│   ├── YamlLoaderService.java
│   └── UpdateCheckService.java
├── storage/                      # Хранение/миграция данных
│   ├── DataStore.java
│   └── LegacyMigrator.java
├── ui/
│   ├── TrayService.java           # Системный трей
│   ├── UpdateCheckUi.java         # UI проверки обновлений
│   ├── controller/                # Контроллеры (FXML)
│   │   ├── MainController.java
│   │   ├── RemindersDialogController.java
│   │   ├── ExportDialogController.java
│   │   ├── HolidaysDialogController.java
│   │   └── SettingsDialogController.java
│   ├── view/                      # Экраны
│   │   ├── DayView.java
│   │   ├── WeekView.java
│   │   └── ReminderUi.java
│   ├── dialog/                    # Диалоги
│   │   └── EntryEditDialog.java
│   ├── theme/                     # Применение темы
│   │   └── ThemeManager.java
│   ├── component/                 # UI-компоненты
│   │   ├── WeekEntryCell.java
│   │   ├── WeekTotalCell.java
│   │   ├── EntryForm.java
│   │   ├── ClockUi.java
│   │   └── EasterEgg.java         # Пасхалка
│   └── util/                      # UI-утилиты
│       ├── DatePickerUtils.java
│       ├── Dialogs.java
│       └── TableCells.java
└── util/                          # Утилиты
    ├── DateUtils.java
    ├── RussianDays.java
    ├── TaskValidationUtils.java
    ├── YamlTemplate.java          # Шаблон YAML из ресурса
    └── VersionInfo.java
```

## 💾 Хранение данных

Все данные хранятся в рабочем каталоге (там, откуда запускается приложение) в виде JSON-файлов в кодировке UTF-8:

| Файл | Назначение |
|------|-----------|
| `work_entries.json`    | Записи учёта времени по дням (дата, проект, задача, тип, часы, комментарий) |
| `predefined_tasks.json`| Справочник ранее введённых задач (проект + задача + тип). Используется для автоподстановки в полях «Проект» и «Задача»; пополняется автоматически при добавлении записей и загрузке из YAML |
| `reminders.json`       | Напоминания |
| `holidays.json`        | Праздничные дни |

Файл `predefined_tasks.json` — справочник ранее введённых задач. Каждая запись содержит `project`, `taskName`, `type` (плюс служебные `hours` и `comment`):

```json
[
  {
    "project": "Основные задачи",
    "taskName": "Работы отдела",
    "type": "Разработка",
    "hours": 0.0,
    "comment": ""
  }
]
```

Файл не нужно редактировать вручную: он пополняется автоматически при добавлении работы (в дневном или недельном режиме) и при загрузке задач из YAML. На его основе строятся списки автоподстановки в полях «Проект» и «Задача». Если при добавлении работы поле «Задача» было пропущено, задача сохраняется с маркером `taskName: "   "` — его можно игнорировать.

- Запись выполняется через временный файл и атомарный перенос (`DataStore`) — данные защищены от повреждения.
- Файлы `.dat` от старых версий автоматически переносятся в JSON при первом запуске (`LegacyMigrator`).
- В релизную сборку файлы данных не попадают — `build-release.ps1` вычищает их из каталога приложения.

## 🚀 Запуск проекта

### Сборка и запуск через Gradle

- Сборка проекта
```
./gradlew clean build
```
- Запуск приложения
```
./gradlew run
```

## 📦 Сборка релиза (портативная версия)

Скрипт `build-release.ps1` собирает app-image, zip-архив и контрольную сумму:

```
.\build-release.ps1
```

Результат в каталоге `build\release\`:

```
build/release/
├── TaskReminder/                     # app-image (можно запускать напрямую)
├── TaskReminder-<версия>-win-x64.zip
└── TaskReminder-<версия>-win-x64.zip.sha256
```

- Версия берётся из git-тега вида `v1.2.3`; если тегов нет — `1.0.0` + короткий хэш коммита; при сборке из ZIP без git-метаданных — `1.0.0-src`.
- JavaFX jmods: берутся из `$env:JAVAFX_JMODS`, затем из `C:\tools\javafx-jmods-*`; если их нет — автоматически скачиваются с сайта Gluon. Нужен только установленный JDK 17.
- В приложение включён модуль проверки обновлений: для публикации новой версии создайте Release в GitHub (`v1.0.1` и т.д.) и прикрепите к нему `TaskReminder-<версия>-win-x64.zip`. Кнопка «Проверить обновления» покажет уведомление о новой версии.
- Готовая сборка предназначена для **Windows x64**.

### 🤖 Автопубликация релизов (GitHub Actions)

Workflow `.github/workflows/release.yml` срабатывает **при пуше тега** `v*`:

1. Проверяет файл заметок `release-notes.md`: если он пуст или равен тексту предыдущего релиза — сборка **не запускается** и Release не создаётся.
2. Собирает проект (gradle + jpackage) и создаёт Release, прикрепляя `*.zip` и `*.sha256`.

Порядок выпуска версии:

```
git add release-notes.md
git commit -m "Release notes v1.2.2"
git tag -a v1.2.2 -m "..."   # или в IDEA: Git → Tag
git push origin main
git push origin v1.2.2        # IDEA: не забыть пушить теги
```

Старые архивы релизов при необходимости удаляются вручную (страховка от переполнения хранилища).

## 📝 Пример YAML файла для загрузки задач


```yaml
tasks:
  - project: "Проект Альфа"
    taskName: "Разработка бэкенда"
    type: "Разработка"
  - project: "Проект Альфа"
    taskName: "Тестирование"
    type: "Тестирование"
  - project: "Проект Бета"
    taskName: "Дизайн интерфейсов"
    type: "Дизайн"
  - project: "Проект Бета"
    taskName: "Встречи с заказчиком"
    type: "Коммуникация"
```

## ⚙️ Системные требования

- **Java 17** или выше
- **Оперативная память**: от 512 MB
- **Операционная система**: Windows, macOS, Linux

## 🎯 Лимиты времени

- **Дневной лимит**: 8 часов
- **Недельный лимит**: 48 часов
- При превышении лимитов значения подсвечиваются красным

## 📋 Управление праздниками

- Добавление/удаление праздничных дней
- Праздничные дни не редактируются в недельном режиме
- Выходные дни (СБ, ВС) редактируются (могут быть рабочими)

## 🤝 Вклад в проект

1. Форкните репозиторий
2. Создайте ветку для ваших изменений (git checkout -b feature/amazing-feature)
3. Закоммитьте изменения (git commit -m 'Add some amazing feature')
4. Запушьте ветку (git push origin feature/amazing-feature)
5. Откройте Pull Request

## 📄 Лицензия

MIT License

Copyright (c) 2026 Zalmat

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## 📞 Контакты

Для вопросов и предложений создавайте Issue в репозитории проекта.
