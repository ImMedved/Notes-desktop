package com.notes.client.ui;

import javax.swing.BorderFactory;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.undo.UndoManager;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseWheelEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

public final class Theme {
    public enum Mode {
        LIGHT,
        DARK
    }

    private static final String MUTED_LABEL = "notes.mutedLabel";
    private static final String ACCENT_BUTTON = "notes.accentButton";

    public static Color BACKGROUND;
    public static Color PANEL;
    public static Color PANEL_ALT;
    public static Color CARD;
    public static Color ACCENT;
    public static Color ACCENT_SOFT;
    public static Color TEXT;
    public static Color MUTED;
    public static Color BORDER;

    private static Mode currentMode = systemMode();

    static {
        applyPalette(currentMode);
    }

    private Theme() {
    }

    public static void install() {
        install(currentMode);
    }

    public static void install(Mode mode) {
        currentMode = mode;
        applyPalette(mode);
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("TabbedPane.selected", PANEL_ALT);
        UIManager.put("TabbedPane.contentAreaColor", PANEL);
        UIManager.put("TabbedPane.foreground", TEXT);
        UIManager.put("TabbedPane.background", PANEL);
        UIManager.put("TabbedPane.focus", ACCENT);
        UIManager.put("OptionPane.background", PANEL);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("TextField.background", PANEL_ALT);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextArea.background", PANEL_ALT);
        UIManager.put("TextArea.foreground", TEXT);
    }

    public static Mode currentMode() {
        return currentMode;
    }

    public static Mode toggledMode() {
        return currentMode == Mode.DARK ? Mode.LIGHT : Mode.DARK;
    }

    public static Mode systemMode() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return Mode.LIGHT;
        }
        try {
            Process process = new ProcessBuilder(
                    "reg",
                    "query",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v",
                    "AppsUseLightTheme"
            ).redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("AppsUseLightTheme") && line.toLowerCase(Locale.ROOT).contains("0x0")) {
                        return Mode.DARK;
                    }
                }
            }
            process.waitFor();
        } catch (Exception ignored) {
            return Mode.LIGHT;
        }
        return Mode.LIGHT;
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
        label.putClientProperty(MUTED_LABEL, Boolean.TRUE);
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

    public static JEditorPane htmlPane() {
        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setBackground(PANEL_ALT);
        pane.setForeground(TEXT);
        pane.setBorder(compoundBorder());
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setFont(bodyFont());
        return pane;
    }

    public static JScrollPane scrollPane(JComponent component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(PANEL_ALT);
        return scrollPane;
    }

    public static JScrollPane smoothScrollPane(JComponent component) {
        JScrollPane scrollPane = scrollPane(component);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.getVerticalScrollBar().setBlockIncrement(72);
        installSmoothWheelScroll(scrollPane);
        return scrollPane;
    }

    public static void installUndo(JTextComponent textComponent) {
        UndoManager undoManager = new UndoManager();
        textComponent.getDocument().addUndoableEditListener(undoManager);
        textComponent.getInputMap().put(KeyStroke.getKeyStroke('Z', InputEvent.CTRL_DOWN_MASK), "undo-last-edit");
        textComponent.getActionMap().put("undo-last-edit", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                }
            }
        });
    }

    public static JButton button(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
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
        button.putClientProperty(ACCENT_BUTTON, Boolean.TRUE);
        button.setBorder(new RoundedBorder(ACCENT_SOFT));
        return button;
    }

    public static void applyToTree(Component component) {
        applyToComponent(component);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyToTree(child);
            }
        }
    }

    public static void styleButton(AbstractButton button) {
        boolean accent = Boolean.TRUE.equals(button.getClientProperty(ACCENT_BUTTON));
        button.setForeground(TEXT);
        button.setBackground(accent ? ACCENT_SOFT : CARD);
        button.setBorder(new RoundedBorder(accent ? ACCENT_SOFT : CARD));
        button.setFont(bodyFont());
    }

    public static String colorHex(Color color) {
        return "%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue());
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
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        );
    }

    private static void applyPalette(Mode mode) {
        if (mode == Mode.DARK) {
            BACKGROUND = new Color(12, 17, 24);
            PANEL = new Color(21, 28, 37);
            PANEL_ALT = new Color(28, 37, 49);
            CARD = new Color(39, 50, 66);
            ACCENT = new Color(91, 194, 249);
            ACCENT_SOFT = new Color(38, 113, 158);
            TEXT = new Color(239, 246, 255);
            MUTED = new Color(148, 163, 184);
            BORDER = new Color(56, 67, 79);
            return;
        }

        BACKGROUND = new Color(248, 242, 232);
        PANEL = new Color(255, 250, 242);
        PANEL_ALT = new Color(252, 246, 236);
        CARD = new Color(230, 218, 238);
        ACCENT = new Color(126, 77, 184);
        ACCENT_SOFT = new Color(183, 148, 218);
        TEXT = new Color(42, 35, 49);
        MUTED = new Color(116, 102, 126);
        BORDER = new Color(220, 207, 226);
    }

    private static void applyToComponent(Component component) {
        if (component instanceof JPanel panel) {
            panel.setBackground(PANEL);
        }
        if (component instanceof JLabel label) {
            label.setForeground(Boolean.TRUE.equals(label.getClientProperty(MUTED_LABEL)) ? MUTED : TEXT);
        }
        if (component instanceof AbstractButton button) {
            styleButton(button);
        }
        if (component instanceof JTextField field) {
            field.setBackground(PANEL_ALT);
            field.setForeground(TEXT);
            field.setCaretColor(TEXT);
            field.setBorder(compoundBorder());
            field.setFont(bodyFont());
        }
        if (component instanceof JTextArea area) {
            area.setBackground(PANEL_ALT);
            area.setForeground(TEXT);
            area.setCaretColor(TEXT);
            area.setSelectionColor(ACCENT_SOFT);
            area.setSelectedTextColor(TEXT);
            area.setBorder(compoundBorder());
            area.setFont(monoFont());
        }
        if (component instanceof JEditorPane pane) {
            pane.setBackground(PANEL_ALT);
            pane.setForeground(TEXT);
            pane.setBorder(compoundBorder());
            pane.setFont(bodyFont());
        }
        if (component instanceof JList<?> list) {
            styleList(list);
        }
        if (component instanceof JScrollPane scrollPane) {
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.getViewport().setBackground(PANEL_ALT);
        }
        if (component instanceof JViewport viewport) {
            viewport.setBackground(PANEL_ALT);
        }
        if (component instanceof JTabbedPane tabs) {
            tabs.setBackground(PANEL);
            tabs.setForeground(TEXT);
            tabs.setFont(bodyFont());
        }
        if (component instanceof JSpinner spinner) {
            spinner.setFont(bodyFont());
        }
    }

    private static void installSmoothWheelScroll(JScrollPane scrollPane) {
        SmoothScrollState state = new SmoothScrollState(scrollPane);
        scrollPane.addMouseWheelListener(event -> {
            if (event.isConsumed() || event.getScrollType() != MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                return;
            }

            JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
            if (!scrollBar.isVisible()) {
                return;
            }

            int delta = (int) Math.round(event.getPreciseWheelRotation() * scrollBar.getUnitIncrement());
            if (delta == 0) {
                return;
            }

            event.consume();
            state.scrollBy(delta);
        });
    }

    private static final class SmoothScrollState {
        private final JScrollPane scrollPane;
        private final Timer timer;
        private int targetValue;

        private SmoothScrollState(JScrollPane scrollPane) {
            this.scrollPane = scrollPane;
            this.timer = new Timer(12, event -> animateStep());
            this.timer.setRepeats(true);
        }

        private void scrollBy(int delta) {
            JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
            int maxValue = Math.max(scrollBar.getMinimum(), scrollBar.getMaximum() - scrollBar.getVisibleAmount());
            int current = scrollBar.getValue();
            if (!timer.isRunning()) {
                targetValue = current;
            }
            targetValue = Math.max(scrollBar.getMinimum(), Math.min(maxValue, targetValue + delta));
            if (!timer.isRunning()) {
                timer.start();
            }
        }

        private void animateStep() {
            JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
            int current = scrollBar.getValue();
            int distance = targetValue - current;
            if (Math.abs(distance) <= 1) {
                scrollBar.setValue(targetValue);
                timer.stop();
                return;
            }

            int step = Math.max(1, Math.abs(distance) / 4);
            scrollBar.setValue(current + Integer.signum(distance) * step);
        }
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
