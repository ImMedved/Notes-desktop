package com.notes.client.ui;

import com.notes.client.config.ClientConfig;
import com.notes.client.integration.DesktopShellIntegration;
import com.notes.client.model.ClientViewState;
import com.notes.client.service.ClientAppService;
import com.notes.client.ui.components.HeaderPanel;
import com.notes.client.ui.components.NotesTabPanel;
import com.notes.client.ui.components.SyncTabPanel;
import com.notes.client.ui.components.TimersTabPanel;
import com.notes.client.ui.notes.MarkdownPreviewRenderer;
import com.notes.client.ui.notes.NoteListAction;
import com.notes.client.ui.notes.NoteListEntry;
import com.notes.shared.model.Note;
import com.notes.shared.model.TimerEntry;
import com.notes.shared.model.TimerMode;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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

    private final HeaderPanel headerPanel = new HeaderPanel();
    private final NotesTabPanel notesTabPanel = new NotesTabPanel();
    private final TimersTabPanel timersTabPanel = new TimersTabPanel();
    private final SyncTabPanel syncTabPanel = new SyncTabPanel();
    private final Timer noteAutoSaveTimer;
    private final Timer timerRefreshTimer;
    private final Timer autoSyncTimer;

    private DesktopShellIntegration shellIntegration;
    private boolean suppressNoteEvents;
    private boolean suppressNoteSelectionEvents;
    private boolean noteEditMode;
    private boolean archiveView;
    private String activeNoteSelectionId;
    private String archivedNoteSelectionId;
    private Point dragStart;

    public WidgetFrame(ClientAppService appService) {
        this.appService = appService;

        Theme.install();
        setTitle("Notes Widget Client");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setUndecorated(true);
        setSize(1120, 760);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BACKGROUND);

        ClientViewState viewState = appService.snapshotView();
        setAlwaysOnTop(viewState.getConfig().isAlwaysOnTop());

        add(headerPanel, BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        bindHeader();
        bindNotes();
        bindTimers();
        bindSync();
        installShellIntegration();

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

    private JTabbedPane buildContent() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.bodyFont());
        tabs.setBackground(Theme.PANEL);
        tabs.setForeground(Theme.TEXT);
        tabs.setBorder(BorderFactory.createEmptyBorder(0, 18, 18, 18));
        tabs.add("Notes", notesTabPanel);
        tabs.add("Timers", timersTabPanel);
        tabs.add("Sync", syncTabPanel);
        return tabs;
    }

    private void bindHeader() {
        headerPanel.getSyncButton().addActionListener(event -> syncNowAsync());
        headerPanel.getMinimizeButton().addActionListener(event -> hideToTray());
        headerPanel.getPinButton().addActionListener(event -> {
            boolean next = !isAlwaysOnTop();
            setAlwaysOnTop(next);
            ClientConfig config = appService.snapshotView().getConfig();
            config.setAlwaysOnTop(next);
            appService.saveConfig(config);
        });
        headerPanel.getCloseButton().addActionListener(event -> exitApplication());

        HeaderDragAdapter dragAdapter = new HeaderDragAdapter();
        headerPanel.addMouseListener(dragAdapter);
        headerPanel.addMouseMotionListener(dragAdapter);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent event) {
                hideToTray();
            }
        });
    }

    private void bindNotes() {
        notesTabPanel.getAddButton().addActionListener(event -> runAsync(() -> {
            Note created = appService.createNote();
            SwingUtilities.invokeLater(() -> {
                archiveView = false;
                activeNoteSelectionId = created.getId();
                refreshFromState();
                selectNote(created.getId());
            });
        }));
        notesTabPanel.getDeleteButton().addActionListener(event -> runAsync(this::deleteSelectedNote));
        notesTabPanel.getEditSaveButton().addActionListener(event -> onEditSaveClicked());
        notesTabPanel.getPinButton().addActionListener(event -> runAsync(this::toggleSelectedPinState));
        notesTabPanel.getArchiveButton().addActionListener(event -> runAsync(this::toggleSelectedArchiveState));
        notesTabPanel.getHeading1Button().addActionListener(event -> insertMarkdown("# "));
        notesTabPanel.getHeading2Button().addActionListener(event -> insertMarkdown("## "));
        notesTabPanel.getListButton().addActionListener(event -> insertMarkdown("- "));
        notesTabPanel.getTaskButton().addActionListener(event -> insertMarkdown("- [ ] "));
        notesTabPanel.getQuoteButton().addActionListener(event -> insertMarkdown("> "));
        notesTabPanel.getBoldButton().addActionListener(event -> wrapMarkdown("**", "**"));
        notesTabPanel.getCodeButton().addActionListener(event -> wrapMarkdown("```\n", "\n```"));
        notesTabPanel.getLinkButton().addActionListener(event -> wrapMarkdown("[", "](https://)"));

        notesTabPanel.getNoteList().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !suppressNoteSelectionEvents) {
                NoteListEntry entry = notesTabPanel.getNoteList().getSelectedValue();
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
            public void insertUpdate(DocumentEvent event) {
                scheduleNoteSave();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                scheduleNoteSave();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                scheduleNoteSave();
            }
        };
        notesTabPanel.getNoteTitleField().getDocument().addDocumentListener(noteListener);
    }

    private void bindTimers() {
        timersTabPanel.getTimerList().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                refreshLiveTimerState();
            }
        });
        timersTabPanel.getAddTimerButton().addActionListener(event -> openCountdownDialog());
        timersTabPanel.getAddStopwatchButton().addActionListener(event -> openStopwatchDialog());
        timersTabPanel.getToggleButton().addActionListener(event -> runAsync(this::toggleSelectedTimer));
        timersTabPanel.getResetButton().addActionListener(event -> runAsync(this::resetSelectedTimer));
        timersTabPanel.getDeleteButton().addActionListener(event -> runAsync(this::deleteSelectedTimer));
    }

    private void bindSync() {
        ClientConfig config = appService.snapshotView().getConfig();
        syncTabPanel.getServerUrlField().setText(config.getServerBaseUrl());
        syncTabPanel.getApiKeyField().setText(config.getApiKey());
        syncTabPanel.getClientIdField().setText(config.getClientId());
        syncTabPanel.getIntervalSpinner().setValue(config.getSyncIntervalSeconds());

        syncTabPanel.getSaveButton().addActionListener(event -> saveSyncSettings());
        syncTabPanel.getTestButton().addActionListener(event -> runAsync(() -> appService.testConnection()));
        syncTabPanel.getSyncButton().addActionListener(event -> syncNowAsync());
    }

    private void installShellIntegration() {
        shellIntegration = new DesktopShellIntegration(this::isVisible, this::showFromTray, this::hideToTray, this::exitApplication);
        shellIntegration.install();
        shellIntegration.showTrayMessage("Notes Widget Client", "Свернуть/развернуть: Ctrl+Alt+Space");
    }

    private void showFromTray() {
        if (!isVisible()) {
            setVisible(true);
        }
        setState(JFrame.NORMAL);
        toFront();
        requestFocus();
    }

    private void hideToTray() {
        if (shellIntegration == null || !shellIntegration.hasTrayIcon()) {
            setState(JFrame.ICONIFIED);
            return;
        }
        setVisible(false);
    }

    private void exitApplication() {
        background.shutdownNow();
        if (shellIntegration != null) {
            shellIntegration.close();
        }
        dispose();
        System.exit(0);
    }

    private void onEditSaveClicked() {
        if (selectedNote() == null) {
            return;
        }
        if (!noteEditMode) {
            setNoteEditMode(true);
            return;
        }
        runAsync(() -> {
            persistSelectedNote();
            SwingUtilities.invokeLater(() -> setNoteEditMode(false));
        });
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
        notesTabPanel.getNoteListModel().clear();
        notesTabPanel.getNoteListModel().addElement(archiveView ? NoteListEntry.backEntry() : NoteListEntry.archiveEntry());
        for (Note note : filteredNotes(viewState)) {
            notesTabPanel.getNoteListModel().addElement(NoteListEntry.noteEntry(note));
            if (Objects.equals(note.getId(), selectedId)) {
                selectionStillExists = true;
            }
        }

        if (selectedId != null) {
            selectNote(selectedId);
        }
        if (notesTabPanel.getNoteList().getSelectedIndex() == -1 && notesTabPanel.getNoteListModel().size() > 1) {
            notesTabPanel.getNoteList().setSelectedIndex(1);
        }
        suppressNoteSelectionEvents = false;

        if (!selectionStillExists || selectedId == null) {
            loadSelectedNoteIntoEditor();
        }
    }

    private void refreshTimerList(ClientViewState viewState) {
        String selectedId = selectedTimerId();
        timersTabPanel.getTimerListModel().clear();
        for (TimerEntry timer : viewState.getSnapshot().getTimers()) {
            timersTabPanel.getTimerListModel().addElement(timer);
        }
        if (selectedId != null) {
            selectTimer(selectedId);
        }
        if (timersTabPanel.getTimerList().getSelectedIndex() == -1 && !timersTabPanel.getTimerListModel().isEmpty()) {
            timersTabPanel.getTimerList().setSelectedIndex(0);
        }
        refreshLiveTimerState();
    }

    private void refreshSyncPanel(ClientViewState state) {
        syncTabPanel.getServerUrlField().setText(state.getConfig().getServerBaseUrl());
        syncTabPanel.getApiKeyField().setText(state.getConfig().getApiKey());
        syncTabPanel.getClientIdField().setText(state.getConfig().getClientId());
        syncTabPanel.getIntervalSpinner().setValue(state.getConfig().getSyncIntervalSeconds());
        autoSyncTimer.setDelay(state.getConfig().getSyncIntervalSeconds() * 1000);
        autoSyncTimer.setInitialDelay(state.getConfig().getSyncIntervalSeconds() * 1000);

        if (state.isServerReachable()) {
            syncTabPanel.getConnectionStatusLabel().setText("Сервер доступен");
            syncTabPanel.getConnectionStatusLabel().setForeground(new Color(115, 223, 160));
        } else if (state.getLastError() != null && !state.getLastError().isBlank()) {
            syncTabPanel.getConnectionStatusLabel().setText(state.getLastError());
            syncTabPanel.getConnectionStatusLabel().setForeground(new Color(255, 190, 92));
        } else {
            syncTabPanel.getConnectionStatusLabel().setText("Сервер: неизвестно");
            syncTabPanel.getConnectionStatusLabel().setForeground(Theme.MUTED);
        }

        syncTabPanel.getLastSyncLabel().setText(state.getLastSuccessfulSyncEpochMillis() == 0
                ? "Последняя синхронизация: еще не было"
                : "Последняя синхронизация: " + DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(state.getLastSuccessfulSyncEpochMillis())));
        syncTabPanel.getRevisionLabel().setText("Revision: " + state.getSnapshot().getRevision());
    }

    private void loadSelectedNoteIntoEditor() {
        Note note = selectedNote();
        suppressNoteEvents = true;

        setNoteEditMode(false);
        if (note == null) {
            notesTabPanel.getNoteTitleField().setText("");
            notesTabPanel.getNoteContentArea().setText("");
            notesTabPanel.setPreviewHtml(MarkdownPreviewRenderer.render(""));
            notesTabPanel.setPinArchivedState(false, false);
            notesTabPanel.getNoteMetaLabel().setText(archiveView ? "Выберите заметку из архива" : "Выберите заметку");
        } else {
            notesTabPanel.getNoteTitleField().setText(note.getTitle());
            notesTabPanel.getNoteContentArea().setText(note.getContent());
            notesTabPanel.setPreviewHtml(MarkdownPreviewRenderer.render(note.getContent()));
            notesTabPanel.setPinArchivedState(note.isPinned(), note.isArchived());
            notesTabPanel.getNoteMetaLabel().setText("Изменена: " + DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(note.getUpdatedAt())));
        }
        notesTabPanel.setEditorEnabled(note != null);
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

        Note updated = copyOf(selected);
        updated.setTitle(notesTabPanel.getNoteTitleField().getText().trim().isBlank()
                ? "Без названия"
                : notesTabPanel.getNoteTitleField().getText().trim());
        updated.setContent(notesTabPanel.getNoteContentArea().getText());
        updated.setUpdatedAt(System.currentTimeMillis());
        appService.upsertNote(updated);
    }

    private void deleteSelectedNote() throws Exception {
        Note selected = selectedNote();
        if (selected == null) {
            return;
        }
        if (archiveView) {
            archivedNoteSelectionId = null;
        } else {
            activeNoteSelectionId = null;
        }
        appService.deleteNote(selected.getId());
    }

    private void toggleSelectedPinState() throws Exception {
        Note selected = selectedNote();
        if (selected == null) {
            return;
        }
        Note updated = copyOf(selected);
        updated.setPinned(!selected.isPinned());
        appService.upsertNote(updated);
    }

    private void toggleSelectedArchiveState() throws Exception {
        Note selected = selectedNote();
        if (selected == null) {
            return;
        }
        Note updated = copyOf(selected);
        updated.setArchived(!selected.isArchived());
        if (archiveView) {
            archivedNoteSelectionId = null;
        } else {
            activeNoteSelectionId = null;
        }
        appService.upsertNote(updated);
    }

    private void refreshLiveTimerState() {
        TimerEntry timer = timersTabPanel.getTimerList().getSelectedValue();
        if (timer == null) {
            timersTabPanel.getTimerDetailLabel().setText("00:00:00");
            timersTabPanel.getTimerMetaLabel().setText("Нет выбранного таймера");
            return;
        }

        long now = System.currentTimeMillis();
        String value = timer.getMode() == TimerMode.STOPWATCH
                ? formatDuration(timer.getElapsedMillis(now))
                : formatDuration(timer.getRemainingMillis(now));
        timersTabPanel.getTimerDetailLabel().setText(value);

        String label = timer.getMode() == TimerMode.STOPWATCH ? "Секундомер" : "Таймер";
        String state = timer.isRunning() ? "работает" : "на паузе";
        timersTabPanel.getTimerMetaLabel().setText(label + " \"" + timer.getName() + "\" " + state);
    }

    private void toggleSelectedTimer() throws Exception {
        TimerEntry selected = timersTabPanel.getTimerList().getSelectedValue();
        if (selected != null) {
            appService.toggleTimer(selected.getId());
        }
    }

    private void resetSelectedTimer() throws Exception {
        TimerEntry selected = timersTabPanel.getTimerList().getSelectedValue();
        if (selected != null) {
            appService.resetTimer(selected.getId());
        }
    }

    private void deleteSelectedTimer() throws Exception {
        TimerEntry selected = timersTabPanel.getTimerList().getSelectedValue();
        if (selected != null) {
            appService.deleteTimer(selected.getId());
        }
    }

    private void openCountdownDialog() {
        javax.swing.JTextField nameField = Theme.textField();
        javax.swing.JSpinner days = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(0, 0, 365, 1));
        javax.swing.JSpinner hours = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(0, 0, 23, 1));
        javax.swing.JSpinner minutes = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(15, 0, 59, 1));
        javax.swing.JSpinner seconds = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(0, 0, 59, 1));

        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridLayout(0, 2, 8, 8));
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
        config.setServerBaseUrl(syncTabPanel.getServerUrlField().getText().trim());
        config.setApiKey(syncTabPanel.getApiKeyField().getText().trim());
        config.setClientId(syncTabPanel.getClientIdField().getText().trim());
        config.setSyncIntervalSeconds((int) syncTabPanel.getIntervalSpinner().getValue());
        config.setAlwaysOnTop(isAlwaysOnTop());
        appService.saveConfig(config);
        runAsync(() -> appService.testConnection());
    }

    private void scheduleNoteSave() {
        if (!suppressNoteEvents) {
            noteAutoSaveTimer.restart();
        }
    }

    private void insertMarkdown(String prefix) {
        int start = notesTabPanel.getNoteContentArea().getSelectionStart();
        int end = notesTabPanel.getNoteContentArea().getSelectionEnd();
        String selected = notesTabPanel.getNoteContentArea().getSelectedText();
        if (selected == null) {
            selected = "";
        }
        String replacement = prefix + selected;
        notesTabPanel.getNoteContentArea().replaceRange(replacement, start, end);
        notesTabPanel.getNoteContentArea().requestFocusInWindow();
        notesTabPanel.getNoteContentArea().select(start + replacement.length(), start + replacement.length());
    }

    private void wrapMarkdown(String before, String after) {
        int start = notesTabPanel.getNoteContentArea().getSelectionStart();
        int end = notesTabPanel.getNoteContentArea().getSelectionEnd();
        String selected = notesTabPanel.getNoteContentArea().getSelectedText();
        if (selected == null || selected.isEmpty()) {
            selected = "text";
        }
        String replacement = before + selected + after;
        notesTabPanel.getNoteContentArea().replaceRange(replacement, start, end);
        notesTabPanel.getNoteContentArea().requestFocusInWindow();
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

    private void setNoteEditMode(boolean editMode) {
        noteEditMode = editMode && selectedNote() != null;
        notesTabPanel.setEditMode(noteEditMode);
    }

    private Note selectedNote() {
        NoteListEntry entry = notesTabPanel.getNoteList().getSelectedValue();
        return entry == null ? null : entry.note();
    }

    private void selectNote(String noteId) {
        for (int i = 0; i < notesTabPanel.getNoteListModel().size(); i++) {
            NoteListEntry entry = notesTabPanel.getNoteListModel().get(i);
            if (entry.note() != null && Objects.equals(entry.note().getId(), noteId)) {
                notesTabPanel.getNoteList().setSelectedIndex(i);
                notesTabPanel.getNoteList().ensureIndexIsVisible(i);
                return;
            }
        }
    }

    private void selectTimer(String timerId) {
        for (int i = 0; i < timersTabPanel.getTimerListModel().size(); i++) {
            if (Objects.equals(timersTabPanel.getTimerListModel().get(i).getId(), timerId)) {
                timersTabPanel.getTimerList().setSelectedIndex(i);
                timersTabPanel.getTimerList().ensureIndexIsVisible(i);
                return;
            }
        }
    }

    private String selectedNoteId() {
        Note note = selectedNote();
        return note == null ? null : note.getId();
    }

    private String selectedTimerId() {
        TimerEntry timer = timersTabPanel.getTimerList().getSelectedValue();
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

    private Note copyOf(Note source) {
        Note copy = new Note();
        copy.setId(source.getId());
        copy.setTitle(source.getTitle());
        copy.setContent(source.getContent());
        copy.setPinned(source.isPinned());
        copy.setArchived(source.isArchived());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
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
