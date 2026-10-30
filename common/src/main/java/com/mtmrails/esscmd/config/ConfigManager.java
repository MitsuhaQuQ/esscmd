package com.mtmrails.esscmd.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mtmrails.esscmd.message.Messages;
import com.mtmrails.esscmd.message.TextDocument;
import com.mtmrails.esscmd.message.TextFiles;
import com.mtmrails.esscmd.platform.Platform;
import net.minecraft.server.MinecraftServer;

/** Loads {@link EssConfig} and the messages file from {@code config/cmdessentials/}. */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static volatile EssConfig config = new EssConfig();

    private ConfigManager() {}

    public static EssConfig get() {
        return config;
    }

    public static Path dir() {
        return Platform.get().configDir().resolve("cmdessentials");
    }

    /** Blocking load, for mod initialisation before the server runs. */
    public static void load() throws IOException {
        apply(read());
    }

    /** Reads the files off the server thread, then swaps them in on the server thread. */
    public static CompletableFuture<Void> reload(MinecraftServer server) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return read();
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }).thenAcceptAsync(ConfigManager::apply, server);
    }

    /**
     * Writes the current config after a command changed it (e.g. /settpr). The JSON is built
     * on the calling (server) thread; only the file write happens elsewhere.
     */
    public static CompletableFuture<Void> save() {
        String json = GSON.toJson(config);
        Path file = dir().resolve("config.json");
        return CompletableFuture.runAsync(() -> {
            try {
                Files.writeString(file, json, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    private record Loaded(EssConfig config, Map<String, Map<String, String>> messages,
                          Map<String, Map<String, TextDocument>> texts) {}

    private static Loaded read() throws IOException {
        Path dir = dir();
        Files.createDirectories(dir);
        Path file = dir.resolve("config.json");
        EssConfig loaded = null;
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, EssConfig.class);
            } catch (RuntimeException e) {
                throw new IOException("config.json is not valid JSON: " + e.getMessage(), e);
            }
        }
        if (loaded == null) {
            loaded = new EssConfig();
        }
        // Rewrite so options added in newer versions appear with their defaults.
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(loaded, writer);
        }
        return new Loaded(loaded, Messages.read(dir), TextFiles.read(dir));
    }

    private static void apply(Loaded loaded) {
        config = loaded.config();
        Messages.set(loaded.messages());
        TextFiles.set(loaded.texts());
    }
}
