package com.notes.client.ui.components;

import com.notes.client.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class SyncTabPanel extends JPanel {
    private final JTextField serverUrlField = Theme.textField();
    private final JTextField apiKeyField = Theme.textField();
    private final JTextField clientIdField = Theme.textField();
    private final JSpinner intervalSpinner = new JSpinner(new SpinnerNumberModel(60, 15, 600, 15));
    private final javax.swing.JLabel connectionStatusLabel = Theme.label("Сервер: неизвестно");
    private final javax.swing.JLabel lastSyncLabel = Theme.mutedLabel("Последняя синхронизация: еще не было");
    private final javax.swing.JLabel revisionLabel = Theme.mutedLabel("Revision: 0");
    private final JButton saveButton = Theme.accentButton("Сохранить");
    private final JButton testButton = Theme.button("Проверить");
    private final JButton syncButton = Theme.button("Sync now");

    public SyncTabPanel() {
        super(new BorderLayout(16, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        build();
    }

    private void build() {
        JPanel form = Theme.panel();
        form.setLayout(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.insets = new Insets(0, 0, 12, 12);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0;
        form.add(Theme.label("Tailscale URL"), gc);

        gc.gridx = 1;
        gc.weightx = 1;
        form.add(serverUrlField, gc);

        gc.gridx = 0;
        gc.gridy++;
        gc.weightx = 0;
        form.add(Theme.label("API key"), gc);

        gc.gridx = 1;
        form.add(apiKeyField, gc);

        gc.gridx = 0;
        gc.gridy++;
        form.add(Theme.label("Client ID"), gc);

        gc.gridx = 1;
        form.add(clientIdField, gc);

        gc.gridx = 0;
        gc.gridy++;
        form.add(Theme.label("Интервал, сек"), gc);

        gc.gridx = 1;
        intervalSpinner.setFont(Theme.bodyFont());
        form.add(intervalSpinner, gc);

        gc.gridx = 0;
        gc.gridy++;
        form.add(Theme.label("Статус"), gc);

        gc.gridx = 1;
        form.add(connectionStatusLabel, gc);

        gc.gridx = 0;
        gc.gridy++;
        form.add(Theme.label("Синхронизация"), gc);

        gc.gridx = 1;
        form.add(lastSyncLabel, gc);

        gc.gridx = 0;
        gc.gridy++;
        form.add(Theme.label("Ревизия"), gc);

        gc.gridx = 1;
        form.add(revisionLabel, gc);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setBackground(Theme.PANEL);
        actions.add(saveButton);
        actions.add(testButton);
        actions.add(syncButton);

        JTextArea instructions = Theme.textArea();
        instructions.setEditable(false);
        instructions.setText("""
                Рекомендуемая схема:
                1. На Arch-сервере запущен backend в Docker.
                2. Доступ к серверу идет по Tailscale IP или MagicDNS имени.
                3. Все клиенты используют один и тот же HTTP API и один API key.

                Пример URL:
                http://notes-server.tailnet-name.ts.net:8080
                """);

        JPanel right = Theme.panel();
        right.setLayout(new BorderLayout(0, 14));
        right.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        right.add(Theme.scrollPane(instructions), BorderLayout.CENTER);
        right.setPreferredSize(new Dimension(360, 0));

        add(form, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
        add(actions, BorderLayout.SOUTH);
    }

    public JTextField getServerUrlField() {
        return serverUrlField;
    }

    public JTextField getApiKeyField() {
        return apiKeyField;
    }

    public JTextField getClientIdField() {
        return clientIdField;
    }

    public JSpinner getIntervalSpinner() {
        return intervalSpinner;
    }

    public javax.swing.JLabel getConnectionStatusLabel() {
        return connectionStatusLabel;
    }

    public javax.swing.JLabel getLastSyncLabel() {
        return lastSyncLabel;
    }

    public javax.swing.JLabel getRevisionLabel() {
        return revisionLabel;
    }

    public JButton getSaveButton() {
        return saveButton;
    }

    public JButton getTestButton() {
        return testButton;
    }

    public JButton getSyncButton() {
        return syncButton;
    }
}
