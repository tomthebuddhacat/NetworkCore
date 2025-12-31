package com.zerosio;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Config {

    private static File configFile;
    private static Configuration config;

    static {
        try {
            configFile = new File(Core.getInstance().getDataFolder(), "config.yml");

            if (!Core.getInstance().getDataFolder().exists()) {
                Core.getInstance().getDataFolder().mkdirs();
            }

            if (!configFile.exists()) {
                try (InputStream in = Core.getInstance().getResourceAsStream("config.yml");
                        FileOutputStream out = new FileOutputStream(configFile)) {
                    if (in != null) {
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                    } else {
                        Configuration defaultConfig = new Configuration();
                        defaultConfig.set("maintenance", false);
                        defaultConfig.set("key", "");
                        defaultConfig.set("database.uri", "mongodb://localhost:27017");
                        defaultConfig.set("database.name", "network_test");
                        ConfigurationProvider.getProvider(YamlConfiguration.class).save(defaultConfig, configFile);
                    }
                }
            }

            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Object get(String path) {
        return config.get(path);
    }

    public static String getString(String path, String def) {
        Object value = config.get(path);
        return value != null ? value.toString() : def;
    }

    public static boolean getBoolean(String path, boolean def) {
        Object value = config.get(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return def;
    }

    public static void set(String path, Object value) {
        config.set(path, value);
        save();
    }

    public static void reload() {
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}