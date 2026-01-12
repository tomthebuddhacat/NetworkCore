package com.zerosio.friends;

import com.zerosio.Core;
import com.zerosio.api.CoreAPI;
import com.zerosio.database.User;
import com.zerosio.friends.database.FriendsDB;
import com.zerosio.privacy.Ignores;
import com.zerosio.rank.Rank;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;

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

	public static void sendHelp(ProxiedPlayer player) {
		sendDivider(player);
		sendFriendMsg(player, "friend accept <player>", "Accept a friend request");
		sendFriendMsg(player, "friend add <player>", "Add a player as a friend");
		sendFriendMsg(player, "friend best <player>", "Toggle a player as best friend");
		sendFriendMsg(player, "friend deny <player>", "Decline a friend request");
		sendFriendMsg(player, "friend block <player>", "Block a player from sending requests");
		sendFriendMsg(player, "friend unblock <player>", "Unblock a player");
		sendFriendMsg(player, "friend help", "Prints all available friend commands");
		sendFriendMsg(player, "friend list <page>", "List your friends");
		sendFriendMsg(player, "friend notifications", "Toggle friend join/leave notifications");
		sendFriendMsg(player, "friend remove <player>", "Remove a player from your friends");
		sendFriendMsg(player, "friend removeall", "Remove all your friends");
		sendDivider(player);
	}

	public static void handleAdd(ProxiedPlayer from, ProxiedPlayer to) {
		UUID senderId = from.getUniqueId();
		UUID receiverId = to.getUniqueId();
		long now = System.currentTimeMillis();

		if (senderId.equals(receiverId) && !from.getName().equalsIgnoreCase("Zerosio")) {
			from.sendMessage("§cYou cannot add yourself as a friend!");
			return;
		}

		if (cooldowns.containsKey(senderId)) {
			long lastSent = cooldowns.get(senderId);
			if (now - lastSent < REQUEST_COOLDOWN) {
				from.sendMessage("§cPlease wait before sending another friend request.");
				return;
			}
		}

		if (!to.isConnected() || to == null) {
			from.sendMessage("§cPlayer not found!");
			return;
		}

		if (FriendsDB.getFriends(from.getUniqueId()).contains(to.getUniqueId())) {
			sendDivider(from);
			from.sendMessage("§cYou are already friends with this player!");
			sendDivider(from);
			return;
		}

		if (Ignores.getIgnoredUsers(to.getUniqueId()).contains(from.getUniqueId())) {
			sendDivider(from);
			from.sendMessage("§cYou are blocked from sending requests to this player!");
			sendDivider(from);
			return;
		}

		if (pendingRequests.containsKey(receiverId) &&
				pendingRequests.get(receiverId).containsKey(senderId)) {
			long sentTime = pendingRequests.get(receiverId).get(senderId);
			if (now - sentTime < REQUEST_EXPIRY) {
				from.sendMessage("§cYou've already sent a friend request to this player.");
				return;
			}
		}

		pendingRequests.computeIfAbsent(receiverId, k -> new HashMap<>()).put(senderId, now);
		cooldowns.put(senderId, now);

		sendDivider(from);
		from.sendMessage("§eYou sent a friend request to " + CoreAPI.getPlayerRank(receiverId).getPrefix()
						 + to.getName() + "§e! They have 5 minutes to accept it!");
		sendDivider(from);

		sendDivider(to);
		to.sendMessage("§eFriend request from " + CoreAPI.getPlayerRank(senderId).getPrefix() + from.getName() + "§e!");

		BaseComponent accept = new TextComponent("§a§l[ACCEPT]");
		accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friend accept " + from.getName()));
		accept.setHoverEvent(
			new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§bClick to accept the friend request")));

		BaseComponent deny = new TextComponent("§c§l[DENY]");
		deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friend deny " + from.getName()));
		deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§bClick to deny the friend request")));

		BaseComponent block = new TextComponent("§7§l[BLOCK]");
		block.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friend block " + from.getName()));
		block.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
										   new Text("§bClick to block all future friend requests and chat messages from this player")));

		BaseComponent[] full = new ComponentBuilder("")
		.append(accept).append(" §8- ")
		.append(deny).append(" §8- ")
		.append(block)
		.create();

		to.sendMessage(full);
		sendDivider(to);

		ProxyServer.getInstance().getScheduler().schedule(Core.getInstance(), new Runnable() {
			@Override
			public void run() {
				if (hasPendingRequest(senderId, receiverId)) {
					clearRequest(senderId, receiverId);

					ProxiedPlayer senderOnline = ProxyServer.getInstance().getPlayer(senderId);
					ProxiedPlayer receiverOnline = ProxyServer.getInstance().getPlayer(receiverId);

					if (senderOnline != null && senderOnline.isConnected()) {
						sendDivider(senderOnline);
						senderOnline.sendMessage("§cYour friend request to "
												 + CoreAPI.getPlayerRank(receiverId).getPrefix() + to.getName() + " has expired.");
						sendDivider(senderOnline);
					}

					if (receiverOnline != null && receiverOnline.isConnected()) {
						sendDivider(receiverOnline);
						receiverOnline.sendMessage("§cThe friend request from "
												   + CoreAPI.getPlayerRank(senderId).getPrefix() + from.getName() + " has expired.");
						sendDivider(receiverOnline);
					}
				}
			}
		}, REQUEST_EXPIRY, java.util.concurrent.TimeUnit.MILLISECONDS);
	}

	public static void handleBestFriend(ProxiedPlayer player, ProxiedPlayer target) {
		UUID playerUUID = player.getUniqueId();
		UUID targetUUID = target.getUniqueId();

		if (!FriendsDB.getFriends(playerUUID).contains(targetUUID)) {
			sendDivider(player);
			player.sendMessage("§cYou need to be friends with this player first!");
			sendDivider(player);
			return;
		}

		List<UUID> bestFriends = FriendsDB.getBestFriends(playerUUID);
		boolean isBestFriend = bestFriends.contains(targetUUID);

		if (isBestFriend) {
			FriendsDB.removeBestFriend(playerUUID, targetUUID);
			sendDivider(player);
			player.sendMessage("§aRemoved " + CoreAPI.getPlayerRank(targetUUID).getPrefix() + target.getName()
							   + " §afrom best friends!");
			sendDivider(player);

			if (target.isConnected()) {
				sendDivider(target);
				target.sendMessage(CoreAPI.getPlayerRank(playerUUID).getPrefix() + player.getName()
								   + " §cremoved you from their best friends!");
				sendDivider(target);
			}
		} else {
			FriendsDB.addBestFriend(playerUUID, targetUUID);
			sendDivider(player);
			player.sendMessage("§aAdded " + CoreAPI.getPlayerRank(targetUUID).getPrefix() + target.getName()
							   + " §ato best friends!");
			sendDivider(player);

			if (target.isConnected()) {
				sendDivider(target);
				target.sendMessage(CoreAPI.getPlayerRank(playerUUID).getPrefix() + player.getName()
								   + " §aadded you to their best friends!");
				sendDivider(target);
			}
		}
	}

	public static void handleAccept(ProxiedPlayer receiver, ProxiedPlayer sender) {
		UUID senderId = sender.getUniqueId();
		UUID receiverId = receiver.getUniqueId();

		if (!hasPendingRequest(senderId, receiverId)) {
			receiver.sendMessage("§cYou don't have a pending friend request from this player.");
			return;
		}

		clearRequest(senderId, receiverId);
		FriendsDB.addFriend(senderId, receiverId);
		FriendsDB.addFriend(receiverId, senderId);

		sendDivider(receiver);
		receiver.sendMessage(
			"§aYou are now friends with " + CoreAPI.getPlayerRank(senderId).getPrefix() + sender.getName() + "§a!");
		sendDivider(receiver);

		sendDivider(sender);
		sender.sendMessage("§aYou are now friends with " + CoreAPI.getPlayerRank(receiverId).getPrefix()
						   + receiver.getName() + "§a!");
		sendDivider(sender);
	}

	public static void handleDeny(ProxiedPlayer receiver, ProxiedPlayer sender) {
		UUID senderId = sender.getUniqueId();
		UUID receiverId = receiver.getUniqueId();

		if (!hasPendingRequest(senderId, receiverId)) {
			receiver.sendMessage("§cYou don't have a pending friend request from this player.");
			return;
		}

		clearRequest(senderId, receiverId);

		sendDivider(receiver);
		receiver.sendMessage("§eYou denied the friend request from " + CoreAPI.getPlayerRank(senderId).getPrefix()
							 + sender.getName() + "§e.");
		sendDivider(receiver);

		sendDivider(sender);
		sender.sendMessage("§cYour friend request was denied by " + CoreAPI.getPlayerRank(receiverId).getPrefix()
						   + receiver.getName() + "§c.");
		sendDivider(sender);
	}

	public static void handleBlock(ProxiedPlayer receiver, ProxiedPlayer sender) {
		UUID senderId = sender.getUniqueId();
		UUID receiverId = receiver.getUniqueId();

		Ignores.ignoreUser(receiverId, senderId);
		clearRequest(senderId, receiverId);
		FriendsDB.removeFriend(receiverId, senderId);
		FriendsDB.removeFriend(senderId, receiverId);
		FriendsDB.removeBestFriend(receiverId, senderId);
		FriendsDB.removeBestFriend(senderId, receiverId);

		sendDivider(receiver);
		receiver.sendMessage("§7You blocked " + CoreAPI.getPlayerRank(senderId).getPrefix() + sender.getName()
							 + "§7. They can no longer send friend requests or messages.");
		sendDivider(receiver);

		sendDivider(sender);
		sender.sendMessage("§cYou have been blocked by this player.");
		sendDivider(sender);
	}

	public static void handleUnblock(ProxiedPlayer player, String targetName) {
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
			player.sendMessage("§cYou haven't blocked this player!");
			sendDivider(player);
			return;
		}

		Ignores.unignore(playerUUID, foundUUID);

		sendDivider(player);
		player.sendMessage("§aYou unblocked " + CoreAPI.getPlayerRank(foundUUID).getPrefix() + foundName + "§a!");
		sendDivider(player);
	}

	public static void handleRemove(ProxiedPlayer sender, String targetName) {
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
			sender.sendMessage("§cYou are not friends with this player!");
			sendDivider(sender);
			return;
		}

		FriendsDB.removeFriend(senderUUID, foundUUID);
		FriendsDB.removeFriend(foundUUID, senderUUID);
		FriendsDB.removeBestFriend(senderUUID, foundUUID);
		FriendsDB.removeBestFriend(foundUUID, senderUUID);

		sendDivider(sender);
		sender.sendMessage("§aYou removed " + CoreAPI.getPlayerRank(foundUUID).getPrefix() + foundName
						   + " §afrom your friends list!");
		sendDivider(sender);

		ProxiedPlayer targetOnline = ProxyServer.getInstance().getPlayer(foundUUID);
		if (targetOnline != null && targetOnline.isConnected()) {
			sendDivider(targetOnline);
			targetOnline.sendMessage(CoreAPI.getPlayerRank(senderUUID).getPrefix() + sender.getName()
									 + " §eremoved you from their friends list!");
			sendDivider(targetOnline);
		}
	}

	public static void handleRemoveAll(ProxiedPlayer player) {
		UUID playerUUID = player.getUniqueId();
		List<UUID> allFriends = FriendsDB.getAllFriends(playerUUID);

		if (allFriends.isEmpty()) {
			sendDivider(player);
			player.sendMessage("§cYou don't have any friends to remove!");
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
		player.sendMessage("§aYou removed all friends from your friends list!");
		sendDivider(player);
	}

	public static String serverName;

	public static void handleList(ProxiedPlayer player, int page) {
		UUID playerUUID = player.getUniqueId();
		List<UUID> allFriends = FriendsDB.getAllFriends(playerUUID);
		List<UUID> bestFriends = FriendsDB.getBestFriends(playerUUID);

		if (allFriends.isEmpty()) {
			sendDivider(player);
			player.sendMessage("§cYou don't have any friends yet!");
			sendDivider(player);
			return;
		}

		// Sort with priority bruv
		List<UUID> sortedFriends = new ArrayList<>(allFriends);
		sortedFriends.sort((a, b) -> {
			boolean aOnline = ProxyServer.getInstance().getPlayer(a) != null;
			boolean bOnline = ProxyServer.getInstance().getPlayer(b) != null;

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
		player.sendMessage("        §6Friends (Page " + page + " of " + totalPages + ")");
		for (int i = startIndex; i < endIndex; i++) {
			UUID friendUUID = sortedFriends.get(i);
			String friendName = User.retrieveLastKnownName(friendUUID);
			Rank friendRank = CoreAPI.getPlayerRank(friendUUID);
			boolean isOnline = ProxyServer.getInstance().getPlayer(friendUUID) != null;
			boolean isBestFriend = bestFriends.contains(friendUUID);

			String bestFriendIndicator = isBestFriend ? "§6★ " : "";

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

				player.sendMessage(
					"§7- " + bestFriendIndicator + friendRank.getColour() + friendName + " §e is in " + serverName);
			} else {
				player.sendMessage(
					"§7- " + bestFriendIndicator + friendRank.getColour() + friendName +
					" §cis currently offline"
				);
			}
		}

		if (totalPages > 1) {
			player.sendMessage("");
			player.sendMessage("§eUse §6/friend list <page> §eto view more friends");
		}

		sendDivider(player);
	}


	public static void handleToggleNotifications(ProxiedPlayer player) {
		UUID playerUUID = player.getUniqueId();
		User user = User.getUser(playerUUID);
		boolean currentSetting = user.getBoolean("friend.join_leave_msg");

		user.setData("friend.join_leave_msg", !currentSetting);

		sendDivider(player);
		player.sendMessage("§aFriend notifications are now " + (!currentSetting ? "§aenabled" : "§cdisabled"));
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

	public static String getDivider() {
		return "§9§m-----------------------------------------------------";
	}

	public static void sendDivider(ProxiedPlayer player) {
		player.sendMessage(getDivider());
	}

	public static void sendFriendMsg(ProxiedPlayer player, String command, String description) {
		TextComponent cmdComponent = new TextComponent("§e/" + command);
		cmdComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + command));
		cmdComponent.setHoverEvent(
			new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7Click to put the command in chat")));

		BaseComponent[] finalMessage = new ComponentBuilder("§e")
		.append(cmdComponent)
		.append(" §8— §b" + description)
		.create();

		player.sendMessage(finalMessage);
	}

	public static String name(String serverName, String beforeName, String afterName) {
		if (serverName.contains(beforeName)) return afterName;
		return serverName;
	}
}