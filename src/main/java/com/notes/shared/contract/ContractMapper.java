package com.notes.shared.contract;

import com.notes.shared.json.JsonUtil;
import com.notes.shared.model.Note;
import com.notes.shared.model.ServerSnapshot;
import com.notes.shared.model.TimerEntry;
import com.notes.shared.model.TimerMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ContractMapper {
    private ContractMapper() {
    }

    public static Map<String, Object> noteToMap(Note note) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", note.getId());
        map.put("title", note.getTitle());
        map.put("content", note.getContent());
        map.put("pinned", note.isPinned());
        map.put("archived", note.isArchived());
        map.put("createdAt", note.getCreatedAt());
        map.put("updatedAt", note.getUpdatedAt());
        return map;
    }

    public static Note noteFromMap(Map<String, Object> map) {
        Note note = new Note();
        note.setId(JsonUtil.asString(map.get("id")));
        note.setTitle(JsonUtil.asString(map.get("title")));
        note.setContent(JsonUtil.asString(map.get("content")));
        note.setPinned(JsonUtil.asBoolean(map.get("pinned")));
        note.setArchived(JsonUtil.asBoolean(map.get("archived")));
        note.setCreatedAt(JsonUtil.asLong(map.get("createdAt")));
        note.setUpdatedAt(JsonUtil.asLong(map.get("updatedAt")));
        return note;
    }

    public static Map<String, Object> timerToMap(TimerEntry timer) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", timer.getId());
        map.put("name", timer.getName());
        map.put("mode", timer.getMode().name());
        map.put("durationMillis", timer.getDurationMillis());
        map.put("startedAt", timer.getStartedAt());
        map.put("accumulatedMillis", timer.getAccumulatedMillis());
        map.put("running", timer.isRunning());
        map.put("createdAt", timer.getCreatedAt());
        map.put("updatedAt", timer.getUpdatedAt());
        return map;
    }

    public static TimerEntry timerFromMap(Map<String, Object> map) {
        TimerEntry timer = new TimerEntry();
        timer.setId(JsonUtil.asString(map.get("id")));
        timer.setName(JsonUtil.asString(map.get("name")));
        String mode = JsonUtil.asString(map.get("mode"));
        timer.setMode("STOPWATCH".equalsIgnoreCase(mode) ? TimerMode.STOPWATCH : TimerMode.COUNTDOWN);
        timer.setDurationMillis(JsonUtil.asLong(map.get("durationMillis")));
        timer.setStartedAt(JsonUtil.asLong(map.get("startedAt")));
        timer.setAccumulatedMillis(JsonUtil.asLong(map.get("accumulatedMillis")));
        timer.setRunning(JsonUtil.asBoolean(map.get("running")));
        timer.setCreatedAt(JsonUtil.asLong(map.get("createdAt")));
        timer.setUpdatedAt(JsonUtil.asLong(map.get("updatedAt")));
        return timer;
    }

    public static Map<String, Object> snapshotToMap(ServerSnapshot snapshot) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("revision", snapshot.getRevision());
        map.put("serverTimeEpochMillis", snapshot.getServerTimeEpochMillis());
        map.put("notes", snapshot.getNotes().stream().map(ContractMapper::noteToMap).toList());
        map.put("timers", snapshot.getTimers().stream().map(ContractMapper::timerToMap).toList());
        return map;
    }

    public static ServerSnapshot snapshotFromMap(Map<String, Object> map) {
        ServerSnapshot snapshot = new ServerSnapshot();
        snapshot.setRevision(JsonUtil.asLong(map.get("revision")));
        snapshot.setServerTimeEpochMillis(JsonUtil.asLong(map.get("serverTimeEpochMillis")));

        List<Note> notes = new ArrayList<>();
        for (Object value : JsonUtil.asList(map.get("notes"))) {
            notes.add(noteFromMap(JsonUtil.asObject(value)));
        }
        notes.sort(Comparator.comparing(Note::isArchived)
                .thenComparing(Note::isPinned, Comparator.reverseOrder())
                .thenComparing(Note::getUpdatedAt, Comparator.reverseOrder()));
        snapshot.setNotes(notes);

        List<TimerEntry> timers = new ArrayList<>();
        for (Object value : JsonUtil.asList(map.get("timers"))) {
            timers.add(timerFromMap(JsonUtil.asObject(value)));
        }
        timers.sort(Comparator.comparing(TimerEntry::getUpdatedAt, Comparator.reverseOrder()));
        snapshot.setTimers(timers);

        return snapshot;
    }

    public static Map<String, Object> error(String code, String message) {
        return Map.of(
                "error", Map.of(
                        "code", code,
                        "message", message
                )
        );
    }
}
