package com.notes.client.model;

import com.notes.client.config.ClientConfig;
import com.notes.shared.model.ServerSnapshot;

public class ClientViewState {
    private ClientConfig config;
    private ServerSnapshot snapshot;
    private long lastSuccessfulSyncEpochMillis;
    private boolean serverReachable;
    private String lastError = "";

    public ClientConfig getConfig() {
        return config;
    }

    public void setConfig(ClientConfig config) {
        this.config = config;
    }

    public ServerSnapshot getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(ServerSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public long getLastSuccessfulSyncEpochMillis() {
        return lastSuccessfulSyncEpochMillis;
    }

    public void setLastSuccessfulSyncEpochMillis(long lastSuccessfulSyncEpochMillis) {
        this.lastSuccessfulSyncEpochMillis = lastSuccessfulSyncEpochMillis;
    }

    public boolean isServerReachable() {
        return serverReachable;
    }

    public void setServerReachable(boolean serverReachable) {
        this.serverReachable = serverReachable;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
