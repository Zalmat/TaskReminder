package com.reminder.ui.theme;

import com.reminder.settings.AppSettings;
import com.reminder.settings.Theme;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

import java.net.URL;
import java.util.List;

/** Применение темы оформления к сценам и диалогам. */
public final class ThemeManager {

    private static final String BASE_STYLESHEET = "/styles/style.css";

    private ThemeManager() {
    }

    public static void apply(Scene scene) {
        apply(scene, AppSettings.theme());
    }

    public static void apply(Scene scene, Theme theme) {
        if (scene == null) {
            return;
        }
        applyStyles(scene.getStylesheets(), theme);
    }

    public static void applyTo(DialogPane pane) {
        applyTo(pane, AppSettings.theme());
    }

    public static void applyTo(DialogPane pane, Theme theme) {
        if (pane == null) {
            return;
        }
        applyStyles(pane.getStylesheets(), theme);
    }

    private static void applyStyles(List<String> stylesheets, Theme theme) {
        addIfMissing(stylesheets, BASE_STYLESHEET);
        stylesheets.removeIf(ThemeManager::isThemeStylesheet);
        addIfMissing(stylesheets, theme.stylesheet());
    }

    private static void addIfMissing(List<String> stylesheets, String resource) {
        URL url = ThemeManager.class.getResource(resource);
        if (url == null) {
            return;
        }
        String externalForm = url.toExternalForm();
        if (!stylesheets.contains(externalForm)) {
            stylesheets.add(externalForm);
        }
    }

    private static boolean isThemeStylesheet(String stylesheet) {
        return stylesheet.contains("/styles/style-light.css")
                || stylesheet.contains("/styles/style-dark.css");
    }
}