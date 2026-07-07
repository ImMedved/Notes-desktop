package com.notes.client.config;

public class ClientConfig {
    private String serverBaseUrl = "http://127.0.0.1:8080";
    private String apiKey = "change-me";
    private int syncIntervalSeconds = 60;
    private boolean alwaysOnTop = true;
    private String clientId = "windows-widget";

    public String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public void setServerBaseUrl(String serverBaseUrl) {
        this.serverBaseUrl = serverBaseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getSyncIntervalSeconds() {
        return syncIntervalSeconds;
    }

    public void setSyncIntervalSeconds(int syncIntervalSeconds) {
        this.syncIntervalSeconds = syncIntervalSeconds;
    }

    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
