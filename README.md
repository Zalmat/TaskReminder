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

### 📊 Экспорт данных
- Поддерживаемые форматы: Excel (.xlsx), JSON, YAML, XML
- Экспорт за выбранный период
- Автоматическое формирование имени файла с датой

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
├── service/                      # Бизнес-логика
│   ├── TaskService.java
│   ├── WorkTimeService.java
│   ├── ReminderService.java
│   ├── ExportService.java
│   ├── YamlLoaderService.java
│   └── UpdateCheckService.java
├── storage/                      # Хранение/миграция данных
│   ├── DataStore.java
│   └── LegacyMigrator.java
├── ui/
│   ├── controller/               # Контроллеры (FXML)
│   │   ├── MainController.java
│   │   ├── RemindersDialogController.java
│   │   ├── ExportDialogController.java
│   │   └── HolidaysDialogController.java
│   └── component/                # UI-компоненты
│       ├── WeekEntryCell.java
│       └── WeekTotalCell.java
└── util/                         # Утилиты
    ├── DateUtils.java
    ├── RussianDays.java
    └── VersionInfo.java
```

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

- Версия берётся из git-тега вида `v1.2.3`; если тегов нет — `1.0.0` + короткий хэш коммита.
- JavaFX jmods: берутся из `$env:JAVAFX_JMODS`, затем из `C:\tools\javafx-jmods-*`; если их нет — автоматически скачиваются с сайта Gluon. Нужен только установленный JDK 17.
- В приложение включён модуль проверки обновлений: для публикации новой версии создайте Release в GitHub (`v1.0.1` и т.д.) и прикрепите к нему `TaskReminder-<версия>-win-x64.zip`. Кнопка «Проверить обновления» покажет уведомление о новой версии.
- Готовая сборка предназначена для **Windows x64**.

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

Copyright (c) 2026

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
