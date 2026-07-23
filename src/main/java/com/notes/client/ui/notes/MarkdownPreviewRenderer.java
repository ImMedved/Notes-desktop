package com.notes.client.ui.notes;

import com.notes.client.ui.Theme;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownPreviewRenderer {
    private static final Pattern TASK_PATTERN = Pattern.compile("^(\\s*)([-*+]|\\d+\\.)\\s+\\[( |x|X)]\\s+(.*?)(?:\\s+<!--\\s*task-index:(\\d+)\\s*-->)?$");
    private static final Pattern RAW_TASK_PATTERN = Pattern.compile("^(\\s*)([-*+]|\\d+\\.)\\s+\\[( |x|X)]\\s+(.*)$");
    private static final Parser PARSER;
    private static final HtmlRenderer RENDERER;

    static {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.<Extension>of(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                AutolinkExtension.create()
        ));
        PARSER = Parser.builder(options).build();
        RENDERER = HtmlRenderer.builder(options).build();
    }

    private MarkdownPreviewRenderer() {
    }

    public static String render(String markdown) {
        String safeMarkdown = markdown == null || markdown.isBlank() ? "_Empty note_" : markdown;
        String displayMarkdown = MarkdownPreviewLayoutBuilder.build(safeMarkdown);
        String htmlBody = RENDERER.render(PARSER.parse(transformTaskLists(displayMarkdown)));
        return """
                <html>
                  <head>
                    <style>
                      body {
                        background: #%s;
                        color: #%s;
                        font-family: 'Segoe UI Variable', 'Segoe UI', sans-serif;
                        font-size: 14px;
                        line-height: 1.6;
                        margin: 0;
                        padding: 0 8px 12px 12px;
                      }
                      h1, h2, h3, h4, h5, h6 {
                        color: #%s;
                        margin: 18px 0 10px 0;
                      }
                      p, ul, ol, blockquote, pre, table {
                        margin: 0 0 14px 0;
                      }
                      ul, ol {
                        padding-left: 22px;
                      }
                      a {
                        color: #%s;
                        text-decoration: none;
                      }
                      code {
                        font-family: Consolas, monospace;
                        background: #%s;
                        color: #%s;
                        padding: 2px 5px;
                        border-radius: 6px;
                      }
                      pre {
                        background: #%s;
                        padding: 12px;
                        border-radius: 12px;
                        overflow-x: auto;
                      }
                      pre code {
                        background: transparent;
                        padding: 0;
                      }
                      blockquote {
                        border-left: 4px solid #%s;
                        margin-left: 0;
                        padding-left: 12px;
                        color: #%s;
                      }
                      table {
                        border-collapse: collapse;
                        width: 100%%;
                      }
                      th, td {
                        border: 1px solid #%s;
                        padding: 8px 10px;
                      }
                      th {
                        background: #%s;
                      }
                      hr {
                        border: none;
                        border-top: 1px solid #%s;
                        margin: 18px 0;
                      }
                      .task-item {
                        width: auto;
                        margin: 0 0 8px 0;
                      }
                      .task-item td {
                        border: none;
                        padding: 0;
                        vertical-align: top;
                      }
                      .task-box-cell {
                        width: 20px;
                        padding-top: 1px;
                      }
                      .task-box {
                        display: inline-block;
                        width: 18px;
                        color: #%s;
                        font-size: 15px;
                        line-height: 1.2;
                        background: transparent;
                      }
                      .task-text {
                        padding-left: 8px;
                      }
                      .task-text p {
                        margin: 0;
                      }
                    </style>
                  </head>
                  <body>%s</body>
                </html>
                """.formatted(
                colorHex(Theme.PANEL_ALT),
                colorHex(Theme.TEXT),
                colorHex(Theme.TEXT),
                colorHex(Theme.ACCENT),
                colorHex(Theme.PANEL),
                colorHex(Theme.TEXT),
                colorHex(Theme.PANEL),
                colorHex(Theme.ACCENT_SOFT),
                colorHex(Theme.MUTED),
                colorHex(Theme.CARD),
                colorHex(Theme.PANEL),
                colorHex(Theme.CARD),
                colorHex(Theme.ACCENT),
                htmlBody
        );
    }

    public static String toggleTask(String markdown, int taskIndex) {
        if (markdown == null || markdown.isBlank()) {
            return markdown;
        }

        String[] lines = markdown.split("\\r?\\n", -1);
        int currentTask = 0;
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = RAW_TASK_PATTERN.matcher(lines[i]);
            if (!matcher.matches()) {
                continue;
            }
            if (currentTask == taskIndex) {
                String toggled = " ".equals(matcher.group(3)) ? "x" : " ";
                lines[i] = matcher.group(1) + matcher.group(2) + " [" + toggled + "] " + matcher.group(4);
                return String.join("\n", lines);
            }
            currentTask++;
        }
        return markdown;
    }

    private static String transformTaskLists(String markdown) {
        String[] lines = markdown.split("\\r?\\n", -1);
        List<String> output = new ArrayList<>();

        for (String line : lines) {
            Matcher matcher = TASK_PATTERN.matcher(line);
            if (!matcher.matches()) {
                output.add(line);
                continue;
            }

            int indent = Math.max(0, matcher.group(1).replace("\t", "    ").length() / 2);
            boolean checked = !" ".equals(matcher.group(3));
            String checkboxSymbol = checked ? "&#9745;" : "&#9744;";
            String inlineHtml = renderInline(matcher.group(4));
            String taskLink = matcher.group(5) == null ? "#" : "task://%s".formatted(matcher.group(5));
            output.add("""
                    <div style="margin-left:%dpx">
                      <table class="task-item">
                        <tr>
                          <td class="task-box-cell"><a class="task-box" href="%s">%s</a></td>
                          <td class="task-text">%s</td>
                        </tr>
                      </table>
                    </div>
                    """.formatted(
                    indent * 8,
                    taskLink,
                    checkboxSymbol,
                    inlineHtml
            ));
        }
        return String.join("\n", output);
    }

    private static String renderInline(String markdown) {
        String html = RENDERER.render(PARSER.parse(markdown == null ? "" : markdown));
        if (html.startsWith("<p>") && html.endsWith("</p>\n")) {
            return html.substring(3, html.length() - 5);
        }
        if (html.startsWith("<p>") && html.endsWith("</p>")) {
            return html.substring(3, html.length() - 4);
        }
        return html;
    }

    private static String colorHex(java.awt.Color color) {
        return "%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue());
    }
}
