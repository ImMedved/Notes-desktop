package com.notes.client.ui;

import com.notes.client.config.ClientConfig;
import com.notes.client.model.ClientViewState;
import com.notes.client.service.ClientAppService;
import com.notes.shared.model.Note;
import com.notes.shared.model.TimerEntry;
import com.notes.shared.model.TimerMode;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WidgetFrame extends JFrame {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final ClientAppService appService;
    private final ExecutorService background = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "notes-client-background");
        thread.setDaemon(true);
        return thread;
    });

    private final DefaultListModel<NoteListEntry> noteListModel = new DefaultListModel<>();
    private final DefaultListModel<TimerEntry> timerListModel = new DefaultListModel<>();
    private final JList<NoteListEntry> noteList = new JList<>(noteListModel);
    private final JList<TimerEntry> timerList = new JList<>(timerListModel);
    private final JTextField noteTitleField = Theme.textField();
    private final JTextArea noteContentArea = Theme.textArea();
    private final JCheckBox pinNoteCheckBox = new JCheckBox("Закрепить");
    private final JCheckBox archiveNoteCheckBox = new JCheckBox("В архиве");
    private final JLabel noteMetaLabel = Theme.mutedLabel("Локальный кеш");
    private final JLabel timerDetailLabel = Theme.label("00:00:00");
    private final JLabel timerMetaLabel = Theme.mutedLabel("Нет выбранного таймера");
    private final JLabel connectionStatusLabel = Theme.label("Сервер: неизвестно");
    private final JLabel lastSyncLabel = Theme.mutedLabel("Последняя синхронизация: еще не было");
    private final JLabel revisionLabel = Theme.mutedLabel("Revision: 0");
    private final JTextField serverUrlField = Theme.textField();
    private final JTextField apiKeyField = Theme.textField();
    private final JTextField clientIdField = Theme.textField();
    private final JSpinner intervalSpinner = new JSpinner(new SpinnerNumberModel(60, 15, 600, 15));
    private final Timer noteAutoSaveTimer;
    private final Timer timerRefreshTimer;
    private final Timer autoSyncTimer;

    private boolean suppressNoteEvents;
    private boolean suppressNoteSelectionEvents;
    private boolean archiveView;
    private String activeNoteSelectionId;
    private String archivedNoteSelectionId;
    private Point dragStart;

    public WidgetFrame(ClientAppService appService) {
        this.appService = appService;

        Theme.install();
        setTitle("Notes Widget Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        setSize(1120, 760);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BACKGROUND);

        ClientViewState viewState = appService.snapshotView();
        setAlwaysOnTop(viewState.getConfig().isAlwaysOnTop());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        styleCheckbox(pinNoteCheckBox);
        styleCheckbox(archiveNoteCheckBox);
        configureNoteTab();
        configureTimerTab();
        configureSyncTab();

        noteAutoSaveTimer = new Timer(700, event -> runAsync(this::persistSelectedNote));
        noteAutoSaveTimer.setRepeats(false);

        timerRefreshTimer = new Timer(400, event -> refreshLiveTimerState());
        timerRefreshTimer.start();

        autoSyncTimer = new Timer(viewState.getConfig().getSyncIntervalSeconds() * 1000, event -> syncNowAsync());
        autoSyncTimer.start();

        appService.addChangeListener(() -> SwingUtilities.invokeLater(this::refreshFromState));
        refreshFromState();
    }

    public void syncNowAsync() {
        runAsync(() -> appService.syncNow(), true);
    }

    private Component buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 10, 18));

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
        JButton sync = miniButton("Sync");
        sync.addActionListener(event -> syncNowAsync());
        JButton minimize = miniButton("_");
        minimize.addActionListener(event -> setState(JFrame.ICONIFIED));
        JButton pin = miniButton("Pin");
        pin.addActionListener(event -> {
            boolean next = !isAlwaysOnTop();
            setAlwaysOnTop(next);
            ClientConfig config = appService.snapshotView().getConfig();
            config.setAlwaysOnTop(next);
            appService.saveConfig(config);
        });
        JButton close = miniButton("X");
        close.addActionListener(event -> System.exit(0));
        controls.add(sync);
        controls.add(minimize);
        controls.add(pin);
        controls.add(close);

        HeaderDragAdapter dragAdapter = new HeaderDragAdapter();
        header.addMouseListener(dragAdapter);
        header.addMouseMotionListener(dragAdapter);

        header.add(titleBox, BorderLayout.WEST);
        header.add(controls, BorderLayout.EAST);
        return header;
    }

    private Component buildContent() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.bodyFont());
        tabs.setBackground(Theme.PANEL);
        tabs.setForeground(Theme.TEXT);
        tabs.setBorder(BorderFactory.createEmptyBorder(0, 18, 18, 18));
        tabs.add("Notes", buildNotesTab());
        tabs.add("Timers", buildTimersTab());
        tabs.add("Sync", buildSyncTab());
        return tabs;
    }

    private Component buildNotesTab() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));

        noteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Theme.styleList(noteList);
        noteList.setCellRenderer(Theme.listRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                NoteListEntry entry = (NoteListEntry) value;
                String text;
                if (entry.navigation()) {
                    text = "<html><b>" + escape(entry.title()) + "</b><br/><span style='color:#9AA6B2;'>"
                            + escape(entry.subtitle()) + "</span></html>";
                } else {
                    Note note = entry.note();
                    String prefix = note.isPinned() ? "\u2022 " : "";
                    text = "<html><b>" + escape(prefix + note.getTitle()) + "</b><br/><span style='color:#9AA6B2;'>"
                            + DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(note.getUpdatedAt())) + "</span></html>";
                }
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        }));

        JPanel listPanel = Theme.panel();
        listPanel.setLayout(new BorderLayout(12, 12));
        listPanel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        listPanel.add(Theme.label("Заметки"), BorderLayout.NORTH);
        listPanel.add(Theme.scrollPane(noteList), BorderLayout.CENTER);

        JPanel listActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        listActions.setBackground(Theme.PANEL);
        JButton add = Theme.accentButton("Новая");
        add.addActionListener(event -> runAsync(() -> {
            Note created = appService.createNote();
            SwingUtilities.invokeLater(() -> {
                archiveView = false;
                activeNoteSelectionId = created.getId();
                refreshFromState();
                selectNote(created.getId());
            });
        }));
        JButton delete = Theme.button("Удалить");
        delete.addActionListener(event -> runAsync(this::deleteSelectedNote));
        listActions.add(add);
        listActions.add(delete);
        listPanel.add(listActions, BorderLayout.SOUTH);

        JPanel editor = Theme.panel();
        editor.setLayout(new BorderLayout(12, 12));
        editor.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel editorTop = new JPanel(new BorderLayout(12, 8));
        editorTop.setBackground(Theme.PANEL);
        noteTitleField.setPreferredSize(new Dimension(320, 40));
        editorTop.add(noteTitleField, BorderLayout.CENTER);
        pinNoteCheckBox.setBackground(Theme.PANEL);
        archiveNoteCheckBox.setBackground(Theme.PANEL);
        JPanel noteFlags = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        noteFlags.setBackground(Theme.PANEL);
        noteFlags.add(pinNoteCheckBox);
        noteFlags.add(archiveNoteCheckBox);
        editorTop.add(noteFlags, BorderLayout.EAST);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setBackground(Theme.PANEL);
        toolbar.add(markdownButton("H1", "# "));
        toolbar.add(markdownButton("H2", "## "));
        toolbar.add(markdownButton("List", "- "));
        toolbar.add(markdownButton("Task", "- [ ] "));
        toolbar.add(markdownButton("Quote", "> "));
        toolbar.add(markdownWrapButton("Bold", "**", "**"));
        toolbar.add(markdownWrapButton("Code", "```\n", "\n```"));
        toolbar.add(markdownWrapButton("Link", "[", "](https://)"));

        JPanel topBlock = new JPanel();
        topBlock.setBackground(Theme.PANEL);
        topBlock.setLayout(new BoxLayout(topBlock, BoxLayout.Y_AXIS));
        topBlock.add(editorTop);
        topBlock.add(Box.createVerticalStrut(12));
        topBlock.add(toolbar);

        JPanel bottomMeta = new JPanel(new BorderLayout());
        bottomMeta.setBackground(Theme.PANEL);
        bottomMeta.add(noteMetaLabel, BorderLayout.WEST);

        editor.add(topBlock, BorderLayout.NORTH);
        editor.add(Theme.scrollPane(noteContentArea), BorderLayout.CENTER);
        editor.add(bottomMeta, BorderLayout.SOUTH);

        panel.add(listPanel, BorderLayout.WEST);
        panel.add(editor, BorderLayout.CENTER);
        listPanel.setPreferredSize(new Dimension(300, 0));
        return panel;
    }

    private Component buildTimersTab() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));

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
        JButton addTimer = Theme.accentButton("Добавить таймер");
        addTimer.addActionListener(event -> openCountdownDialog());
        JButton addStopwatch = Theme.button("Добавить секундомер");
        addStopwatch.addActionListener(event -> openStopwatchDialog());
        JButton toggle = Theme.button("Старт / Пауза");
        toggle.addActionListener(event -> runAsync(this::toggleSelectedTimer));
        JButton reset = Theme.button("Сбросить");
        reset.addActionListener(event -> runAsync(this::resetSelectedTimer));
        JButton delete = Theme.button("Удалить");
        delete.addActionListener(event -> runAsync(this::deleteSelectedTimer));
        actions.add(addTimer);
        actions.add(addStopwatch);
        actions.add(toggle);
        actions.add(reset);
        actions.add(delete);
        left.add(actions, BorderLayout.SOUTH);

        JPanel detail = Theme.panel();
        detail.setLayout(new BorderLayout(14, 14));
        detail.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel title = Theme.label("Активный таймер");
        title.setFont(new Font("Bahnschrift", Font.BOLD, 22));
        timerDetailLabel.setHorizontalAlignment(SwingConstants.CENTER);
        timerDetailLabel.setFont(new Font("Bahnschrift", Font.BOLD, 56));
        timerDetailLabel.setForeground(Theme.ACCENT);
        timerMetaLabel.setFont(Theme.bodyFont());

        JTextArea description = Theme.textArea();
        description.setEditable(false);
        description.setText("""
                В этой версии клиент не хранит серверные заметки и таймеры как источник истины.
                Все изменения отправляются на сервер, а UI после этого подтягивает свежий snapshot.

                Это упрощает добавление Android-клиента:
                - один API
                - одна ревизия состояния
                - одинаковые DTO для всех клиентов
                """);

        detail.add(title, BorderLayout.NORTH);
        detail.add(timerDetailLabel, BorderLayout.CENTER);
        detail.add(timerMetaLabel, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout(0, 14));
        right.setBackground(Theme.BACKGROUND);
        right.add(detail, BorderLayout.CENTER);
        right.add(Theme.scrollPane(description), BorderLayout.SOUTH);
        ((JScrollPane) right.getComponent(1)).setPreferredSize(new Dimension(0, 190));

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.CENTER);
        left.setPreferredSize(new Dimension(300, 0));
        return panel;
    }

    private Component buildSyncTab() {
        JPanel panel = Theme.panel();
        panel.setLayout(new BorderLayout(16, 16));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));

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
        JButton save = Theme.accentButton("Сохранить");
        save.addActionListener(event -> saveSyncSettings());
        JButton test = Theme.button("Проверить");
        test.addActionListener(event -> runAsync(() -> appService.testConnection()));
        JButton sync = Theme.button("Sync now");
        sync.addActionListener(event -> syncNowAsync());
        actions.add(save);
        actions.add(test);
        actions.add(sync);

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

        panel.add(form, BorderLayout.CENTER);
        panel.add(right, BorderLayout.EAST);
        panel.add(actions, BorderLayout.SOUTH);
        right.setPreferredSize(new Dimension(360, 0));
        return panel;
    }

    private void configureNoteTab() {
        noteList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !suppressNoteSelectionEvents) {
                NoteListEntry entry = noteList.getSelectedValue();
                if (entry != null && entry.navigation()) {
                    toggleArchiveView(entry.action() == NoteListAction.OPEN_ARCHIVE);
                    return;
                }
                rememberCurrentNoteSelection();
                loadSelectedNoteIntoEditor();
            }
        });
        DocumentListener noteListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleNoteSave();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleNoteSave();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                scheduleNoteSave();
            }
        };
        noteTitleField.getDocument().addDocumentListener(noteListener);
        noteContentArea.getDocument().addDocumentListener(noteListener);
        pinNoteCheckBox.addActionListener(event -> scheduleNoteSave());
        archiveNoteCheckBox.addActionListener(event -> scheduleNoteSave());
    }

    private void configureTimerTab() {
        timerList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                refreshLiveTimerState();
            }
        });
    }

    private void configureSyncTab() {
        ClientConfig config = appService.snapshotView().getConfig();
        serverUrlField.setText(config.getServerBaseUrl());
        apiKeyField.setText(config.getApiKey());
        clientIdField.setText(config.getClientId());
        intervalSpinner.setValue(config.getSyncIntervalSeconds());
    }

    private void refreshFromState() {
        ClientViewState state = appService.snapshotView();
        refreshNoteList(state);
        refreshTimerList(state);
        refreshSyncPanel(state);
    }

    private void refreshNoteList(ClientViewState viewState) {
        String selectedId = archiveView ? archivedNoteSelectionId : activeNoteSelectionId;
        boolean selectionStillExists = false;
        suppressNoteSelectionEvents = true;
        noteListModel.clear();
        noteListModel.addElement(archiveView ? NoteListEntry.backEntry() : NoteListEntry.archiveEntry());
        for (Note note : filteredNotes(viewState)) {
            noteListModel.addElement(NoteListEntry.noteEntry(note));
            if (Objects.equals(note.getId(), selectedId)) {
                selectionStillExists = true;
            }
        }
        if (selectedId != null) {
            selectNote(selectedId);
        }
        if (noteList.getSelectedIndex() == -1 && noteListModel.size() > 1) {
            noteList.setSelectedIndex(1);
        }
        suppressNoteSelectionEvents = false;
        if (!selectionStillExists || selectedId == null) {
            loadSelectedNoteIntoEditor();
        }
    }

    private void refreshTimerList(ClientViewState viewState) {
        String selectedId = selectedTimerId();
        timerListModel.clear();
        for (TimerEntry timer : viewState.getSnapshot().getTimers()) {
            timerListModel.addElement(timer);
        }
        if (selectedId != null) {
            selectTimer(selectedId);
        }
        if (timerList.getSelectedIndex() == -1 && !timerListModel.isEmpty()) {
            timerList.setSelectedIndex(0);
        }
        refreshLiveTimerState();
    }

    private void refreshSyncPanel(ClientViewState state) {
        serverUrlField.setText(state.getConfig().getServerBaseUrl());
        apiKeyField.setText(state.getConfig().getApiKey());
        clientIdField.setText(state.getConfig().getClientId());
        intervalSpinner.setValue(state.getConfig().getSyncIntervalSeconds());
        autoSyncTimer.setDelay(state.getConfig().getSyncIntervalSeconds() * 1000);
        autoSyncTimer.setInitialDelay(state.getConfig().getSyncIntervalSeconds() * 1000);

        if (state.isServerReachable()) {
            connectionStatusLabel.setText("Сервер доступен");
            connectionStatusLabel.setForeground(new Color(115, 223, 160));
        } else if (state.getLastError() != null && !state.getLastError().isBlank()) {
            connectionStatusLabel.setText(state.getLastError());
            connectionStatusLabel.setForeground(new Color(255, 190, 92));
        } else {
            connectionStatusLabel.setText("Сервер: неизвестно");
            connectionStatusLabel.setForeground(Theme.MUTED);
        }
        lastSyncLabel.setText(state.getLastSuccessfulSyncEpochMillis() == 0
                ? "Последняя синхронизация: еще не было"
                : "Последняя синхронизация: " + DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(state.getLastSuccessfulSyncEpochMillis())));
        revisionLabel.setText("Revision: " + state.getSnapshot().getRevision());
    }

    private void loadSelectedNoteIntoEditor() {
        Note note = selectedNote();
        suppressNoteEvents = true;
        if (note == null) {
            noteTitleField.setText("");
            noteContentArea.setText("");
            pinNoteCheckBox.setSelected(false);
            archiveNoteCheckBox.setSelected(false);
            noteMetaLabel.setText(archiveView ? "Выберите заметку из архива" : "Выберите заметку");
        } else {
            noteTitleField.setText(note.getTitle());
            noteContentArea.setText(note.getContent());
            pinNoteCheckBox.setSelected(note.isPinned());
            archiveNoteCheckBox.setSelected(note.isArchived());
            noteMetaLabel.setText("Изменена: " + DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(note.getUpdatedAt())));
        }
        setNoteEditorEnabled(note != null);
        suppressNoteEvents = false;
    }

    private void persistSelectedNote() throws Exception {
        if (suppressNoteEvents) {
            return;
        }
        Note selected = selectedNote();
        if (selected == null) {
            return;
        }
        Note updated = new Note();
        updated.setId(selected.getId());
        updated.setTitle(noteTitleField.getText().trim().isBlank() ? "Без названия" : noteTitleField.getText().trim());
        updated.setContent(noteContentArea.getText());
        updated.setPinned(pinNoteCheckBox.isSelected());
        updated.setArchived(archiveNoteCheckBox.isSelected());
        updated.setCreatedAt(selected.getCreatedAt());
        updated.setUpdatedAt(System.currentTimeMillis());
        appService.upsertNote(updated);
    }

    private void deleteSelectedNote() throws Exception {
        Note selected = selectedNote();
        if (selected != null) {
            appService.deleteNote(selected.getId());
        }
    }

    private void refreshLiveTimerState() {
        TimerEntry timer = timerList.getSelectedValue();
        if (timer == null) {
            timerDetailLabel.setText("00:00:00");
            timerMetaLabel.setText("Нет выбранного таймера");
            return;
        }
        long now = System.currentTimeMillis();
        String value = timer.getMode() == TimerMode.STOPWATCH
                ? formatDuration(timer.getElapsedMillis(now))
                : formatDuration(timer.getRemainingMillis(now));
        timerDetailLabel.setText(value);
        String label = timer.getMode() == TimerMode.STOPWATCH ? "Секундомер" : "Таймер";
        String state = timer.isRunning() ? "работает" : "на паузе";
        timerMetaLabel.setText(label + " \"" + timer.getName() + "\" " + state);
    }

    private void toggleSelectedTimer() throws Exception {
        TimerEntry selected = timerList.getSelectedValue();
        if (selected != null) {
            appService.toggleTimer(selected.getId());
        }
    }

    private void resetSelectedTimer() throws Exception {
        TimerEntry selected = timerList.getSelectedValue();
        if (selected != null) {
            appService.resetTimer(selected.getId());
        }
    }

    private void deleteSelectedTimer() throws Exception {
        TimerEntry selected = timerList.getSelectedValue();
        if (selected != null) {
            appService.deleteTimer(selected.getId());
        }
    }

    private void openCountdownDialog() {
        JTextField nameField = Theme.textField();
        JSpinner days = new JSpinner(new SpinnerNumberModel(0, 0, 365, 1));
        JSpinner hours = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
        JSpinner minutes = new JSpinner(new SpinnerNumberModel(15, 0, 59, 1));
        JSpinner seconds = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.setBackground(Theme.PANEL);
        panel.add(Theme.label("Название"));
        panel.add(nameField);
        panel.add(Theme.label("Дни"));
        panel.add(days);
        panel.add(Theme.label("Часы"));
        panel.add(hours);
        panel.add(Theme.label("Минуты"));
        panel.add(minutes);
        panel.add(Theme.label("Секунды"));
        panel.add(seconds);

        int result = JOptionPane.showConfirmDialog(this, panel, "Новый таймер", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            long totalMillis = ((((long) (int) days.getValue() * 24
                    + (int) hours.getValue()) * 60
                    + (int) minutes.getValue()) * 60
                    + (int) seconds.getValue()) * 1000L;
            if (totalMillis <= 0) {
                totalMillis = 60_000L;
            }
            final long durationMillis = totalMillis;
            runAsync(() -> {
                TimerEntry created = appService.createCountdown(nameField.getText(), durationMillis);
                SwingUtilities.invokeLater(() -> selectTimer(created.getId()));
            });
        }
    }

    private void openStopwatchDialog() {
        String name = JOptionPane.showInputDialog(this, "Название секундомера", "Новый секундомер", JOptionPane.PLAIN_MESSAGE);
        if (name != null) {
            runAsync(() -> {
                TimerEntry created = appService.createStopwatch(name);
                SwingUtilities.invokeLater(() -> selectTimer(created.getId()));
            });
        }
    }

    private void saveSyncSettings() {
        ClientConfig config = appService.snapshotView().getConfig();
        config.setServerBaseUrl(serverUrlField.getText().trim());
        config.setApiKey(apiKeyField.getText().trim());
        config.setClientId(clientIdField.getText().trim());
        config.setSyncIntervalSeconds((int) intervalSpinner.getValue());
        config.setAlwaysOnTop(isAlwaysOnTop());
        appService.saveConfig(config);
        runAsync(() -> appService.testConnection());
    }

    private void scheduleNoteSave() {
        if (!suppressNoteEvents) {
            noteAutoSaveTimer.restart();
        }
    }

    private JButton miniButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setForeground(Theme.TEXT);
        button.setBackground(Theme.PANEL);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        return button;
    }

    private JButton markdownButton(String label, String prefix) {
        JButton button = Theme.button(label);
        button.addActionListener(event -> insertMarkdown(prefix));
        return button;
    }

    private JButton markdownWrapButton(String label, String before, String after) {
        JButton button = Theme.button(label);
        button.addActionListener(event -> wrapMarkdown(before, after));
        return button;
    }

    private void insertMarkdown(String prefix) {
        int start = noteContentArea.getSelectionStart();
        int end = noteContentArea.getSelectionEnd();
        String selected = noteContentArea.getSelectedText();
        if (selected == null) {
            selected = "";
        }
        String replacement = prefix + selected;
        noteContentArea.replaceRange(replacement, start, end);
        noteContentArea.requestFocusInWindow();
        noteContentArea.select(start + replacement.length(), start + replacement.length());
    }

    private void wrapMarkdown(String before, String after) {
        int start = noteContentArea.getSelectionStart();
        int end = noteContentArea.getSelectionEnd();
        String selected = noteContentArea.getSelectedText();
        if (selected == null || selected.isEmpty()) {
            selected = "text";
        }
        String replacement = before + selected + after;
        noteContentArea.replaceRange(replacement, start, end);
        noteContentArea.requestFocusInWindow();
    }

    private void styleCheckbox(JCheckBox checkBox) {
        checkBox.setForeground(Theme.TEXT);
        checkBox.setFont(Theme.bodyFont());
        checkBox.setFocusPainted(false);
        checkBox.setBorder(BorderFactory.createEmptyBorder());
    }

    private List<Note> filteredNotes(ClientViewState viewState) {
        List<Note> notes = new ArrayList<>();
        for (Note note : viewState.getSnapshot().getNotes()) {
            if (note.isArchived() == archiveView) {
                notes.add(note);
            }
        }
        return notes;
    }

    private void toggleArchiveView(boolean nextArchiveView) {
        rememberCurrentNoteSelection();
        archiveView = nextArchiveView;
        refreshFromState();
    }

    private void rememberCurrentNoteSelection() {
        String selectedId = selectedNoteId();
        if (archiveView) {
            archivedNoteSelectionId = selectedId;
        } else {
            activeNoteSelectionId = selectedId;
        }
    }

    private void setNoteEditorEnabled(boolean enabled) {
        noteTitleField.setEnabled(enabled);
        noteContentArea.setEnabled(enabled);
        pinNoteCheckBox.setEnabled(enabled);
        archiveNoteCheckBox.setEnabled(enabled);
    }

    private Note selectedNote() {
        NoteListEntry entry = noteList.getSelectedValue();
        return entry == null ? null : entry.note();
    }

    private void selectNote(String noteId) {
        for (int i = 0; i < noteListModel.size(); i++) {
            NoteListEntry entry = noteListModel.get(i);
            if (entry.note() != null && Objects.equals(entry.note().getId(), noteId)) {
                noteList.setSelectedIndex(i);
                noteList.ensureIndexIsVisible(i);
                return;
            }
        }
    }

    private void selectTimer(String timerId) {
        for (int i = 0; i < timerListModel.size(); i++) {
            if (Objects.equals(timerListModel.get(i).getId(), timerId)) {
                timerList.setSelectedIndex(i);
                timerList.ensureIndexIsVisible(i);
                return;
            }
        }
    }

    private String selectedNoteId() {
        Note note = selectedNote();
        return note == null ? null : note.getId();
    }

    private String selectedTimerId() {
        TimerEntry timer = timerList.getSelectedValue();
        return timer == null ? null : timer.getId();
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0, millis / 1000);
        long days = totalSeconds / 86_400;
        long hours = (totalSeconds % 86_400) / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;
        long seconds = totalSeconds % 60;
        if (days > 0) {
            return String.format("%dd %02d:%02d:%02d", days, hours, minutes, seconds);
        }
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void runAsync(ThrowingRunnable task) {
        runAsync(task, false);
    }

    private void runAsync(ThrowingRunnable task, boolean silentOnError) {
        background.submit(() -> {
            try {
                task.run();
            } catch (Exception exception) {
                if (!silentOnError) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, exception.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE));
                }
            }
        });
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private enum NoteListAction {
        OPEN_ARCHIVE,
        BACK_TO_NOTES
    }

    private record NoteListEntry(Note note, String title, String subtitle, NoteListAction action) {
        private static NoteListEntry noteEntry(Note note) {
            return new NoteListEntry(note, null, null, null);
        }

        private static NoteListEntry archiveEntry() {
            return new NoteListEntry(null, "Архив", "Открыть архив заметок", NoteListAction.OPEN_ARCHIVE);
        }

        private static NoteListEntry backEntry() {
            return new NoteListEntry(null, "Назад к заметкам", "Вернуться к активным заметкам", NoteListAction.BACK_TO_NOTES);
        }

        private boolean navigation() {
            return action != null;
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private final class HeaderDragAdapter extends java.awt.event.MouseAdapter {
        @Override
        public void mousePressed(java.awt.event.MouseEvent event) {
            dragStart = event.getPoint();
        }

        @Override
        public void mouseDragged(java.awt.event.MouseEvent event) {
            Point location = getLocation();
            setLocation(location.x + event.getX() - dragStart.x, location.y + event.getY() - dragStart.y);
        }
    }
}
