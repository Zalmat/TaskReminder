#!/usr/bin/env bash
# build-release.sh
# Сборка релизной сборки TaskReminder под Linux: app-image + tar.gz + sha256.
# jmods берутся из $JAVAFX_JMODS, затем из ~/tools/javafx-jmods-*,
# при отсутствии — скачиваются с сайта Gluon.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

JAVAFX_VERSION="${JAVAFX_VERSION:-17.0.20}"

echo "========================================"
echo "  TaskReminder Release Build (Linux)"
echo "========================================"

# 1. Сборка jar + генерация версии из git
if [[ "${SKIP_BUILD:-0}" != "1" ]]; then
    echo "[1/6] Building (generateVersionInfo + shadowJar)..."
    if [[ -f "$ROOT/gradlew" ]]; then
        chmod +x "$ROOT/gradlew"
    fi
    "$ROOT/gradlew" generateVersionInfo shadowJar --console=plain
else
    echo "[1/6] Skipping build (SKIP_BUILD=1)"
fi

# 2. Чтение версии
VERSION_FILE="$ROOT/build/release/version.properties"
if [[ ! -f "$VERSION_FILE" ]]; then
    echo "version.properties not found: $VERSION_FILE" >&2
    exit 1
fi
VERSION="$(grep -E '^version=' "$VERSION_FILE" | head -n1 | cut -d= -f2- | tr -d '\r')"
NUMERIC="$(grep -E '^numeric=' "$VERSION_FILE" | head -n1 | cut -d= -f2- | tr -d '\r')"
VERSION="${VERSION:-1.0.0}"
NUMERIC="${NUMERIC:-1.0.0}"
echo "[2/6] Version: $VERSION (app-version: $NUMERIC)"

# 3. Поиск JavaFX jmods
find_jmods_dir() {
    local base="$1"
    [[ -d "$base" ]] || return 1
    local jmod
    jmod="$(find "$base" -maxdepth 3 -type f -name '*.jmod' -print -quit 2>/dev/null || true)"
    [[ -n "$jmod" ]] || return 1
    dirname "$jmod"
}

JMODS=""
if [[ -n "${JAVAFX_JMODS:-}" ]]; then
    JMODS="$(find_jmods_dir "$JAVAFX_JMODS" || true)"
fi

if [[ -z "$JMODS" ]]; then
    for candidate in "$HOME"/tools/javafx-jmods-*; do
        [[ -d "$candidate" ]] || continue
        resolved="$(find_jmods_dir "$candidate" || true)"
        if [[ -n "$resolved" ]]; then
            JMODS="$resolved"
            break
        fi
    done
fi

if [[ -z "$JMODS" ]]; then
    echo "[3/6] JavaFX jmods not found locally, downloading from Gluon..."
    MODS_DIR="$ROOT/build/jmods"
    ZIP="$MODS_DIR/openjfx-${JAVAFX_VERSION}_linux-x64_bin-jmods.zip"
    DIR="$MODS_DIR/openjfx-${JAVAFX_VERSION}"
    mkdir -p "$MODS_DIR"
    if [[ ! -f "$ZIP" ]]; then
        curl -fL "https://download2.gluonhq.com/openjfx/${JAVAFX_VERSION}/openjfx-${JAVAFX_VERSION}_linux-x64_bin-jmods.zip" -o "$ZIP"
    fi
    rm -rf "$DIR"
    mkdir -p "$DIR"
    unzip -q "$ZIP" -d "$DIR"
    JMODS="$(find_jmods_dir "$DIR" || true)"
fi

if [[ -z "$JMODS" ]]; then
    echo "JavaFX jmods not found" >&2
    exit 1
fi
echo "[3/6] Using jmods: $JMODS"

# 4. Поиск jpackage
JPACKAGE=""
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/jpackage" ]]; then
    JPACKAGE="$JAVA_HOME/bin/jpackage"
else
    JPACKAGE="$(command -v jpackage || true)"
fi
if [[ -z "$JPACKAGE" ]]; then
    echo "jpackage not found. Install JDK 17 or set JAVA_HOME." >&2
    exit 1
fi
echo "[4/6] jpackage: $JPACKAGE"

# 5. Сборка app-image
RELEASE_DIR="$ROOT/build/release"
APP_DIR="$RELEASE_DIR/TaskReminder"
rm -rf "$APP_DIR"
mkdir -p "$RELEASE_DIR"

JAR="$(find "$ROOT/build/libs" -maxdepth 1 -type f -name 'TaskReminder-*-all.jar' -printf '%T@ %p\n' 2>/dev/null | sort -nr | head -n1 | cut -d' ' -f2-)"
if [[ -z "$JAR" ]]; then
    echo "Shadow jar not found in build/libs" >&2
    exit 1
fi
JAR_NAME="$(basename "$JAR")"
echo "[5/6] Building app-image from $JAR_NAME..."

"$JPACKAGE" \
    --name TaskReminder \
    --input "$ROOT/build/libs" \
    --main-jar "$JAR_NAME" \
    --main-class com.reminder.App \
    --type app-image \
    --dest "$RELEASE_DIR" \
    --module-path "$JMODS" \
    --add-modules javafx.controls,javafx.fxml,javafx.media,java.desktop,java.net.http,java.prefs,java.xml,java.logging,java.management,java.naming,java.sql,jdk.crypto.ec,jdk.crypto.cryptoki \
    --java-options "-Dfile.encoding=UTF-8" \
    --app-version "$NUMERIC"

# Чистка служебных файлов данных, попавших в каталог приложения
find "$APP_DIR" -maxdepth 1 -type f \( -name '*.dat' -o -name 'work_entries.json' -o -name 'predefined_tasks.json' -o -name 'reminders.json' \) -delete 2>/dev/null || true

# Апдейтер (если собран): updater/autoupdater -> <app>/updater/
UPDATER_BIN="$ROOT/updater/autoupdater"
if [[ -f "$UPDATER_BIN" ]]; then
    mkdir -p "$APP_DIR/updater"
    cp "$UPDATER_BIN" "$APP_DIR/updater/autoupdater"
    chmod +x "$APP_DIR/updater/autoupdater"
    echo "Updater added: updater/autoupdater"
else
    echo "Updater not found (updater/autoupdater) - auto-update disabled"
fi

# 6. Архив + контрольная сумма
echo "[6/6] Creating archive..."
ARCHIVE_NAME="TaskReminder-${VERSION}-linux-x64.tar.gz"
ARCHIVE_PATH="$RELEASE_DIR/$ARCHIVE_NAME"
rm -f "$ARCHIVE_PATH"
tar -czf "$ARCHIVE_PATH" -C "$RELEASE_DIR" TaskReminder
( cd "$RELEASE_DIR" && sha256sum "$ARCHIVE_NAME" > "$ARCHIVE_NAME.sha256" )

echo ""
echo "========================================"
echo " Release build complete!"
echo " Version : $VERSION"
echo " Archive : $ARCHIVE_PATH"
echo " SHA256  : $ARCHIVE_PATH.sha256"
echo "========================================"
