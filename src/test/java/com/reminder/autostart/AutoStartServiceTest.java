package com.reminder.autostart;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AutoStartServiceTest {

    @Test
    void windowsRunValueQuotesPathAndAddsMinimized() {
        assertEquals("\"C:\\apps\\TaskReminder\\TaskReminder.exe\" --minimized",
                AutoStartService.windowsRunValue("C:\\apps\\TaskReminder\\TaskReminder.exe"));
    }

    @Test
    void desktopEntryHasExecAndMinimized() {
        String entry = AutoStartService.desktopEntry("/opt/TaskReminder/bin/TaskReminder");
        assertTrue(entry.contains("Type=Application"));
        assertTrue(entry.contains("Exec=\"/opt/TaskReminder/bin/TaskReminder\" --minimized"));
        assertTrue(entry.contains("X-GNOME-Autostart-enabled=true"));
    }

    @Test
    void resolveLauncherForWindowsAppImage() {
        assumeTrue(System.getProperty("os.name", "").toLowerCase().startsWith("windows"));
        Path javaHome = Path.of("C:\\apps\\TaskReminder\\runtime");
        Path expected = Path.of("C:\\apps\\TaskReminder\\TaskReminder.exe");
        assertEquals(expected, LauncherPath.resolveLauncher(javaHome, "Windows 11").orElseThrow());
    }

    @Test
    void resolveLauncherForLinuxAppImage() {
        Path javaHome = Path.of("/opt/TaskReminder/lib/runtime");
        Path expected = Path.of("/opt/TaskReminder/bin/TaskReminder");
        assertEquals(expected, LauncherPath.resolveLauncher(javaHome, "Linux").orElseThrow());
    }

    @Test
    void resolveLauncherReturnsEmptyForRegularJdk() {
        assertTrue(LauncherPath.resolveLauncher(Path.of("/usr/lib/jvm/zulu-17"), "Linux").isEmpty());
    }
}