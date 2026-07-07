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

src/main/java/com/reminder/

├── App.java     # Точка входа

├── models/     # Модели данных

│   ├── Task.java

│   ├── WorkEntry.java

│   ├── Reminder.java

│   └── WeekEntry.java

├── services/    # Сервисный слой

│   ├── TaskService.java

│   ├── ReminderService.java

│   ├── ExportService.java

│   └── YamlLoaderService.java

├── controllers/      # Контроллеры

│   └── MainController.java

└── components/       # UI компоненты

    ├── WeekEntryCell.java
    
    └── WeekTotalCell.java

## 🚀 Запуск проекта

### Сборка и запуск через Gradle

# Сборка проекта
./gradlew clean build

# Запуск приложения
./gradlew run

# Сборка fat JAR
./gradlew shadowJar

### Запуск JAR файла

java -jar build/libs/TaskReminder-1.0.0-all.jar

### Создание нативного установщика

# Windows (создает .exe)
./gradlew createInstaller

# macOS (создает .dmg)
./gradlew createInstaller

# Linux (создает .deb)
./gradlew createInstaller

## 📝 Пример YAML файла для загрузки задач

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

## ⚙️ Системные требования

- **Java 17** или выше
- **Оперативная память**: от 512 MB
- **Операционная система**: Windows, macOS, Linux

## 📦 Установка

### Windows
1. Скачайте TaskReminder-1.0.0.exe
2. Запустите установщик
3. Следуйте инструкциям
4. Запустите приложение из меню Пуск

### macOS
1. Скачайте TaskReminder-1.0.0.dmg
2. Откройте DMG файл
3. Перетащите приложение в папку Applications
4. Запустите приложение

### Linux (Debian/Ubuntu)
sudo dpkg -i TaskReminder-1.0.0.deb

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
