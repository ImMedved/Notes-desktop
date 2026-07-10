package com.notes.client.ui.notes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MarkdownPreviewLayoutBuilder {
    private static final Pattern TASK_PATTERN = Pattern.compile("^(\\s*)([-*+]|\\d+\\.)\\s+\\[( |x|X)]\\s+(.*)$");
    private static final String TASK_INDEX_COMMENT = "<!-- task-index:%d -->";
    private static final String ALL_COMPLETED_PLACEHOLDER = "All tasks completed from that list";

    private MarkdownPreviewLayoutBuilder() {
    }

    static String build(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return markdown;
        }

        String[] lines = markdown.split("\\r?\\n", -1);
        List<String> output = new ArrayList<>();
        List<List<String>> completedGroups = new ArrayList<>();
        int[] taskIndex = {0};

        int cursor = 0;
        while (cursor < lines.length) {
            if (!isTaskLine(lines[cursor])) {
                output.add(lines[cursor]);
                cursor++;
                continue;
            }

            List<String> blockLines = new ArrayList<>();
            while (cursor < lines.length && (isTaskLine(lines[cursor]) || lines[cursor].isBlank())) {
                blockLines.add(lines[cursor]);
                cursor++;
            }

            BlockProjection projection = projectBlock(blockLines, taskIndex);
            output.addAll(projection.activeLines());
            completedGroups.addAll(projection.completedGroups());
        }

        trimTrailingBlankLines(output);
        if (!completedGroups.isEmpty()) {
            if (!output.isEmpty()) {
                output.add("");
            }
            output.add("----");
            output.add("## Completed:");
            for (int i = 0; i < completedGroups.size(); i++) {
                output.addAll(completedGroups.get(i));
                if (i < completedGroups.size() - 1) {
                    output.add("");
                }
            }
        }

        return String.join("\n", output);
    }

    private static BlockProjection projectBlock(List<String> blockLines, int[] taskIndex) {
        List<List<String>> groups = splitIntoGroups(blockLines);
        List<String> activeLines = new ArrayList<>();
        List<List<String>> completedGroups = new ArrayList<>();

        for (int i = 0; i < groups.size(); i++) {
            List<TaskNode> roots = parseGroup(groups.get(i), taskIndex);
            if (roots.isEmpty()) {
                continue;
            }

            List<String> activeGroup = new ArrayList<>();
            for (TaskNode root : roots) {
                appendActive(root, activeGroup);
            }
            trimTrailingBlankLines(activeGroup);
            if (activeGroup.isEmpty()) {
                activeLines.add(ALL_COMPLETED_PLACEHOLDER);
            } else {
                activeLines.addAll(activeGroup);
            }

            List<String> completedGroup = new ArrayList<>();
            for (TaskNode root : roots) {
                appendCompleted(root, completedGroup);
            }
            trimTrailingBlankLines(completedGroup);
            if (!completedGroup.isEmpty()) {
                completedGroups.add(completedGroup);
            }

            if (i < groups.size() - 1) {
                activeLines.add("");
            }
        }

        return new BlockProjection(activeLines, completedGroups);
    }

    private static List<List<String>> splitIntoGroups(List<String> blockLines) {
        List<List<String>> groups = new ArrayList<>();
        List<String> current = new ArrayList<>();

        for (String line : blockLines) {
            if (line.isBlank()) {
                if (!current.isEmpty()) {
                    groups.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }
            current.add(line);
        }

        if (!current.isEmpty()) {
            groups.add(current);
        }
        return groups;
    }

    private static List<TaskNode> parseGroup(List<String> groupLines, int[] taskIndex) {
        List<TaskNode> roots = new ArrayList<>();
        Deque<TaskNode> stack = new ArrayDeque<>();

        for (String line : groupLines) {
            Matcher matcher = TASK_PATTERN.matcher(line);
            if (!matcher.matches()) {
                continue;
            }

            String indent = matcher.group(1);
            int indentWidth = indent.replace("\t", "    ").length();
            TaskNode node = new TaskNode(
                    indent,
                    indentWidth,
                    matcher.group(2),
                    !" ".equals(matcher.group(3)),
                    matcher.group(4),
                    taskIndex[0]++
            );

            while (!stack.isEmpty() && indentWidth <= stack.peek().indentWidth) {
                stack.pop();
            }

            if (stack.isEmpty()) {
                roots.add(node);
            } else {
                stack.peek().children.add(node);
            }
            stack.push(node);
        }

        return roots;
    }

    private static void appendActive(TaskNode node, List<String> lines) {
        if (!hasActiveContent(node)) {
            return;
        }

        lines.add(node.toMarkdownLine());
        for (TaskNode child : node.children) {
            appendActive(child, lines);
        }
    }

    private static void appendCompleted(TaskNode node, List<String> lines) {
        if (!hasCompletedContent(node)) {
            return;
        }

        lines.add(node.toMarkdownLine());
        for (TaskNode child : node.children) {
            appendCompleted(child, lines);
        }
    }

    private static boolean hasActiveContent(TaskNode node) {
        if (!node.checked) {
            return true;
        }
        for (TaskNode child : node.children) {
            if (hasActiveContent(child)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCompletedContent(TaskNode node) {
        if (node.checked) {
            return true;
        }
        for (TaskNode child : node.children) {
            if (hasCompletedContent(child)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTaskLine(String line) {
        return TASK_PATTERN.matcher(line).matches();
    }

    private static void trimTrailingBlankLines(List<String> lines) {
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isBlank()) {
            lines.remove(lines.size() - 1);
        }
    }

    private record BlockProjection(List<String> activeLines, List<List<String>> completedGroups) {
    }

    private static final class TaskNode {
        private final String indent;
        private final int indentWidth;
        private final String marker;
        private final boolean checked;
        private final String text;
        private final int originalTaskIndex;
        private final List<TaskNode> children = new ArrayList<>();

        private TaskNode(String indent, int indentWidth, String marker, boolean checked, String text, int originalTaskIndex) {
            this.indent = indent;
            this.indentWidth = indentWidth;
            this.marker = marker;
            this.checked = checked;
            this.text = text;
            this.originalTaskIndex = originalTaskIndex;
        }

        private String toMarkdownLine() {
            return indent + marker + " [" + (checked ? "x" : " ") + "] " + text + " " + TASK_INDEX_COMMENT.formatted(originalTaskIndex);
        }
    }
}
