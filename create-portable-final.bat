@echo off
chcp 65001 > nul

echo ========================================
echo   Creating Portable Version
echo ========================================
echo.

REM 1. Сборка JAR
echo 1. Building JAR...
call gradlew shadowJar

if errorlevel 1 (
    echo [ERROR] Build failed!
    pause
    exit /b 1
)

REM 2. Создание JRE
echo.
echo 2. Creating JRE with JavaFX...
if exist "build\jre" rmdir /s /q build\jre

jlink --module-path "C:\tools\javafx-sdk-17.0.19\lib" ^
      --add-modules java.base,java.desktop,java.logging,java.xml,javafx.controls,javafx.fxml,javafx.graphics,javafx.base ^
      --output build\jre ^
      --compress=2 ^
      --no-header-files ^
      --no-man-pages

if errorlevel 1 (
    echo [ERROR] jlink failed!
    pause
    exit /b 1
)

REM 3. Создание app-image
echo.
echo 3. Creating app-image...
if exist "build\installer" rmdir /s /q build\installer

jpackage --name TaskReminder ^
         --input build\libs ^
         --main-jar TaskReminder-1.0.0-all.jar ^
         --main-class com.reminder.App ^
         --type app-image ^
         --dest build\installer ^
         --runtime-image build\jre ^
         --java-options "-Dfile.encoding=UTF-8"

if errorlevel 1 (
    echo [ERROR] jpackage failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo Portable version created successfully!
echo ========================================
echo.
echo Location: build\installer\TaskReminder\
echo Run: build\installer\TaskReminder\TaskReminder.exe
echo.
pause