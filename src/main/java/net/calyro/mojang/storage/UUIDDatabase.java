package net.calyro.mojang.storage;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UUIDDatabase {
    private final ConcurrentHashMap<String, UUID> cache = new ConcurrentHashMap<>();

    public void storePremiumUUID(String username, UUID uuid) {
        cache.put(username.toLowerCase(), uuid);
    }

    public UUID getPremiumUUID(String username) {
        return cache.get(username.toLowerCase());
    }
}
