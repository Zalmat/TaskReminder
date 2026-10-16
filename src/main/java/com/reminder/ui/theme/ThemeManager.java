package com.reminder.ui.theme;

import com.reminder.settings.AppSettings;
import com.reminder.settings.Theme;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

import java.net.URL;

/** Применение темы оформления к сценам и диалогам. */
public final class ThemeManager {

    private ThemeManager() {
    }

    public static void apply(Scene scene) {
        apply(scene, AppSettings.theme());
    }

    public static void apply(Scene scene, Theme theme) {
        if (scene == null) {
            return;
        }
        scene.getStylesheets().removeIf(ThemeManager::isThemeStylesheet);
        add(scene.getStylesheets(), theme);
    }

    public static void applyTo(DialogPane pane) {
        applyTo(pane, AppSettings.theme());
    }

    public static void applyTo(DialogPane pane, Theme theme) {
        if (pane == null) {
            return;
        }
        pane.getStylesheets().removeIf(ThemeManager::isThemeStylesheet);
        add(pane.getStylesheets(), theme);
    }

    private static void add(java.util.List<String> stylesheets, Theme theme) {
        URL url = ThemeManager.class.getResource(theme.stylesheet());
        if (url != null) {
            stylesheets.add(url.toExternalForm());
        }
    }

    private static boolean isThemeStylesheet(String stylesheet) {
        return stylesheet.contains("/styles/style-light.css")
                || stylesheet.contains("/styles/style-dark.css");
    }
}