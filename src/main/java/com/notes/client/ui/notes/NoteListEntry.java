package com.notes.client.ui.notes;

import com.notes.shared.model.Note;

public record NoteListEntry(Note note, String title, String subtitle, NoteListAction action) {
    public static NoteListEntry noteEntry(Note note) {
        return new NoteListEntry(note, null, null, null);
    }

    public static NoteListEntry archiveEntry() {
        return new NoteListEntry(null, "Архив", "Открыть архив заметок", NoteListAction.OPEN_ARCHIVE);
    }

    public static NoteListEntry backEntry() {
        return new NoteListEntry(null, "Назад к заметкам", "Вернуться к активным заметкам", NoteListAction.BACK_TO_NOTES);
    }

    public boolean navigation() {
        return action != null;
    }
}
