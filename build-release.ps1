# build-release.ps1
# Сборка релизной сборки TaskReminder: app-image + zip + sha256.
# Работает на любом ПК с JDK 17: jmods берутся из $env:JAVAFX_JMODS, затем
# из локальных каталогов, при отсутствии - скачиваются с сайта Gluon.
param(
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot

Write-Host "========================================"
Write-Host "  TaskReminder Release Build"
Write-Host "========================================"

# 1. Сборка jar + генерация версии из git
if (-not $SkipBuild) {
    Write-Host "[1/6] Building (generateVersionInfo + shadowJar)..."
    & "$Root\gradlew.bat" generateVersionInfo shadowJar --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed (exit $LASTEXITCODE)" }
} else {
    Write-Host "[1/6] Skipping build (--SkipBuild)"
}

# 2. Чтение версии
$versionFile = Join-Path $Root "build\release\version.properties"
if (-not (Test-Path $versionFile)) { throw "version.properties not found: $versionFile" }
$props = @{}
Get-Content $versionFile | ForEach-Object {
    if ($_ -match '^\s*([^#=]+)=(.*)$') { $props[$matches[1].Trim()] = $matches[2].Trim() }
}
$version = if ($props['version']) { $props['version'] } else { '1.0.0' }
$numeric = if ($props['numeric']) { $props['numeric'] } else { '1.0.0' }
Write-Host "[2/6] Version: $version (app-version: $numeric)"

# 3. Поиск JavaFX jmods
$jmods = $null
if ($env:JAVAFX_JMODS -and (Test-Path $env:JAVAFX_JMODS)) { $jmods = $env:JAVAFX_JMODS }
elseif (Test-Path "C:\tools\javafx-jmods-17.0.19") { $jmods = "C:\tools\javafx-jmods-17.0.19" }
elseif (Test-Path "C:\tools\javafx-jmods-17.0.6") { $jmods = "C:\tools\javafx-jmods-17.0.6" }

if (-not $jmods) {
    Write-Host "[3/6] JavaFX jmods not found locally, downloading from Gluon..."
    $jmodsZip = Join-Path $env:TEMP "openjfx-17.0.6_windows-x64_bin-jmods.zip"
    $jmodsDir = Join-Path $env:TEMP "openjfx-jmods-17.0.6"
    if (-not (Test-Path $jmodsZip)) {
        Invoke-WebRequest -Uri "https://download2.gluonhq.com/openjfx/17.0.6/openjfx-17.0.6_windows-x64_bin-jmods.zip" -OutFile $jmodsZip -UseBasicParsing
    }
    if (Test-Path $jmodsDir) { Remove-Item -Recurse -Force $jmodsDir }
    Expand-Archive -Path $jmodsZip -DestinationPath $jmodsDir -Force
    $jmods = $jmodsDir
}
Write-Host "[3/6] Using jmods: $jmods"

# 4. Поиск jpackage
function Find-Jpackage {
    $cmd = Get-Command jpackage -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\jpackage.exe")) { return "$env:JAVA_HOME\bin\jpackage.exe" }
    foreach ($p in @("C:\Program Files\Zulu\zulu-17\bin\jpackage.exe", "C:\Program Files\Eclipse Adoptium\jdk-17*\bin\jpackage.exe", "C:\Program Files\Java\jdk-17*\bin\jpackage.exe")) {
        $hit = Get-ChildItem -Path $p -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($hit) { return $hit.FullName }
    }
    return $null
}
$jpackage = Find-Jpackage
if (-not $jpackage) { throw "jpackage not found. Install JDK 17 or set JAVA_HOME." }
Write-Host "[4/6] jpackage: $jpackage"

# 5. Сборка app-image
$releaseDir = Join-Path $Root "build\release"
$appDir = Join-Path $releaseDir "TaskReminder"
if (Test-Path $appDir) { Remove-Item -Recurse -Force $appDir }
New-Item -ItemType Directory -Force -Path $releaseDir | Out-Null

$jar = Get-ChildItem -Path (Join-Path $Root "build\libs") -Filter "TaskReminder-*-all.jar" -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { throw "Shadow jar not found in build\libs" }
Write-Host "[5/6] Building app-image from $($jar.Name)..."

& $jpackage `
    --name TaskReminder `
    --input (Join-Path $Root "build\libs") `
    --main-jar $jar.Name `
    --main-class com.reminder.App `
    --type app-image `
    --dest $releaseDir `
    --module-path $jmods `
    --add-modules javafx.controls,javafx.fxml,javafx.media,java.desktop,java.net.http,java.prefs,java.xml,java.logging,java.management,java.naming,java.sql,jdk.crypto.ec,jdk.crypto.cryptoki `
    --java-options "-Dfile.encoding=UTF-8" `
    --app-version $numeric
if ($LASTEXITCODE -ne 0) { throw "jpackage failed (exit $LASTEXITCODE)" }

# Чистка служебных файлов данных, попавших в каталог приложения при запуске exe
Get-ChildItem -Path $appDir -Force -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Extension -eq '.dat' -or $_.Name -in @('work_entries.json', 'predefined_tasks.json', 'reminders.json') } |
    Remove-Item -Force -ErrorAction SilentlyContinue

# 6. Архив + контрольная сумма
Write-Host "[6/6] Creating archive..."
$zipName = "TaskReminder-$version-win-x64.zip"
$zipPath = Join-Path $releaseDir $zipName
if (Test-Path $zipPath) { Remove-Item -Force $zipPath }
Compress-Archive -Path $appDir -DestinationPath $zipPath -CompressionLevel Optimal

$hash = (Get-FileHash -Path $zipPath -Algorithm SHA256).Hash
"$hash  $zipName" | Set-Content -Path "$zipPath.sha256" -Encoding ascii

Write-Host ""
Write-Host "========================================"
Write-Host " Release build complete!"
Write-Host " Version : $version"
Write-Host " Zip     : $zipPath"
Write-Host " SHA256  : $zipPath.sha256"
Write-Host "========================================"