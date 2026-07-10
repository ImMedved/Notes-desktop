package com.notes.client.ui.components;

import com.notes.client.ui.Theme;
import com.notes.shared.model.TimerEntry;
import com.notes.shared.model.TimerMode;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

public class TimersTabPanel extends JPanel {
    private final DefaultListModel<TimerEntry> timerListModel = new DefaultListModel<>();
    private final JList<TimerEntry> timerList = new JList<>(timerListModel);
    private final JButton addTimerButton = Theme.accentButton("Добавить таймер");
    private final JButton addStopwatchButton = Theme.button("Добавить секундомер");
    private final JButton toggleButton = Theme.button("Старт / Пауза");
    private final JButton resetButton = Theme.button("Сбросить");
    private final JButton deleteButton = Theme.button("Удалить");
    private final javax.swing.JLabel timerDetailLabel = Theme.label("00:00:00");
    private final javax.swing.JLabel timerMetaLabel = Theme.mutedLabel("Нет выбранного таймера");

    public TimersTabPanel() {
        super(new BorderLayout(16, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        build();
    }

    private void build() {
        timerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Theme.styleList(timerList);
        timerList.setCellRenderer(Theme.listRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                TimerEntry timer = (TimerEntry) value;
                String status = timer.getMode() == TimerMode.STOPWATCH ? "секундомер" : "таймер";
                String text = "<html><b>" + escape(timer.getName()) + "</b><br/><span style='color:#9AA6B2;'>"
                        + status + "</span></html>";
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        }));

        JPanel left = Theme.panel();
        left.setLayout(new BorderLayout(12, 12));
        left.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        left.add(Theme.label("Таймеры"), BorderLayout.NORTH);
        left.add(Theme.scrollPane(timerList), BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(0, 1, 8, 8));
        actions.setBackground(Theme.PANEL);
        actions.add(addTimerButton);
        actions.add(addStopwatchButton);
        actions.add(toggleButton);
        actions.add(resetButton);
        actions.add(deleteButton);
        left.add(actions, BorderLayout.SOUTH);
        left.setPreferredSize(new Dimension(300, 0));

        JPanel detail = Theme.panel();
        detail.setLayout(new BorderLayout(14, 14));
        detail.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        javax.swing.JLabel title = Theme.label("Активный таймер");
        title.setFont(new Font("Bahnschrift", Font.BOLD, 22));
        timerDetailLabel.setHorizontalAlignment(SwingConstants.CENTER);
        timerDetailLabel.setFont(new Font("Bahnschrift", Font.BOLD, 56));
        timerDetailLabel.setForeground(Theme.ACCENT);
        timerMetaLabel.setFont(Theme.bodyFont());

        JTextArea description = Theme.textArea();
        description.setEditable(false);
        description.setText("""
                Creating new stopwatches and timers is only available when the server is connected.
                """);

        detail.add(title, BorderLayout.NORTH);
        detail.add(timerDetailLabel, BorderLayout.CENTER);
        detail.add(timerMetaLabel, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout(0, 14));
        right.setBackground(Theme.BACKGROUND);
        right.add(detail, BorderLayout.CENTER);
        right.add(Theme.scrollPane(description), BorderLayout.SOUTH);
        ((JScrollPane) right.getComponent(1)).setPreferredSize(new Dimension(0, 190));

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);
    }

    public DefaultListModel<TimerEntry> getTimerListModel() {
        return timerListModel;
    }

    public JList<TimerEntry> getTimerList() {
        return timerList;
    }

    public JButton getAddTimerButton() {
        return addTimerButton;
    }

    public JButton getAddStopwatchButton() {
        return addStopwatchButton;
    }

    public JButton getToggleButton() {
        return toggleButton;
    }

    public JButton getResetButton() {
        return resetButton;
    }

    public JButton getDeleteButton() {
        return deleteButton;
    }

    public javax.swing.JLabel getTimerDetailLabel() {
        return timerDetailLabel;
    }

    public javax.swing.JLabel getTimerMetaLabel() {
        return timerMetaLabel;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
