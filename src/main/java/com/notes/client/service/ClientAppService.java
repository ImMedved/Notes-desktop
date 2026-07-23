package com.notes.client.service;

import com.notes.client.api.ServerApiClient;
import com.notes.client.config.ClientConfig;
import com.notes.client.model.ClientViewState;
import com.notes.client.storage.ClientCacheStore;
import com.notes.client.storage.ClientConfigStore;
import com.notes.shared.model.Note;
import com.notes.shared.model.ServerSnapshot;
import com.notes.shared.model.TimerEntry;
import com.notes.shared.model.TimerMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientAppService {
    private final ClientConfigStore configStore;
    private final ClientCacheStore cacheStore;
    private final ServerApiClient apiClient = new ServerApiClient();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private ClientConfig config;
    private ServerSnapshot snapshot;
    private boolean serverReachable;
    private String lastError = "";
    private long lastSuccessfulSyncEpochMillis;

    public ClientAppService(ClientConfigStore configStore, ClientCacheStore cacheStore) {
        this.configStore = configStore;
        this.cacheStore = cacheStore;
        this.config = configStore.load();
        this.snapshot = cacheStore.load();
        ensureDefaults();
    }

    public synchronized ClientViewState snapshotView() {
        ClientViewState viewState = new ClientViewState();
        viewState.setConfig(copyConfig(config));
        viewState.setSnapshot(copySnapshot(snapshot));
        viewState.setServerReachable(serverReachable);
        viewState.setLastError(lastError);
        viewState.setLastSuccessfulSyncEpochMillis(lastSuccessfulSyncEpochMillis);
        return viewState;
    }

    public synchronized void saveConfig(ClientConfig newConfig) {
        if (newConfig.getSyncIntervalSeconds() < 15) {
            newConfig.setSyncIntervalSeconds(15);
        }
        this.config = copyConfig(newConfig);
        configStore.save(this.config);
        notifyListeners();
    }

    public synchronized void syncNow() throws Exception {
        ServerSnapshot fresh = apiClient.fetchSnapshot(config);
        this.snapshot = copySnapshot(fresh);
        this.serverReachable = true;
        this.lastError = "";
        this.lastSuccessfulSyncEpochMillis = System.currentTimeMillis();
        cacheStore.save(this.snapshot);
        notifyListeners();
    }

    public synchronized void testConnection() {
        this.serverReachable = apiClient.ping(config);
        if (serverReachable) {
            this.lastError = "";
        } else {
            this.lastError = "The server is unavailable at the current URL.";
        }
        notifyListeners();
    }

    public synchronized Note createNote() throws Exception {
        Note note = new Note();
        note.setId(UUID.randomUUID().toString());
        note.setTitle("New note");
        note.setContent("# New note\n\n- first thought");
        note.setPinned(false);
        note.setArchived(false);
        note.setCreatedAt(System.currentTimeMillis());
        note.setUpdatedAt(System.currentTimeMillis());
        apiClient.upsertNote(config, note);
        syncNow();
        return copyNote(note);
    }

    public synchronized void upsertNote(Note note) throws Exception {
        note.setUpdatedAt(System.currentTimeMillis());
        if (note.getCreatedAt() == 0) {
            note.setCreatedAt(System.currentTimeMillis());
        }
        apiClient.upsertNote(config, note);
        syncNow();
    }

    public synchronized void deleteNote(String noteId) throws Exception {
        apiClient.deleteNote(config, noteId);
        syncNow();
    }

    public synchronized TimerEntry createCountdown(String name, long durationMillis) throws Exception {
        TimerEntry timer = new TimerEntry();
        timer.setId(UUID.randomUUID().toString());
        timer.setMode(TimerMode.COUNTDOWN);
        timer.setName(name == null || name.isBlank() ? "Timer" : name.trim());
        timer.setDurationMillis(durationMillis);
        timer.setCreatedAt(System.currentTimeMillis());
        timer.setUpdatedAt(System.currentTimeMillis());
        apiClient.upsertTimer(config, timer);
        syncNow();
        return copyTimer(timer);
    }

    public synchronized TimerEntry createStopwatch(String name) throws Exception {
        return createStopwatch(name, System.currentTimeMillis());
    }

    public synchronized TimerEntry createStopwatch(String name, long startedAt) throws Exception {
        TimerEntry timer = new TimerEntry();
        timer.setId(UUID.randomUUID().toString());
        timer.setMode(TimerMode.STOPWATCH);
        timer.setName(name == null || name.isBlank() ? "Stopwatch" : name.trim());
        timer.setDurationMillis(365L * 24 * 60 * 60 * 1000);
        timer.setStartedAt(startedAt);
        timer.setAccumulatedMillis(0);
        timer.setRunning(true);
        timer.setCreatedAt(System.currentTimeMillis());
        timer.setUpdatedAt(System.currentTimeMillis());
        apiClient.upsertTimer(config, timer);
        syncNow();
        return copyTimer(timer);
    }

    public synchronized void upsertTimer(TimerEntry timer) throws Exception {
        timer.setUpdatedAt(System.currentTimeMillis());
        if (timer.getCreatedAt() == 0) {
            timer.setCreatedAt(System.currentTimeMillis());
        }
        apiClient.upsertTimer(config, timer);
        syncNow();
    }

    public synchronized void deleteTimer(String timerId) throws Exception {
        apiClient.deleteTimer(config, timerId);
        syncNow();
    }

    public synchronized void toggleTimer(String timerId) throws Exception {
        TimerEntry timer = findTimer(timerId);
        if (timer == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (timer.isRunning()) {
            timer.setAccumulatedMillis(timer.getElapsedMillis(now));
            timer.setStartedAt(0);
            timer.setRunning(false);
        } else {
            if (timer.getMode() == TimerMode.COUNTDOWN && timer.getRemainingMillis(now) == 0) {
                timer.setAccumulatedMillis(0);
            }
            timer.setStartedAt(now);
            timer.setRunning(true);
        }
        upsertTimer(timer);
    }

    public synchronized void resetTimer(String timerId) throws Exception {
        TimerEntry timer = findTimer(timerId);
        if (timer == null) {
            return;
        }
        timer.setStartedAt(0);
        timer.setAccumulatedMillis(0);
        timer.setRunning(false);
        upsertTimer(timer);
    }

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    public void shutdown() {
        configStore.save(config);
        cacheStore.save(snapshot);
    }

    private TimerEntry findTimer(String timerId) {
        return snapshot.getTimers().stream()
                .filter(timer -> timerId.equals(timer.getId()))
                .findFirst()
                .map(ClientAppService::copyTimer)
                .orElse(null);
    }

    private void ensureDefaults() {
        if (snapshot.getNotes().isEmpty()) {
            Note note = new Note();
            note.setTitle("Client ready");
            note.setContent("""
                    # Notes Client

                    Configure the Tailscale server URL and API key on the Sync tab.
                    After the first sync, the local cache will be replaced with the server snapshot.
                    """);
            snapshot.getNotes().add(note);
        }
        sortSnapshot(snapshot);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    private static ClientConfig copyConfig(ClientConfig source) {
        ClientConfig copy = new ClientConfig();
        copy.setServerBaseUrl(source.getServerBaseUrl());
        copy.setApiKey(source.getApiKey());
        copy.setSyncIntervalSeconds(source.getSyncIntervalSeconds());
        copy.setAlwaysOnTop(source.isAlwaysOnTop());
        copy.setClientId(source.getClientId());
        return copy;
    }

    private static ServerSnapshot copySnapshot(ServerSnapshot source) {
        ServerSnapshot copy = new ServerSnapshot();
        copy.setRevision(source.getRevision());
        copy.setServerTimeEpochMillis(source.getServerTimeEpochMillis());

        List<Note> notes = new ArrayList<>();
        for (Note note : source.getNotes()) {
            notes.add(copyNote(note));
        }
        copy.setNotes(notes);

        List<TimerEntry> timers = new ArrayList<>();
        for (TimerEntry timer : source.getTimers()) {
            timers.add(copyTimer(timer));
        }
        copy.setTimers(timers);
        sortSnapshot(copy);
        return copy;
    }

    private static Note copyNote(Note source) {
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

    private static TimerEntry copyTimer(TimerEntry source) {
        TimerEntry copy = new TimerEntry();
        copy.setId(source.getId());
        copy.setName(source.getName());
        copy.setMode(source.getMode());
        copy.setDurationMillis(source.getDurationMillis());
        copy.setStartedAt(source.getStartedAt());
        copy.setAccumulatedMillis(source.getAccumulatedMillis());
        copy.setRunning(source.isRunning());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }

    private static void sortSnapshot(ServerSnapshot snapshot) {
        snapshot.getNotes().sort(Comparator.comparing(Note::isArchived)
                .thenComparing(Note::isPinned, Comparator.reverseOrder())
                .thenComparing(Note::getUpdatedAt, Comparator.reverseOrder()));
        snapshot.getTimers().sort(Comparator.comparing(TimerEntry::getUpdatedAt, Comparator.reverseOrder()));
    }
}
