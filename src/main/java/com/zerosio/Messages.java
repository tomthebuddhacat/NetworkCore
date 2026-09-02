package com.zerosio;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Map;

public class Messages {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static Component get(String path) {
        return miniMessage.deserialize(Config.getString("messages." + path, ""));
    }

    public static Component get(String path, Map<String, String> messagePlaceholders) {
        String message = Config.getString("messages." + path, "");

        for (Map.Entry<String, String> mapEntry : messagePlaceholders.entrySet()) {
            message = message.replace("%" + mapEntry.getKey() + "%", mapEntry.getValue());
        }

        return miniMessage.deserialize(message);
    }

    public static String getString(String path) {
        return Config.getString("messages." + path, "");
    }
}
