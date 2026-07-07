package com.notes.client.api;

import com.notes.client.config.ClientConfig;
import com.notes.shared.contract.ContractMapper;
import com.notes.shared.json.JsonUtil;
import com.notes.shared.model.Note;
import com.notes.shared.model.ServerSnapshot;
import com.notes.shared.model.TimerEntry;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public class ServerApiClient {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public ServerSnapshot fetchSnapshot(ClientConfig config) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder(config, "/api/v1/snapshot")
                .GET()
                .build();
        String body = send(request);
        return ContractMapper.snapshotFromMap(JsonUtil.asObject(JsonUtil.parse(body)));
    }

    public void upsertNote(ClientConfig config, Note note) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder(config, "/api/v1/notes/" + note.getId())
                .PUT(HttpRequest.BodyPublishers.ofString(JsonUtil.stringify(ContractMapper.noteToMap(note)), StandardCharsets.UTF_8))
                .build();
        send(request);
    }

    public void deleteNote(ClientConfig config, String noteId) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder(config, "/api/v1/notes/" + noteId)
                .DELETE()
                .build();
        send(request);
    }

    public void upsertTimer(ClientConfig config, TimerEntry timer) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder(config, "/api/v1/timers/" + timer.getId())
                .PUT(HttpRequest.BodyPublishers.ofString(JsonUtil.stringify(ContractMapper.timerToMap(timer)), StandardCharsets.UTF_8))
                .build();
        send(request);
    }

    public void deleteTimer(ClientConfig config, String timerId) throws IOException, InterruptedException {
        HttpRequest request = requestBuilder(config, "/api/v1/timers/" + timerId)
                .DELETE()
                .build();
        send(request);
    }

    public boolean ping(ClientConfig config) {
        try {
            HttpRequest request = requestBuilder(config, "/health").GET().build();
            send(request);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private HttpRequest.Builder requestBuilder(ClientConfig config, String path) {
        String baseUrl = trimTrailingSlash(config.getServerBaseUrl());
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("X-Client-Id", blankToDefault(config.getClientId(), "windows-widget"))
                .header("X-Client-Platform", "windows")
                .header("X-Client-Version", "1.0.0");
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            builder.header("X-Notes-Api-Key", config.getApiKey().trim());
        }
        return builder;
    }

    private String send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        }
        Map<String, Object> parsed = JsonUtil.asObject(JsonUtil.parse(response.body()));
        Map<String, Object> error = JsonUtil.asObject(parsed.get("error"));
        String message = JsonUtil.asString(error.get("message"));
        throw new IOException(message.isBlank() ? "HTTP " + response.statusCode() : message);
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
