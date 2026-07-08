package com.notes.client.ui.components;

import com.notes.client.ui.Theme;
import com.notes.client.ui.notes.NoteListEntry;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class NotesTabPanel extends JPanel {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final String PREVIEW_CARD = "preview";
    private static final String EDIT_CARD = "edit";

    private final DefaultListModel<NoteListEntry> noteListModel = new DefaultListModel<>();
    private final JList<NoteListEntry> noteList = new JList<>(noteListModel);
    private final JButton addButton = Theme.accentButton("Новая");
    private final JButton deleteButton = Theme.button("Удалить");
    private final JButton editSaveButton = Theme.button("Редактировать");
    private final JButton pinButton = Theme.button("Закрепить");
    private final JButton archiveButton = Theme.button("Архивировать");
    private final JTextField noteTitleField = Theme.textField();
    private final JTextArea noteContentArea = Theme.textArea();
    private final JEditorPane previewPane = Theme.htmlPane();
    private final JButton heading1Button = Theme.button("H1");
    private final JButton heading2Button = Theme.button("H2");
    private final JButton listButton = Theme.button("List");
    private final JButton taskButton = Theme.button("Task");
    private final JButton quoteButton = Theme.button("Quote");
    private final JButton boldButton = Theme.button("Bold");
    private final JButton codeButton = Theme.button("Code");
    private final JButton linkButton = Theme.button("Link");
    private final javax.swing.JLabel noteMetaLabel = Theme.mutedLabel("Локальный кеш");
    private final JPanel markdownToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JPanel contentCardPanel = new JPanel(new CardLayout());

    public NotesTabPanel() {
        super(new BorderLayout(16, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        build();
    }

    private void build() {
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
                    String prefix = entry.note().isPinned() ? "\u2022 " : "";
                    text = "<html><b>" + escape(prefix + entry.note().getTitle()) + "</b><br/><span style='color:#9AA6B2;'>"
                            + DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(entry.note().getUpdatedAt())) + "</span></html>";
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
        listActions.add(addButton);
        listActions.add(deleteButton);
        listPanel.add(listActions, BorderLayout.SOUTH);
        listPanel.setPreferredSize(new Dimension(300, 0));

        JPanel editor = Theme.panel();
        editor.setLayout(new BorderLayout(12, 12));
        editor.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel editorTop = new JPanel(new BorderLayout(12, 8));
        editorTop.setBackground(Theme.PANEL);

        JPanel titleRow = new JPanel(new BorderLayout(8, 0));
        titleRow.setBackground(Theme.PANEL);
        noteTitleField.setPreferredSize(new Dimension(320, 40));
        titleRow.add(editSaveButton, BorderLayout.WEST);
        titleRow.add(noteTitleField, BorderLayout.CENTER);

        JPanel noteActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        noteActions.setBackground(Theme.PANEL);
        noteActions.add(pinButton);
        noteActions.add(archiveButton);

        editorTop.add(titleRow, BorderLayout.CENTER);
        editorTop.add(noteActions, BorderLayout.EAST);

        markdownToolbar.setBackground(Theme.PANEL);
        markdownToolbar.add(heading1Button);
        markdownToolbar.add(heading2Button);
        markdownToolbar.add(listButton);
        markdownToolbar.add(taskButton);
        markdownToolbar.add(quoteButton);
        markdownToolbar.add(boldButton);
        markdownToolbar.add(codeButton);
        markdownToolbar.add(linkButton);

        contentCardPanel.setBackground(Theme.PANEL);
        contentCardPanel.add(Theme.scrollPane(previewPane), PREVIEW_CARD);
        contentCardPanel.add(Theme.scrollPane(noteContentArea), EDIT_CARD);

        JPanel topBlock = new JPanel();
        topBlock.setBackground(Theme.PANEL);
        topBlock.setLayout(new BoxLayout(topBlock, BoxLayout.Y_AXIS));
        topBlock.add(editorTop);
        topBlock.add(Box.createVerticalStrut(12));
        topBlock.add(markdownToolbar);

        JPanel bottomMeta = new JPanel(new BorderLayout());
        bottomMeta.setBackground(Theme.PANEL);
        bottomMeta.add(noteMetaLabel, BorderLayout.WEST);

        editor.add(topBlock, BorderLayout.NORTH);
        editor.add(contentCardPanel, BorderLayout.CENTER);
        editor.add(bottomMeta, BorderLayout.SOUTH);

        add(listPanel, BorderLayout.WEST);
        add(editor, BorderLayout.CENTER);
        setEditMode(false);
    }

    public DefaultListModel<NoteListEntry> getNoteListModel() {
        return noteListModel;
    }

    public JList<NoteListEntry> getNoteList() {
        return noteList;
    }

    public JButton getAddButton() {
        return addButton;
    }

    public JButton getDeleteButton() {
        return deleteButton;
    }

    public JButton getEditSaveButton() {
        return editSaveButton;
    }

    public JButton getPinButton() {
        return pinButton;
    }

    public JButton getArchiveButton() {
        return archiveButton;
    }

    public JTextField getNoteTitleField() {
        return noteTitleField;
    }

    public JTextArea getNoteContentArea() {
        return noteContentArea;
    }

    public JButton getHeading1Button() {
        return heading1Button;
    }

    public JButton getHeading2Button() {
        return heading2Button;
    }

    public JButton getListButton() {
        return listButton;
    }

    public JButton getTaskButton() {
        return taskButton;
    }

    public JButton getQuoteButton() {
        return quoteButton;
    }

    public JButton getBoldButton() {
        return boldButton;
    }

    public JButton getCodeButton() {
        return codeButton;
    }

    public JButton getLinkButton() {
        return linkButton;
    }

    public JEditorPane getPreviewPane() {
        return previewPane;
    }

    public javax.swing.JLabel getNoteMetaLabel() {
        return noteMetaLabel;
    }

    public void setPreviewHtml(String html) {
        previewPane.setText(html);
        previewPane.setCaretPosition(0);
    }

    public void setEditMode(boolean editMode) {
        CardLayout layout = (CardLayout) contentCardPanel.getLayout();
        layout.show(contentCardPanel, editMode ? EDIT_CARD : PREVIEW_CARD);
        markdownToolbar.setVisible(editMode);
        editSaveButton.setText(editMode ? "Сохранить" : "Редактировать");
    }

    public void setPinArchivedState(boolean pinned, boolean archived) {
        pinButton.setText(pinned ? "Открепить" : "Закрепить");
        archiveButton.setText(archived ? "Разархивировать" : "Архивировать");
    }

    public void setEditorEnabled(boolean enabled) {
        noteTitleField.setEnabled(enabled);
        noteContentArea.setEnabled(enabled);
        editSaveButton.setEnabled(enabled);
        pinButton.setEnabled(enabled);
        archiveButton.setEnabled(enabled);
        heading1Button.setEnabled(enabled);
        heading2Button.setEnabled(enabled);
        listButton.setEnabled(enabled);
        taskButton.setEnabled(enabled);
        quoteButton.setEnabled(enabled);
        boldButton.setEnabled(enabled);
        codeButton.setEnabled(enabled);
        linkButton.setEnabled(enabled);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
