package com.atenls.rapidsutils.client.config;

import com.atenls.rapidsutils.client.RapidsUtilsClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RapidsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("rapidsutils.json");

    public boolean enabled = true;
    public int margin = 8;
    public float backgroundOpacity = 0.75F;
    public int maxWidth = 300;

    public static RapidsConfig load() {
        if (!Files.exists(PATH)) {
            RapidsConfig config = new RapidsConfig();
            config.save();
            return config;
        }

        try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            RapidsConfig config = GSON.fromJson(reader, RapidsConfig.class);
            if (config == null) {
                config = new RapidsConfig();
            }
            config.sanitize();
            return config;
        } catch (IOException | JsonSyntaxException e) {
            RapidsUtilsClient.LOGGER.warn("Failed to load rapidsutils.json; using defaults", e);
            return new RapidsConfig();
        }
    }

    private void save() {
        sanitize();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            RapidsUtilsClient.LOGGER.warn("Failed to write default rapidsutils.json", e);
        }
    }

    private void sanitize() {
        margin = Math.max(0, Math.min(64, margin));
        backgroundOpacity = Math.max(0.15F, Math.min(0.95F, backgroundOpacity));
        maxWidth = Math.max(140, Math.min(600, maxWidth));
    }
}
