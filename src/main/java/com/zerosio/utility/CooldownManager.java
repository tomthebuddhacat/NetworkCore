package com.zerosio.utility;

import java.util.HashMap;
import java.util.UUID;

public class CooldownManager {
    private static final HashMap<UUID, Long> banCooldowns = new HashMap<>();
    private static final HashMap<UUID, Long> kickCooldowns = new HashMap<>();
    private static final HashMap<UUID, Long> muteCooldowns = new HashMap<>();

    private static final long COOLDOWN_TIME = 30 * 1000; // 30 seconds

    public static boolean isOnCooldown(UUID playerId, String commandType) {
        HashMap<UUID, Long> cooldownMap;

        switch (commandType.toLowerCase()) {
            case "ban":
                cooldownMap = banCooldowns;
                break;
            case "kick":
                cooldownMap = kickCooldowns;
                break;
            case "mute":
                cooldownMap = muteCooldowns;
                break;
            default:
                return false;
        }

        if (cooldownMap.containsKey(playerId)) {
            long cooldownEnd = cooldownMap.get(playerId);
            if (System.currentTimeMillis() < cooldownEnd) {
                return true;
            } else {
                cooldownMap.remove(playerId);
            }
        }
        return false;
    }

    public static long getRemainingCooldown(UUID playerId, String commandType) {
        HashMap<UUID, Long> cooldownMap;

        switch (commandType.toLowerCase()) {
            case "ban":
                cooldownMap = banCooldowns;
                break;
            case "kick":
                cooldownMap = kickCooldowns;
                break;
            case "mute":
                cooldownMap = muteCooldowns;
                break;
            default:
                return 0;
        }

        if (cooldownMap.containsKey(playerId)) {
            long remaining = cooldownMap.get(playerId) - System.currentTimeMillis();
            return remaining > 0 ? remaining : 0;
        }
        return 0;
    }

    public static void setCooldown(UUID playerId, String commandType) {
        HashMap<UUID, Long> cooldownMap;

        switch (commandType.toLowerCase()) {
            case "ban":
                cooldownMap = banCooldowns;
                break;
            case "kick":
                cooldownMap = kickCooldowns;
                break;
            case "mute":
                cooldownMap = muteCooldowns;
                break;
            default:
                return;
        }

        cooldownMap.put(playerId, System.currentTimeMillis() + COOLDOWN_TIME);
    }
}