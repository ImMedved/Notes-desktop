package com.notes.client;

import com.notes.client.service.ClientAppService;
import com.notes.client.storage.ClientCacheStore;
import com.notes.client.storage.ClientConfigStore;
import com.notes.client.ui.Theme;
import com.notes.client.ui.WidgetFrame;
import com.notes.client.util.StartupManager;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        ClientConfigStore configStore = new ClientConfigStore();
        ClientCacheStore cacheStore = new ClientCacheStore();
        ClientAppService appService = new ClientAppService(configStore, cacheStore);

        StartupManager.ensureAutostartForPackagedApp();

        Runtime.getRuntime().addShutdownHook(new Thread(appService::shutdown, "notes-client-shutdown"));

        SwingUtilities.invokeLater(() -> {
            Theme.install(Theme.systemMode());
            WidgetFrame frame = new WidgetFrame(appService);
            frame.setVisible(true);
            frame.syncNowAsync();
        });
    }
}
