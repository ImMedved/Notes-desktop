package com.notes.client.ui.notes;

import com.notes.client.ui.Theme;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;

import java.util.List;

public final class MarkdownPreviewRenderer {
    private static final Parser PARSER;
    private static final HtmlRenderer RENDERER;

    static {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.<Extension>of(
                TablesExtension.create(),
                TaskListExtension.create(),
                StrikethroughExtension.create(),
                AutolinkExtension.create()
        ));
        PARSER = Parser.builder(options).build();
        RENDERER = HtmlRenderer.builder(options).build();
    }

    private MarkdownPreviewRenderer() {
    }

    public static String render(String markdown) {
        String safeMarkdown = markdown == null || markdown.isBlank() ? "_Пустая заметка_" : markdown;
        String htmlBody = RENDERER.render(PARSER.parse(safeMarkdown));
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
                        padding: 0 4px 12px 0;
                      }
                      h1, h2, h3, h4, h5, h6 {
                        color: #%s;
                        margin: 18px 0 10px 0;
                      }
                      p, ul, ol, blockquote, pre, table {
                        margin: 0 0 14px 0;
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
                htmlBody
        );
    }

    private static String colorHex(java.awt.Color color) {
        return "%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue());
    }
}
