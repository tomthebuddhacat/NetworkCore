package net.calyro.privacy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.calyro.database.User;

public class Ignores {
	
	public static List<UUID> getIgnoredUsers(UUID playerUUID) {
        return getUUIDList(playerUUID, "ignores");
    }
	
	public static void ignoreUser(UUID playerUUID, UUID friendUUID) {
        addUUIDToList(playerUUID, "ignores", friendUUID);
    }
	
	public static void unignore(UUID playerUUID, UUID friendUUID) {
        removeUUIDFromList(playerUUID, "ignores", friendUUID);
    }
	
	// utility stuff
	private static List<UUID> getUUIDList(UUID playerUUID, String path) {
        User user = User.getUser(playerUUID);
        List<String> rawList = user.getData(path);
        List<UUID> uuids = new ArrayList<>();
        if (rawList != null) {
            for (String s : rawList) {
                try {
                    uuids.add(UUID.fromString(s));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return uuids;
    }

    private static void addUUIDToList(UUID playerUUID, String path, UUID uuidToAdd) {
        User user = User.getUser(playerUUID);
        List<String> list = user.getData(path);
        if (list == null) list = new ArrayList<>();
        if (!list.contains(uuidToAdd.toString())) {
            list.add(uuidToAdd.toString());
            user.setData(path, list);
        }
    }

    private static void removeUUIDFromList(UUID playerUUID, String path, UUID uuidToRemove) {
        User user = User.getUser(playerUUID);
        List<String> list = user.getData(path);
        if (list != null && list.remove(uuidToRemove.toString())) {
            user.setData(path, list);
        }
    }
}