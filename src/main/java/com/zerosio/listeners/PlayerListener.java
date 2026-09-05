package com.zerosio.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.zerosio.Config;
import com.zerosio.Core;
import com.zerosio.Messages;
import com.zerosio.api.ControllerAPI;
import com.zerosio.api.CoreAPI;
import com.zerosio.authentication.AuthDB;
import com.zerosio.authentication.Authentication;
import com.zerosio.commands.punishments.PunishmentDomains;
import com.zerosio.database.Punishment;
import com.zerosio.database.User;
import com.zerosio.friends.database.FriendsDB;
import com.zerosio.guilds.Guild;
import com.zerosio.utility.PremiumUtil;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bson.Document;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PlayerListener {

	private final Map<UUID, Long> joinTimestamps = new ConcurrentHashMap<>();

	private final ProxyServer proxyServer;

	public PlayerListener(ProxyServer proxyServer) {
		this.proxyServer = proxyServer;
	}

	@Subscribe
	public void onServerConnect(ServerPreConnectEvent serverPreConnectEvent) {
		Player player = serverPreConnectEvent.getPlayer();
		UUID uuid = player.getUniqueId();
		User user = User.getUser(uuid);
		boolean isPremium = PremiumUtil.isPremium(player);
		long currentUnix = System.currentTimeMillis() / 1000L;
		boolean maintenance = (boolean) Config.get("maintenance");

		Guild guild = Guild.getGuildFromPlayer(player);

		if (maintenance && !CoreAPI.getPlayerRank(uuid).isStaff()) {
			player.disconnect(Messages.get("maintenance-disconnect-reason"));
			serverPreConnectEvent.setResult(ServerPreConnectEvent.ServerResult.denied());
			return;
		}


		// Check active ban
		if (Punishment.isBanned(uuid, player.getRemoteAddress().getAddress().getHostAddress())) {
			Document ban = Punishment.getActiveBan(uuid, player.getRemoteAddress().getAddress().getHostAddress());
			if (ban != null) {
				String reason = ban.getString("reason");
				String id = ban.getString("id");
				long length = ban.getLong("length");
				long timestamp = ban.getLong("timestamp");

				List<String> kickMessage;
				String remaining = "";
				if (length == -1) {
					kickMessage = Config.getStringList("server.punishment-messages.permanently-banned");
					/*
					kickMessage = "§cYou are permanently banned from this server!\n\n" +
								  "§7Reason: §f" + reason + "\n" +
								  "§7Find out more: §b§n" + PunishmentDomains.BAN + "\n\n" +
								  "§7Ban ID: §f#" + id + "\n" +
								  "§7Sharing your Ban ID may affect the processing of your appeal
					 */
				} else {
					long expiry = timestamp + (length * 1000L);
					remaining = calculateTime((expiry - System.currentTimeMillis()) / 1000L);
					kickMessage = Config.getStringList("server.punishment-messages.temporarily-banned");
				}

				String msg = String.join("\n", kickMessage).replace("%punishmentReason%", reason)
								.replace("%punishmentId%", id)
						        .replace("%punishmentUrl%", PunishmentDomains.BAN)
						        .replace("%remainingPunishmentTime%", remaining);

				player.disconnect(Component.text(msg));
				return;
			}
		}

		if (!AuthListener.processPostLogin(player)) {
			return;
		}

		Authentication.storeAuthenticatedIP(player);
		user.setData("last_known_name", player.getUsername());
		user.setData("last_login", System.currentTimeMillis());

		joinTimestamps.put(uuid, System.currentTimeMillis());

		// Notify friends
		for (UUID friendId : FriendsDB.getFriends(uuid)) {
			Player friend = CoreAPI.getProxyPlayerUsingUUID(friendId);
			User friendUser = User.getUser(friendId);
			if (friend != null && friend.isActive()) {
				if (friendUser.getBoolean("friend.join_leave_msg")) {
					friend.sendMessage(Messages.get("friend-join-message", Map.of("playerRank", CoreAPI.getPlayerRank(player.getUniqueId()).getPrefix(), "playerName", player.getUsername())));
				}
			}
			
			if (friend != null) {
				friendUser.incrementFCount();
			}
		}

		if (CoreAPI.getPlayerRank(player.getUniqueId()).isStaff()) {
			for (Player poo : Core.getInstance().getProxy().getAllPlayers()) {
				if (CoreAPI.getPlayerRank(poo.getUniqueId()).isStaff()) {
					poo.sendMessage(Messages.get("staff-alert-join-message", Map.of("playerRank", CoreAPI.getPlayerRank(player.getUniqueId()).getPrefix(), "playerName", player.getUsername())));
				}
			}
		}

		if (guild != null) {
			String playerName = player.getUsername();
			String guildTagColor = guild.getTagColor();
			String guildTag = guild.getTag().isEmpty() ? "" : "[" + guild.getTag() + "] ";
			Map<String, String> messagePlaceholders = new HashMap<>();
			messagePlaceholders.put("playerRank", CoreAPI.getPlayerRank(player.getUniqueId()).getPrefix());
			messagePlaceholders.put("playerName", playerName);
			messagePlaceholders.put("guildTagColor", guildTagColor);
			messagePlaceholders.put("guildTag", guildTag);

			for (Player poop : guild.getOnlinePlayers()) {
				if (!poop.equals(player)) {
					poop.sendMessage(Messages.get("guild-join-message", messagePlaceholders));
				}
			}
			
			guild.getDatabase().incrementOnline();
		}
	}


	@Subscribe
	public void onServerConnectMsg(ServerPreConnectEvent serverPreConnectEvent) {
		Player player = serverPreConnectEvent.getPlayer();

		if (serverPreConnectEvent.getResult() == ServerPreConnectEvent.ServerResult.denied()) {
			return;
		}

		RegisteredServer registeredServer = serverPreConnectEvent.getOriginalServer();

		player.sendMessage(Component.text("Sending to server " + registeredServer.getServerInfo().getName() + "...", NamedTextColor.GRAY));
		player.sendMessage(Component.empty());
	}


	@Subscribe
	public void onServerConnected(ServerPostConnectEvent serverPostConnectEvent) {
		Player player = serverPostConnectEvent.getPlayer();
		UUID uuid = player.getUniqueId();

		if (joinTimestamps.containsKey(uuid)) {
			long joinTime = joinTimestamps.remove(uuid);
			long duration = System.currentTimeMillis() - joinTime;

			CoreAPI.debug(uuid, "Took " + duration + " ms to load instance.");
		}
	}

	@Subscribe
	public void onDisconnect(DisconnectEvent disconnectEvent) {
		Player player = disconnectEvent.getPlayer();
		UUID uuid = player.getUniqueId();
		Guild guild = Guild.getGuildFromPlayer(player);
		User user = User.getUser(uuid);

		joinTimestamps.remove(uuid);

		if (Punishment.isBanned(uuid, player.getRemoteAddress().getAddress().getHostAddress()) || !Authentication.shouldAutoLogin(player)) {
			return;
		}

		user.setData("last_logout", System.currentTimeMillis());

		for (UUID friendId : FriendsDB.getFriends(uuid)) {
			Player friend = CoreAPI.getProxyPlayerUsingUUID(friendId);
			User friendUser = User.getUser(friendId);
			if (friend != null && friend.isActive() && User.getUser(friendId).getBoolean("friend.join_leave_msg")) {
				friend.sendMessage(Messages.get("friend-leave-message", Map.of("playerRank", CoreAPI.getPlayerRank(player.getUniqueId()).getPrefix(), "playerName", player.getUsername())));
			}
			
			if (friend != null) {
				friendUser.decrementFCount();
			}
		}

		if (CoreAPI.getPlayerRank(player.getUniqueId()).isStaff()) {
			for (Player poo : Core.getInstance().getProxy().getAllPlayers()) {
				if (CoreAPI.getPlayerRank(poo.getUniqueId()).isStaff()) {
					poo.sendMessage(Messages.get("staff-alert-leave-message", Map.of("playerRank", CoreAPI.getPlayerRank(player.getUniqueId()).getPrefix(), "playerName", player.getUsername())));
				}
			}
		}

		if (guild != null) {
			String playerName = player.getUsername();
			String guildTagColor = guild.getTagColor();
			String guildTag = guild.getTag().isEmpty() ? "" : "[" + guild.getTag() + "] ";
			Map<String, String> messagePlaceholders = new HashMap<>();
			messagePlaceholders.put("playerRank", CoreAPI.getPlayerRank(player.getUniqueId()).getPrefix());
			messagePlaceholders.put("playerName", playerName);
			messagePlaceholders.put("guildTagColor", guildTagColor);
			messagePlaceholders.put("guildTag", guildTag);

			for (Player popo : guild.getOnlinePlayers()) {
				if (!popo.equals(player)) {
					popo.sendMessage(Messages.get("guild-leave-message", messagePlaceholders));
				}
			}
			
			guild.getDatabase().decrementOnline();
		}
	}


	@Subscribe
	public void onPluginMessage(PluginMessageEvent pluginMessageEvent) {
		String channel = pluginMessageEvent.getIdentifier().getId();

		String message = new String(pluginMessageEvent.getData(), StandardCharsets.UTF_8);

		Player player = Core.getInstance().getProxy().getPlayer(message).orElse(null);

		if (player == null) return;

		ByteArrayDataOutput byteArrayDataOutput = ByteStreams.newDataOutput();
		byteArrayDataOutput.writeUTF("MySubChannel");

		player.getCurrentServer().ifPresent(serverConnection -> serverConnection.sendPluginMessage(MinecraftChannelIdentifier.from("guilds:tag"), byteArrayDataOutput.toByteArray()));
	}

	private String calculateTime(long seconds) {
		int days = (int) TimeUnit.SECONDS.toDays(seconds);
		long hours = TimeUnit.SECONDS.toHours(seconds) - days * 24L;
		long minutes = TimeUnit.SECONDS.toMinutes(seconds) - TimeUnit.SECONDS.toHours(seconds) * 60;
		long secs = TimeUnit.SECONDS.toSeconds(seconds) - TimeUnit.SECONDS.toMinutes(seconds) * 60;

		String time = (days > 0 ? days + "d " : "") +
					  (hours > 0 ? hours + "h " : "") +
					  (minutes > 0 ? minutes + "m " : "") +
					  (secs > 0 ? secs + "s" : "");

		return time.trim();
	}

	public String name(String serverName, String beforeName, String afterName) {
		if (serverName.contains(beforeName)) return afterName;
		return serverName;
	}
}