package com.notes.client.storage;

import com.notes.client.config.ClientConfig;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClientConfigStore {
    private final Path directory = Paths.get(System.getProperty("user.home"), ".notes-desktop-client");
    private final Path file = directory.resolve("config.xml");

    public ClientConfig load() {
        try {
            Files.createDirectories(directory);
            if (!Files.exists(file)) {
                return new ClientConfig();
            }
            try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file));
                 XMLDecoder decoder = new XMLDecoder(inputStream)) {
                Object object = decoder.readObject();
                if (object instanceof ClientConfig config) {
                    return config;
                }
            }
        } catch (Exception ignored) {
            return new ClientConfig();
        }
        return new ClientConfig();
    }

    public void save(ClientConfig config) {
        try {
            Files.createDirectories(directory);
            try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(file));
                 XMLEncoder encoder = new XMLEncoder(outputStream)) {
                encoder.writeObject(config);
                encoder.flush();
            }
        } catch (Exception ignored) {
            // Local config errors should not crash the widget.
        }
    }
}
