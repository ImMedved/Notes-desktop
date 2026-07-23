package com.notes.client.storage;

import com.notes.shared.model.ServerSnapshot;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClientCacheStore {
    private final Path directory = Paths.get(System.getProperty("user.home"), ".notes-desktop-client");
    private final Path file = directory.resolve("cache.xml");

    public ServerSnapshot load() {
        try {
            Files.createDirectories(directory);
            if (!Files.exists(file)) {
                return new ServerSnapshot();
            }
            try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file));
                 XMLDecoder decoder = new XMLDecoder(inputStream)) {
                Object object = decoder.readObject();
                if (object instanceof ServerSnapshot snapshot) {
                    return snapshot;
                }
            }
        } catch (Exception ignored) {
            return new ServerSnapshot();
        }
        return new ServerSnapshot();
    }

    public void save(ServerSnapshot snapshot) {
        try {
            Files.createDirectories(directory);
            try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(file));
                 XMLEncoder encoder = new XMLEncoder(outputStream)) {
                encoder.writeObject(snapshot);
                encoder.flush();
            }
        } catch (Exception ignored) {}
    }
}
