package com.reminder.ui;

import com.reminder.i18n.I18n;
import com.reminder.util.Log;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/** Сворачивает приложение в системный трей через java.awt.SystemTray. */
public class TrayService {

    private TrayIcon trayIcon;

    public static boolean isSupported() {
        return SystemTray.isSupported();
    }

    /** Устанавливает иконку в трей. onQuit вызывается при выборе "Выход". */
    public void install(Stage stage, Runnable onQuit) {
        if (!isSupported()) {
            return;
        }

        PopupMenu menu = new PopupMenu();
        MenuItem openItem = new MenuItem(I18n.get("tray.open"));
        openItem.addActionListener(e -> restore(stage));
        MenuItem quitItem = new MenuItem(I18n.get("tray.exit"));
        quitItem.addActionListener(e -> Platform.runLater(onQuit));
        menu.add(openItem);
        menu.addSeparator();
        menu.add(quitItem);

        trayIcon = new TrayIcon(createTrayImage(), I18n.get("app.tray.caption"), menu);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    restore(stage);
                }
            }
        });

        try {
            SystemTray.getSystemTray().add(trayIcon);
            Log.log("Tray", "tray icon installed");
        } catch (AWTException e) {
            Log.log("Tray", "cannot add tray icon: " + e.getMessage(), e);
            System.err.println("Cannot add tray icon: " + e.getMessage());
        }
    }

    /** Показывает уведомление у иконки в трее. */
    public void showMessage(String caption, String text) {
        if (trayIcon != null) {
            trayIcon.displayMessage(caption, text, TrayIcon.MessageType.INFO);
        }
    }

    /** Разворачивает и показывает главное окно. */
    public void restore(Stage stage) {
        Platform.runLater(() -> {
            stage.setIconified(false);
            stage.show();
            stage.toFront();
            stage.requestFocus();
        });
    }

    /** Убирает иконку из трея. */
    public void remove() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
    }

    private static Image createTrayImage() {
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(0x2196F3));
        g.fillRoundRect(0, 0, size, size, 5, 5);

        g.setColor(Color.WHITE);
        g.fillRoundRect(3, 2, 10, 12, 2, 2);

        g.setColor(new Color(0x2196F3));
        g.fillRect(5, 5, 6, 1);
        g.fillRect(5, 8, 6, 1);
        g.fillRect(5, 11, 4, 1);

        g.dispose();
        return image;
    }
}
