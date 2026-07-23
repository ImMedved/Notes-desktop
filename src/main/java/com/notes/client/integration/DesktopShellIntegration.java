package com.notes.client.integration;

import com.notes.client.ui.Theme;

import javax.swing.SwingUtilities;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.util.function.BooleanSupplier;

public class DesktopShellIntegration implements AutoCloseable {
    private final BooleanSupplier visibleSupplier;
    private final Runnable showAction;
    private final Runnable hideAction;
    private final Runnable exitAction;
    private TrayIcon trayIcon;
    private WindowsHotkeyManager hotkeyManager;

    public DesktopShellIntegration(BooleanSupplier visibleSupplier, Runnable showAction, Runnable hideAction, Runnable exitAction) {
        this.visibleSupplier = visibleSupplier;
        this.showAction = showAction;
        this.hideAction = hideAction;
        this.exitAction = exitAction;
    }

    public void install() {
        installTray();
        hotkeyManager = WindowsHotkeyManager.tryCreate(this::toggleVisibility);
        if (hotkeyManager != null) {
            hotkeyManager.start();
        }
    }

    public void showTrayMessage(String caption, String text) {
        if (trayIcon != null) {
            trayIcon.displayMessage(caption, text, TrayIcon.MessageType.INFO);
        }
    }

    public boolean hasTrayIcon() {
        return trayIcon != null;
    }

    public void refreshTrayImage() {
        if (trayIcon != null) {
            trayIcon.setImage(createTrayImage());
        }
    }

    private void toggleVisibility() {
        SwingUtilities.invokeLater(() -> {
            if (visibleSupplier.getAsBoolean()) {
                hideAction.run();
            } else {
                showAction.run();
            }
        });
    }

    private void installTray() {
        if (!SystemTray.isSupported()) {
            return;
        }
        try {
            PopupMenu menu = new PopupMenu();

            MenuItem openItem = new MenuItem("Open");
            openItem.addActionListener(event -> SwingUtilities.invokeLater(showAction));

            MenuItem hideItem = new MenuItem("Hide to tray");
            hideItem.addActionListener(event -> SwingUtilities.invokeLater(hideAction));

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(event -> SwingUtilities.invokeLater(exitAction));

            menu.add(openItem);
            menu.add(hideItem);
            menu.addSeparator();
            menu.add(exitItem);

            trayIcon = new TrayIcon(createTrayImage(), "Notes Widget Client", menu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(event -> SwingUtilities.invokeLater(showAction));
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException ignored) {
            trayIcon = null;
        }
    }

    private Image createTrayImage() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        graphics.setColor(Theme.PANEL);
        graphics.fillRoundRect(0, 0, 16, 16, 6, 6);
        graphics.setColor(Theme.ACCENT);
        graphics.drawRoundRect(0, 0, 15, 15, 6, 6);
        graphics.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 11));
        graphics.drawString("N", 4, 12);
        graphics.dispose();
        return image;
    }

    @Override
    public void close() {
        if (hotkeyManager != null) {
            hotkeyManager.close();
        }
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
    }
}
