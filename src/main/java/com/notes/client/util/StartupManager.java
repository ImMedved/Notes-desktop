package com.notes.client.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class StartupManager {
    private StartupManager() {
    }

    public static void ensureAutostartForPackagedApp() {
        String executablePath = System.getProperty("jpackage.app-path");
        if (executablePath == null || executablePath.isBlank()) {
            return;
        }
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) {
            return;
        }
        Path startupScript = Paths.get(appData, "Microsoft", "Windows", "Start Menu", "Programs", "Startup", "NotesWidgetClient.cmd");
        String content = "@echo off\r\nstart \"\" \"" + executablePath + "\"\r\n";
        try {
            Files.writeString(startupScript, content, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Startup registration is optional.
        }
    }
}
