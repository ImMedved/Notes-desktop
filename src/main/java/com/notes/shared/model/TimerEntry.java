package com.notes.shared.model;

import java.util.UUID;

public class TimerEntry {
    private String id = UUID.randomUUID().toString();
    private String name = "";
    private TimerMode mode = TimerMode.COUNTDOWN;
    private long durationMillis;
    private long startedAt;
    private long accumulatedMillis;
    private boolean running;
    private long createdAt = System.currentTimeMillis();
    private long updatedAt = System.currentTimeMillis();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TimerMode getMode() {
        return mode;
    }

    public void setMode(TimerMode mode) {
        this.mode = mode;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getAccumulatedMillis() {
        return accumulatedMillis;
    }

    public void setAccumulatedMillis(long accumulatedMillis) {
        this.accumulatedMillis = accumulatedMillis;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getElapsedMillis(long now) {
        long elapsed = accumulatedMillis;
        if (running && startedAt > 0) {
            elapsed += Math.max(0, now - startedAt);
        }
        return elapsed;
    }

    public long getRemainingMillis(long now) {
        return Math.max(0, durationMillis - getElapsedMillis(now));
    }
}
