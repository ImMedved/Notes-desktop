package com.notes.client.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

public final class Theme {
    public static final Color BACKGROUND = new Color(12, 17, 24);
    public static final Color PANEL = new Color(21, 28, 37);
    public static final Color PANEL_ALT = new Color(28, 37, 49);
    public static final Color CARD = new Color(39, 50, 66);
    public static final Color ACCENT = new Color(91, 194, 249);
    public static final Color ACCENT_SOFT = new Color(38, 113, 158);
    public static final Color TEXT = new Color(239, 246, 255);
    public static final Color MUTED = new Color(148, 163, 184);

    private Theme() {
    }

    public static void install() {
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("TabbedPane.selected", PANEL_ALT);
        UIManager.put("TabbedPane.contentAreaColor", PANEL);
        UIManager.put("TabbedPane.foreground", TEXT);
        UIManager.put("TabbedPane.background", PANEL);
        UIManager.put("TabbedPane.focus", ACCENT);
    }

    public static Font titleFont() {
        return new Font("Bahnschrift", Font.BOLD, 18);
    }

    public static Font bodyFont() {
        return new Font("Segoe UI Variable", Font.PLAIN, 14);
    }

    public static Font monoFont() {
        return new Font("Consolas", Font.PLAIN, 13);
    }

    public static JPanel panel() {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL);
        return panel;
    }

    public static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(bodyFont());
        return label;
    }

    public static JLabel mutedLabel(String text) {
        JLabel label = label(text);
        label.setForeground(MUTED);
        return label;
    }

    public static JTextField textField() {
        JTextField field = new JTextField();
        field.setBackground(PANEL_ALT);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setBorder(compoundBorder());
        field.setFont(bodyFont());
        return field;
    }

    public static JTextArea textArea() {
        JTextArea area = new JTextArea();
        area.setBackground(PANEL_ALT);
        area.setForeground(TEXT);
        area.setCaretColor(TEXT);
        area.setSelectionColor(ACCENT_SOFT);
        area.setSelectedTextColor(TEXT);
        area.setBorder(compoundBorder());
        area.setFont(monoFont());
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    public static JScrollPane scrollPane(JComponent component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(PANEL_ALT);
        return scrollPane;
    }

    public static JButton button(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setForeground(TEXT);
        button.setBackground(CARD);
        button.setOpaque(false);
        button.setBorder(new RoundedBorder(CARD));
        button.setFont(bodyFont());
        button.setMargin(new Insets(8, 12, 8, 12));
        return button;
    }

    public static JButton accentButton(String text) {
        JButton button = button(text);
        button.setBorder(new RoundedBorder(ACCENT_SOFT));
        return button;
    }

    public static <T> void styleList(JList<T> list) {
        list.setBackground(PANEL_ALT);
        list.setForeground(TEXT);
        list.setSelectionBackground(ACCENT_SOFT);
        list.setSelectionForeground(TEXT);
        list.setFont(bodyFont());
        list.setBorder(compoundBorder());
    }

    public static <T> ListCellRenderer<? super T> listRenderer(ListCellRenderer<? super T> delegate) {
        return (list, value, index, isSelected, cellHasFocus) -> {
            Component component = delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (component instanceof JComponent cell) {
                cell.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                cell.setBackground(isSelected ? ACCENT_SOFT : PANEL_ALT);
                cell.setForeground(TEXT);
            }
            return component;
        };
    }

    private static Border compoundBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 67, 79), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        );
    }

    private static final class RoundedBorder implements Border {
        private final Color color;

        private RoundedBorder(Color color) {
            this.color = color;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(color);
            graphics.setStroke(new BasicStroke(1.2f));
            graphics.drawRoundRect(x, y, width - 1, height - 1, 16, 16);
            graphics.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(8, 12, 8, 12);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}
