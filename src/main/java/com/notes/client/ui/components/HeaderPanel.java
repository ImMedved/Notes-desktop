package com.notes.client.ui.components;

import com.notes.client.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class HeaderPanel extends JPanel {
    private final JButton syncButton = miniButton("Sync");
    private final JButton minimizeButton = miniButton("_");
    private final JButton pinButton = miniButton("Pin");
    private final JButton closeButton = miniButton("X");

    public HeaderPanel() {
        super(new BorderLayout());
        setBackground(Theme.PANEL);
        setBorder(BorderFactory.createEmptyBorder(14, 18, 10, 18));

        JPanel titleBox = new JPanel();
        titleBox.setBackground(Theme.PANEL);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Notes Widget Client");
        title.setForeground(Theme.TEXT);
        title.setFont(Theme.titleFont());

        JLabel subtitle = new JLabel("Windows-клиент для сервера заметок и таймеров через Tailscale");
        subtitle.setForeground(Theme.MUTED);
        subtitle.setFont(Theme.bodyFont());

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(subtitle);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setBackground(Theme.PANEL);
        controls.add(syncButton);
        controls.add(minimizeButton);
        controls.add(pinButton);
        controls.add(closeButton);

        add(titleBox, BorderLayout.WEST);
        add(controls, BorderLayout.EAST);
    }

    public JButton getSyncButton() {
        return syncButton;
    }

    public JButton getMinimizeButton() {
        return minimizeButton;
    }

    public JButton getPinButton() {
        return pinButton;
    }

    public JButton getCloseButton() {
        return closeButton;
    }

    private JButton miniButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.setForeground(Theme.TEXT);
        button.setBackground(Theme.PANEL);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        return button;
    }
}
