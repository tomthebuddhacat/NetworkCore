package com.zerosio;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {

    private static Path filePath;

    private static ConfigurationLoader<CommentedConfigurationNode> configurationLoader;

    private static CommentedConfigurationNode commentedConfigurationNode;

    private Config() {}

    public static void init() {
        try {
            Path dataDirectory = Core.getInstance().getDataDirectory();

            Files.createDirectories(dataDirectory);

            filePath = dataDirectory.resolve("config.conf");

            configurationLoader = HoconConfigurationLoader.builder().path(filePath).build();

            if (Files.notExists(filePath)) {
                createDefaultConfig();
            }

            commentedConfigurationNode = configurationLoader.load();
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to initialize configuration", ioException);
        }
    }

    private static void createDefaultConfig() throws IOException {
        CommentedConfigurationNode configurationNode = configurationLoader.createNode();

        configurationNode.node("maintenance").set(false);
        configurationNode.node("key").set("");

        configurationNode.node("database", "uri").set("mongodb://localhost:27017");
        configurationNode.node("database", "name").set("Testing Database");

        configurationLoader.save(configurationNode);
    }

    public static Object get(String path) {
        return node(path).raw();
    }

    public static String getString(String path, String def) {
        return node(path).getString(def);
    }

    public static boolean getBoolean(String path, boolean def) {
        return node(path).getBoolean(def);
    }

    public static void set(String path, Object value) {
        try {
            node(path).set(value);
            save();
        } catch (ConfigurateException configurateException) {
            throw new RuntimeException("Failed to set config value: " + path, configurateException);
        }
    }

    public static void reload() {
        try {
            commentedConfigurationNode = configurationLoader.load();
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to reload configuration", ioException);
        }
    }

    public static void save() {
        try {
            configurationLoader.save(commentedConfigurationNode);
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to save configuration", ioException);
        }
    }

    public static CommentedConfigurationNode node(String path) {
        String[] part = path.split("\\.");

        CommentedConfigurationNode node = commentedConfigurationNode;

        for (String parts : part) {
            node = node.node(parts);
        }

        return node;
    }
}