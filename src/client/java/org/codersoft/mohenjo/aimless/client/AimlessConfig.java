package org.codersoft.mohenjo.aimless.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AimlessConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("aimless");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("aimless.json");

    private int reactionTicks = 6;
    private List<String> exceptions = new ArrayList<>();

    public int getReactionTicks() {
        return reactionTicks;
    }

    public void setReactionTicks(int reactionTicks) {
        this.reactionTicks = reactionTicks;
        save();
    }

    public List<String> getExceptions() {
        return exceptions;
    }

    public boolean isExcepted(String playerName) {
        return exceptions.stream().anyMatch(e -> e.equalsIgnoreCase(playerName));
    }

    public void addException(String playerName) {
        String lower = playerName.toLowerCase();
        if (exceptions.stream().noneMatch(e -> e.equalsIgnoreCase(lower))) {
            exceptions.add(lower);
            save();
        }
    }

    public void removeException(String playerName) {
        if (exceptions.removeIf(e -> e.equalsIgnoreCase(playerName))) {
            save();
        }
    }

    public void clearExceptions() {
        if (!exceptions.isEmpty()) {
            exceptions.clear();
            save();
        }
    }

    public static AimlessConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                AimlessConfig config = GSON.fromJson(reader, AimlessConfig.class);
                if (config != null) {
                    LOGGER.info("Loaded config from {}", CONFIG_PATH);
                    return config;
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to load config from {}: {}", CONFIG_PATH, e.getMessage());
            }
        } else {
            LOGGER.info("Config not found at {}, using defaults", CONFIG_PATH);
        }
        return new AimlessConfig();
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
            LOGGER.info("Saved config to {}", CONFIG_PATH);
        } catch (IOException e) {
            LOGGER.error("Failed to save config to {}: {}", CONFIG_PATH, e.getMessage());
        }
    }
}
