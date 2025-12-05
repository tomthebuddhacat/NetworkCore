package net.calyro.friends.database;

import net.calyro.database.User;

import java.util.*;

public class FriendsDB {

    public static List<UUID> getFriends(UUID playerUUID) {
        return getUUIDList(playerUUID, "friend.friends");
    }

    //public static List<UUID> getBlockedUsers(UUID playerUUID) {
//        return getUUIDList(playerUUID, "friend.blocked_users");
//    }

    public static List<UUID> getBestFriends(UUID playerUUID) {
        return getUUIDList(playerUUID, "friend.best_friends");
    }

    public static List<UUID> getAllFriends(UUID playerUUID) {
        Set<UUID> combined = new HashSet<>(getFriends(playerUUID));
        combined.addAll(getBestFriends(playerUUID));
        return new ArrayList<>(combined);
    }

    public static void addFriend(UUID playerUUID, UUID friendUUID) {
        addUUIDToList(playerUUID, "friend.friends", friendUUID);
    }

    //public static void blockUser(UUID playerUUID, UUID friendUUID) {
//        addUUIDToList(playerUUID, "friend.blocked_users", friendUUID);
//    }

    public static void addBestFriend(UUID playerUUID, UUID bestFriendUUID) {
        addUUIDToList(playerUUID, "friend.best_friends", bestFriendUUID);
    }

    public static void removeFriend(UUID playerUUID, UUID friendUUID) {
        removeUUIDFromList(playerUUID, "friend.friends", friendUUID);
    }

    //public static void unblockUser(UUID playerUUID, UUID friendUUID) {
//        removeUUIDFromList(playerUUID, "friend.blocked_users", friendUUID);
//    }

    public static void removeBestFriend(UUID playerUUID, UUID bestFriendUUID) {
        removeUUIDFromList(playerUUID, "friend.best_friends", bestFriendUUID);
    }

    public static void removeAllFriends(UUID playerUUID) {
        User user = User.getUser(playerUUID);
        user.setData("friend.friends", new ArrayList<String>());
        user.setData("friend.best_friends", new ArrayList<String>());
    }

    private static List<UUID> getUUIDList(UUID playerUUID, String path) {
        User user = User.getUser(playerUUID);
        List<String> rawList = user.getData(path);
        List<UUID> uuids = new ArrayList<>();
        if (rawList != null) {
            for (String s : rawList) {
                try {
                    uuids.add(UUID.fromString(s));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return uuids;
    }

    private static void addUUIDToList(UUID playerUUID, String path, UUID uuidToAdd) {
        User user = User.getUser(playerUUID);
        List<String> list = user.getData(path);
        if (list == null)
            list = new ArrayList<>();
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