package com.notes.shared.model;

import java.util.ArrayList;
import java.util.List;

public class ServerSnapshot {
    private long revision;
    private long serverTimeEpochMillis;
    private List<Note> notes = new ArrayList<>();
    private List<TimerEntry> timers = new ArrayList<>();

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    public long getServerTimeEpochMillis() {
        return serverTimeEpochMillis;
    }

    public void setServerTimeEpochMillis(long serverTimeEpochMillis) {
        this.serverTimeEpochMillis = serverTimeEpochMillis;
    }

    public List<Note> getNotes() {
        return notes;
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
    }

    public List<TimerEntry> getTimers() {
        return timers;
    }

    public void setTimers(List<TimerEntry> timers) {
        this.timers = timers;
    }
}
