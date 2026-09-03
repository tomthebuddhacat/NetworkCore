package com.zerosio.friends;

import com.velocitypowered.api.proxy.Player;
import com.zerosio.Core;
import com.zerosio.Messages;
import com.zerosio.api.CoreAPI;
import com.zerosio.database.User;
import com.zerosio.friends.database.FriendsDB;
import com.zerosio.privacy.Ignores;
import com.zerosio.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Friend {

	private static final Map<UUID, Map<UUID, Long>> pendingRequests = new HashMap<>();
	private static final Map<UUID, Long> cooldowns = new HashMap<>();

	private static final long REQUEST_EXPIRY = 5 * 60 * 1000L;
	private static final long REQUEST_COOLDOWN = 3 * 1000L;

	public static void sendHelp(Player player) {
		sendDivider(player);
		sendFriendMsg(player, "friend accept <player>", "friend-command-accept-description");
		sendFriendMsg(player, "friend add <player>", "friend-command-add-description");
		sendFriendMsg(player, "friend best <player>", "friend-command-best-description");
		sendFriendMsg(player, "friend deny <player>", "friend-command-deny-description");
		sendFriendMsg(player, "friend block <player>", "friend-command-block-description");
		sendFriendMsg(player, "friend unblock <player>", "friend-command-unblock-description");
		sendFriendMsg(player, "friend help", "friend-command-help-description");
		sendFriendMsg(player, "friend list <page>", "friend-command-list-description");
		sendFriendMsg(player, "friend notifications", "friend-command-notifications-description");
		sendFriendMsg(player, "friend remove <player>", "friend-command-remove-description");
		sendFriendMsg(player, "friend removeall", "friend-command-removeall-description");
		sendDivider(player);
	}

	public static void handleAdd(Player from, Player to) {
		UUID senderId = from.getUniqueId();
		UUID receiverId = to.getUniqueId();
		long now = System.currentTimeMillis();

		if (senderId.equals(receiverId)) {
			from.sendMessage(Messages.get("friend-command-you-cannot-add-yourself"));
			return;
		}

		if (cooldowns.containsKey(senderId)) {
			long lastSent = cooldowns.get(senderId);
			if (now - lastSent < REQUEST_COOLDOWN) {
				from.sendMessage(Messages.get("friend-command-on-cooldown"));
				return;
			}
		}

		if (to == null) {
			from.sendMessage(Messages.get("friend-command-player-not-online"));
			return;
		}

		if (FriendsDB.getFriends(from.getUniqueId()).contains(to.getUniqueId())) {
			sendDivider(from);
			from.sendMessage(Messages.get("friend-command-already-friends-with-this-player"));
			sendDivider(from);
			return;
		}

		if (Ignores.getIgnoredUsers(to.getUniqueId()).contains(from.getUniqueId())) {
			sendDivider(from);
			from.sendMessage(Messages.get("friend-command-you-are-blocked"));
			sendDivider(from);
			return;
		}

		if (pendingRequests.containsKey(receiverId) &&
				pendingRequests.get(receiverId).containsKey(senderId)) {
			long sentTime = pendingRequests.get(receiverId).get(senderId);
			if (now - sentTime < REQUEST_EXPIRY) {
				from.sendMessage(Messages.get("friend-command-request-already-sent"));
				return;
			}
		}

		pendingRequests.computeIfAbsent(receiverId, k -> new HashMap<>()).put(senderId, now);
		cooldowns.put(senderId, now);

		sendDivider(from);
		Map<String, String> messagePlaceholders = new HashMap<>();
		messagePlaceholders.put("playerRank", CoreAPI.getPlayerRank(receiverId).getPrefix());
		messagePlaceholders.put("playerName", to.getUsername());
		messagePlaceholders.put("playerRank", CoreAPI.getPlayerRank(senderId).getPrefix());
		messagePlaceholders.put("playerName", from.getUsername());

		from.sendMessage(Messages.get("friend-command-request-sent", messagePlaceholders));
		sendDivider(from);

		sendDivider(to);
		to.sendMessage(Messages.get("friend-command-request-recieved", messagePlaceholders));

		Component acceptComponent = Messages.get("friend-command-accept")
				.clickEvent(ClickEvent.runCommand("/friend accept " + from.getUsername()))
				.hoverEvent(HoverEvent.showText(Messages.get("friend-command-accept-hover")));

		Component denyComponent = Messages.get("friend-command-deny")
				.clickEvent(ClickEvent.runCommand("/friend deny " + from.getUsername()))
				.hoverEvent(HoverEvent.showText(Messages.get("friend-command-deny-hover")));

		Component blockComponent = Messages.get("friend-command-block")
				.clickEvent(ClickEvent.runCommand("/friend block " + from.getUsername()))
				.hoverEvent(HoverEvent.showText(Messages.get("friend-command-block-hover")));

		Component component = Component.empty()
				.append(acceptComponent)
				.append(Messages.get("friend-command-separator"))
				.append(denyComponent)
				.append(Messages.get("friend-command-separator"))
				.append(blockComponent);

		to.sendMessage(component);
		sendDivider(to);


		Core.getInstance().getProxy().getScheduler()
				.buildTask(Core.getInstance(), () -> {
					if (hasPendingRequest(senderId, receiverId)) {
						clearRequest(senderId, receiverId);

						Player senderOnline = Core.getInstance().getProxy().getPlayer(senderId).orElse(null);
						Player receiverOnline = Core.getInstance().getProxy().getPlayer(receiverId).orElse(null);

						if (senderOnline != null) {
							Map<String, String> senderMessagePlaceholders = new HashMap<>();
							senderMessagePlaceholders.put("targetRank", CoreAPI.getPlayerRank(receiverId).getPrefix());
							senderMessagePlaceholders.put("targetName", to.getUsername());

							sendDivider(senderOnline);
							senderOnline.sendMessage(Messages.get("friend-request-expired-sender", senderMessagePlaceholders));
							sendDivider(senderOnline);
						}

						if (receiverOnline != null) {
							Map<String, String> receiverMessagePlaceholders = new HashMap<>();
							receiverMessagePlaceholders.put("senderRank", CoreAPI.getPlayerRank(senderId).getPrefix());
							receiverMessagePlaceholders.put("senderName", from.getUsername());

							sendDivider(receiverOnline);
							receiverOnline.sendMessage(Messages.get("friend-request-expired-receiver", receiverMessagePlaceholders));
							sendDivider(receiverOnline);
						}
					}
				})
				.delay(Duration.ofMillis(REQUEST_EXPIRY))
				.schedule();
	}

	public static void handleBestFriend(Player player, Player target) {
		UUID playerUUID = player.getUniqueId();
		UUID targetUUID = target.getUniqueId();

		if (!FriendsDB.getFriends(playerUUID).contains(targetUUID)) {
			sendDivider(player);
			player.sendMessage(Messages.get("friend-command-you-must-be-friends"));
			sendDivider(player);
			return;
		}

		List<UUID> bestFriends = FriendsDB.getBestFriends(playerUUID);
		boolean isBestFriend = bestFriends.contains(targetUUID);
		Map<String, String> messagePlaceholders = new HashMap<>();
		messagePlaceholders.put("targetRank", CoreAPI.getPlayerRank(targetUUID).getPrefix());
		messagePlaceholders.put("targetName", target.getUsername());
		messagePlaceholders.put("playerRank", CoreAPI.getPlayerRank(playerUUID).getPrefix());
		messagePlaceholders.put("playerName", player.getUsername());

		if (isBestFriend) {
			FriendsDB.removeBestFriend(playerUUID, targetUUID);
			sendDivider(player);

			player.sendMessage(Messages.get("best-friend-removed", messagePlaceholders));
			sendDivider(player);

			if (target != null) {
				sendDivider(target);
				target.sendMessage(Messages.get("best-friend-removed-notification", messagePlaceholders));
				sendDivider(target);
			}
		} else {
			FriendsDB.addBestFriend(playerUUID, targetUUID);
			sendDivider(player);
			player.sendMessage(Messages.get("best-friend-added", messagePlaceholders));
			sendDivider(player);

			if (target != null) {
				sendDivider(target);
				target.sendMessage(Messages.get("best-friend-added-notification", messagePlaceholders));
				sendDivider(target);
			}
		}
	}

	public static void handleAccept(Player receiver, Player sender) {
		UUID senderId = sender.getUniqueId();
		UUID receiverId = receiver.getUniqueId();

		if (!hasPendingRequest(senderId, receiverId)) {
			receiver.sendMessage(Messages.get("friend-command-no-pending-friend-request"));
			return;
		}

		clearRequest(senderId, receiverId);
		FriendsDB.addFriend(senderId, receiverId);
		FriendsDB.addFriend(receiverId, senderId);
		Map<String, String> messagePlaceholders = new HashMap<>();
		messagePlaceholders.put("senderPlayerRank", CoreAPI.getPlayerRank(senderId).getPrefix());
		messagePlaceholders.put("senderPlayerName", sender.getUsername());
		messagePlaceholders.put("receiverPlayerRank", CoreAPI.getPlayerRank(receiverId).getPrefix());
		messagePlaceholders.put("receiverPlayerName", receiver.getUsername());

		sendDivider(receiver);
		receiver.sendMessage(Messages.get("friend-command-friend-added-successfully", messagePlaceholders));
		sendDivider(receiver);

		sendDivider(sender);
		sender.sendMessage(Messages.get("friend-command-player-accepted-friend-request", messagePlaceholders));
		sendDivider(sender);
	}

	public static void handleDeny(Player receiver, Player sender) {
		UUID senderId = sender.getUniqueId();
		UUID receiverId = receiver.getUniqueId();

		if (!hasPendingRequest(senderId, receiverId)) {
			receiver.sendMessage(Messages.get("friend-command-no-pending-friend-request"));
			return;
		}

		clearRequest(senderId, receiverId);
		Map<String, String> messagePlaceholders = new HashMap<>();
		messagePlaceholders.put("senderPlayerRank", CoreAPI.getPlayerRank(senderId).getPrefix());
		messagePlaceholders.put("senderPlayerName", sender.getUsername());
		messagePlaceholders.put("receiverPlayerRank", CoreAPI.getPlayerRank(receiverId).getPrefix());
		messagePlaceholders.put("receiverPlayerName", receiver.getUsername());

		sendDivider(receiver);
		receiver.sendMessage(Messages.get("friend-command-friend-request-denied-successfully", messagePlaceholders));
		sendDivider(receiver);

		sendDivider(sender);
		sender.sendMessage(Messages.get("friend-command-player-denied-friend-request", messagePlaceholders));
		sendDivider(sender);
	}

	public static void handleBlock(Player receiver, Player sender) {
		UUID senderId = sender.getUniqueId();
		UUID receiverId = receiver.getUniqueId();

		Ignores.ignoreUser(receiverId, senderId);
		clearRequest(senderId, receiverId);
		FriendsDB.removeFriend(receiverId, senderId);
		FriendsDB.removeFriend(senderId, receiverId);
		FriendsDB.removeBestFriend(receiverId, senderId);
		FriendsDB.removeBestFriend(senderId, receiverId);

		Map<String, String> messagePlaceholders = new HashMap<>();
		messagePlaceholders.put("senderPlayerRank", CoreAPI.getPlayerRank(senderId).getPrefix());
		messagePlaceholders.put("senderPlayerName", sender.getUsername());
		messagePlaceholders.put("receiverPlayerRank", CoreAPI.getPlayerRank(receiverId).getPrefix());
		messagePlaceholders.put("receiverPlayerName", receiver.getUsername());

		sendDivider(receiver);
		receiver.sendMessage(Messages.get("you-have-blocked-user-successfully", messagePlaceholders));
		sendDivider(receiver);

		sendDivider(sender);
		sender.sendMessage(Messages.get("you-are-currently-blocked-by-this-player"));
		sendDivider(sender);
	}

	public static void handleUnblock(Player player, String targetName) {
		UUID playerUUID = player.getUniqueId();
		List<UUID> blockedUsers = Ignores.getIgnoredUsers(playerUUID);

		UUID foundUUID = null;
		String foundName = null;

		for (UUID uuid : blockedUsers) {
			String name = User.retrieveLastKnownName(uuid);
			if (name != null && name.equalsIgnoreCase(targetName) && !name.equals("null")) {
				foundUUID = uuid;
				foundName = name;
				break;
			}
		}

		if (foundUUID == null) {
			sendDivider(player);
			player.sendMessage(Messages.get("you-dont-have-this-player-blocked"));
			sendDivider(player);
			return;
		}

		Ignores.unignore(playerUUID, foundUUID);

		Map<String, String> messagePlaceholders = new HashMap<>();
		messagePlaceholders.put("playerRank", CoreAPI.getPlayerRank(foundUUID).getPrefix());
		messagePlaceholders.put("playerName", foundName);

		sendDivider(player);
		player.sendMessage(Messages.get("you-have-successfully-unlocked-player", messagePlaceholders));
		sendDivider(player);
	}

	public static void handleRemove(Player sender, String targetName) {
		UUID senderUUID = sender.getUniqueId();
		List<UUID> allFriends = FriendsDB.getAllFriends(senderUUID);

		UUID foundUUID = null;
		String foundName = null;

		for (UUID uuid : allFriends) {
			String name = User.retrieveLastKnownName(uuid);
			if (name != null && name.equalsIgnoreCase(targetName) && !name.equals("null")) {
				foundUUID = uuid;
				foundName = name;
				break;
			}
		}

		if (foundUUID == null) {
			sendDivider(sender);
			sender.sendMessage(Messages.get("you-are-not-friends-with-this-player"));
			sendDivider(sender);
			return;
		}

		FriendsDB.removeFriend(senderUUID, foundUUID);
		FriendsDB.removeFriend(foundUUID, senderUUID);
		FriendsDB.removeBestFriend(senderUUID, foundUUID);
		FriendsDB.removeBestFriend(foundUUID, senderUUID);

		Map<String, String> messagePlaceholders = new HashMap<>();
		messagePlaceholders.put("foundPlayerRank", CoreAPI.getPlayerRank(foundUUID).getPrefix());
		messagePlaceholders.put("foundPlayerName", foundName);
		messagePlaceholders.put("senderPlayerRank", CoreAPI.getPlayerRank(senderUUID).getPrefix());
		messagePlaceholders.put("senderPlayerName", sender.getUsername());

		sendDivider(sender);
		sender.sendMessage(Messages.get("successfully-removed-player-from-friend-list", messagePlaceholders));
		sendDivider(sender);

		Player targetOnline = Core.getInstance().getProxy().getPlayer(foundUUID).orElse(null);
		if (targetOnline != null && targetOnline != null) {
			sendDivider(targetOnline);
			targetOnline.sendMessage(Messages.get("you-have-been-removed-from-friend-list", messagePlaceholders));
			sendDivider(targetOnline);
		}
	}

	public static void handleRemoveAll(Player player) {
		UUID playerUUID = player.getUniqueId();
		List<UUID> allFriends = FriendsDB.getAllFriends(playerUUID);

		if (allFriends.isEmpty()) {
			sendDivider(player);
			player.sendMessage(Messages.get("you-dont-have-any-friends-added"));
			sendDivider(player);
			return;
		}

		for (UUID friendUUID : allFriends) {
			FriendsDB.removeFriend(playerUUID, friendUUID);
			FriendsDB.removeFriend(friendUUID, playerUUID);
			FriendsDB.removeBestFriend(playerUUID, friendUUID);
			FriendsDB.removeBestFriend(friendUUID, playerUUID);
		}

		sendDivider(player);
		player.sendMessage(Messages.get("removed-everyone-from-friend-list"));
		sendDivider(player);
	}

	public static String serverName;

	public static void handleList(Player player, int page) {
		UUID playerUUID = player.getUniqueId();
		List<UUID> allFriends = FriendsDB.getAllFriends(playerUUID);
		List<UUID> bestFriends = FriendsDB.getBestFriends(playerUUID);

		if (allFriends.isEmpty()) {
			sendDivider(player);
			player.sendMessage(Messages.get("you-dont-have-any-friends-added"));
			sendDivider(player);
			return;
		}

		// Sort with priority bruv
		List<UUID> sortedFriends = new ArrayList<>(allFriends);
		sortedFriends.sort((a, b) -> {
			boolean aOnline = Core.getInstance().getProxy().getPlayer(a) != null;
			boolean bOnline = Core.getInstance().getProxy().getPlayer(b) != null;

			boolean aBest = bestFriends.contains(a);
			boolean bBest = bestFriends.contains(b);

			if (aOnline && !bOnline) return -1;
			if (!aOnline && bOnline) return 1;

			if (aBest && !bBest) return -1;
			if (!aBest && bBest) return 1;

			return 0;
		});

		int pageSize = 10;
		int totalPages = (int) Math.ceil((double) sortedFriends.size() / pageSize);

		if (page < 1) page = 1;
		if (page > totalPages) page = totalPages;

		int startIndex = (page - 1) * pageSize;
		int endIndex = Math.min(startIndex + pageSize, sortedFriends.size());

		sendDivider(player);
		Map<String, String> messagePlaceholders = new HashMap<>();
		messagePlaceholders.put("pageNumber", String.valueOf(page));
		messagePlaceholders.put("totalPages", String.valueOf(totalPages));

		player.sendMessage(Messages.get("friends-page-title", messagePlaceholders));
		for (int i = startIndex; i < endIndex; i++) {
			UUID friendUUID = sortedFriends.get(i);
			String friendName = User.retrieveLastKnownName(friendUUID);
			Rank friendRank = CoreAPI.getPlayerRank(friendUUID);
			boolean isOnline = Core.getInstance().getProxy().getPlayer(friendUUID) != null;
			boolean isBestFriend = bestFriends.contains(friendUUID);

			String bestFriendIndicator = isBestFriend ? "§6★ " : "";

			Map<String, String> serverThing = new HashMap<>();
			messagePlaceholders.put("bestFriendIndicator", bestFriendIndicator);
			messagePlaceholders.put("friendRankColour", friendRank.getColour());
			messagePlaceholders.put("friendName", friendName);
			messagePlaceholders.put("serverName", serverName);

			if (isOnline) {
				serverName = CoreAPI.getPlayerServerName(CoreAPI.getProxyPlayerUsingUUID(friendUUID));

				serverName = name(serverName, "lobby", "Main Lobby");
				serverName = name(serverName, "limbo", "Limbo");
				serverName = name(serverName, "sbh", "SkyBlock Hub");
				serverName = name(serverName, "sbi", "SkyBlock Island");
				serverName = name(serverName, "sbgd", "Goldmine");
				serverName = name(serverName, "sbdc", "Deep Caverens");
				serverName = name(serverName, "sbdm", "Dwarven Mines");
				serverName = name(serverName, "sbsd", "Spider's Den");
				serverName = name(serverName, "sbci", "Crimson Isle");
				serverName = name(serverName, "sbe", "End Island");
				serverName = name(serverName, "sbf", "Farming Island");
				serverName = name(serverName, "sbp", "Park");
				serverName = name(serverName, "sbw", "Jerry's Workshop");
				serverName = name(serverName, "sbch", "Crystal Hollows");
				serverName = name(serverName, "sbg", "Garden");
				serverName = name(serverName, "sbri", "Rift");
				serverName = name(serverName, "sbms", "Mineshaft");
				serverName = name(serverName, "sbbw", "Backwater Bayou");

				player.sendMessage(Messages.get("friend-server-online", serverThing));
			} else {
				player.sendMessage(Messages.get("friend-server-offline", serverThing));
			}
		}

		if (totalPages > 1) {
			player.sendMessage(Component.empty());
			player.sendMessage(Messages.get("friends-view-more-from-list"));
		}

		sendDivider(player);
	}


	public static void handleToggleNotifications(Player player) {
		UUID playerUUID = player.getUniqueId();
		User user = User.getUser(playerUUID);
		boolean currentSetting = user.getBoolean("friend.join_leave_msg");

		user.setData("friend.join_leave_msg", !currentSetting);
		Map<String, String> messagePlaceholders = new HashMap<>();
		messagePlaceholders.put("status", currentSetting ? "<red>disabled" : "<green>enabled");

		sendDivider(player);
		player.sendMessage(Messages.get("friend-notifications-toggle", messagePlaceholders));
    	sendDivider(player);
	}

	public static boolean hasPendingRequest(UUID from, UUID to) {
		if (!pendingRequests.containsKey(to))
			return false;
		Long sent = pendingRequests.get(to).get(from);
		return sent != null && System.currentTimeMillis() - sent < REQUEST_EXPIRY;
	}

	public static void clearRequest(UUID from, UUID to) {
		if (pendingRequests.containsKey(to)) {
			pendingRequests.get(to).remove(from);
			if (pendingRequests.get(to).isEmpty()) {
				pendingRequests.remove(to);
			}
		}
	}

	public static void sendDivider(Player player) {
		player.sendMessage(Messages.get("friend-command-message-divider"));
	}

	public static void sendFriendMsg(Player player, String command, String path) {
		Map<String, String> messagePlaceholders = new HashMap<>();
		messagePlaceholders.put("friendCommand", command);

		Component component = Messages.get("friend-command-label", messagePlaceholders)
				.clickEvent(ClickEvent.suggestCommand("/" + command))
				.hoverEvent(HoverEvent.showText(Messages.get("friend-command-label-hover")));

		Component anotherComponent = Messages.get("friend-command-prefix")
				.append(component)
				.append(Messages.get("friend-command-separator"))
				.append(Messages.get(path));

		player.sendMessage(anotherComponent);
	}

	public static String name(String serverName, String beforeName, String afterName) {
		if (serverName.contains(beforeName)) return afterName;
		return serverName;
	}
}