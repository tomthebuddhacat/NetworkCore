package com.zerosio.listeners;

import com.zerosio.Config;
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

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import org.bson.Document;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PlayerListener implements Listener {

	private final Map<UUID, Long> joinTimestamps = new ConcurrentHashMap<>();
	String serverName;

	@EventHandler(priority = EventPriority.HIGH)
	public void onServerConnect(ServerConnectEvent event) {
		if (event.getReason() != ServerConnectEvent.Reason.JOIN_PROXY) {
			return;
		}

		ProxiedPlayer player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		User user = User.getUser(uuid);
		boolean isPremium = PremiumUtil.isPremium(player);
		long currentUnix = System.currentTimeMillis() / 1000L;
		boolean maintenance = (boolean) Config.get("maintenance");

		Guild guild = Guild.getGuildFromPlayer(player);

		if (maintenance && !CoreAPI.getPlayerRank(uuid).isStaff()) {
			player.disconnect("§cWe are sorry but the ShardMC network is currently down for maintenance.\n§cFor more information: §bshardmc.net");
			return;
		}

		// Check active ban
		if (Punishment.isBanned(uuid, player.getAddress().getAddress().getHostAddress())) {
			Document ban = Punishment.getActiveBan(uuid, player.getAddress().getAddress().getHostAddress());
			if (ban != null) {
				String reason = ban.getString("reason");
				String id = ban.getString("id");
				long length = ban.getLong("length");
				long timestamp = ban.getLong("timestamp");

				String kickMessage;
				if (length == -1) {
					kickMessage = "§cYou are permanently banned from this server!\n\n" +
								  "§7Reason: §f" + reason + "\n" +
								  "§7Find out more: §b§n" + PunishmentDomains.BAN + "\n\n" +
								  "§7Ban ID: §f#" + id + "\n" +
								  "§7Sharing your Ban ID may affect the processing of your appeal!";
				} else {
					long expiry = timestamp + (length * 1000L);
					long remaining = (expiry - System.currentTimeMillis()) / 1000L;
					kickMessage = "§cYou are temporarily banned for §f" + calculateTime(remaining) + " §cfrom this server!\n\n" +
								  "§7Reason: §f" + reason + "\n" +
								  "§7Find out more: §b§n" + PunishmentDomains.BAN + "\n\n" +
								  "§7Ban ID: §f#" + id + "\n" +
								  "§7Sharing your Ban ID may affect the processing of your appeal!";
				}

				player.disconnect(kickMessage);
				return;
			}
		}

		if (!AuthListener.processPostLogin(event)) {
			return;
		}

		Authentication.storeAuthenticatedIP(player);
		user.setData("last_known_name", player.getName());
		user.setData("last_login", System.currentTimeMillis());

		joinTimestamps.put(uuid, System.currentTimeMillis());

		// Notify friends
		for (UUID friendId : FriendsDB.getFriends(uuid)) {
			ProxiedPlayer friend = CoreAPI.getProxyPlayerUsingUUID(friendId);
			User friendUser = User.getUser(friendId);
			if (friend != null && friend.isConnected()) {
				if (friendUser.getBoolean("friend.join_leave_msg")) {
					friend.sendMessage("§aFriend > " + CoreAPI.getPlayerRank(uuid).getPrefix() + player.getName() + " §ejoined.");
				}
			}
			
			if (friend != null) {
				friendUser.incrementFCount();
			}
		}

		if (CoreAPI.getPlayerRank(player.getUniqueId()).isStaff()) {
			for (ProxiedPlayer poo : ProxyServer.getInstance().getPlayers()) {
				if (CoreAPI.getPlayerRank(poo.getUniqueId()).isStaff()) {
					poo.sendMessage("§b[STAFF] " + CoreAPI.getPlayerRank(player.getUniqueId()).getPrefix() + player.getName() + " §econnected.");
				}
			}
		}

		if (guild != null) {
			String playerName = player.getName();
			String guildTagColor = guild.getTagColor();
			String guildTag = guild.getTag().isEmpty() ? "" : "[" + guild.getTag() + "] ";

			for (ProxiedPlayer poop : guild.getOnlinePlayers()) {
				if (!poop.equals(player)) {
					poop.sendMessage("§" + guildTagColor + guildTag + CoreAPI.getPlayerRank(player.getUniqueId()).getPrefix() + playerName + " §ejoined.");
				}
			}
			
			guild.getDatabase().incrementOnline();
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onServerConnectMsg(ServerConnectEvent event) {
		
		event.getPlayer().sendMessage("§7Sending to server " + event.getTarget().getName() + "...");
		event.getPlayer().sendMessage("");
		
	}

	@EventHandler
	public void onServerConnected(ServerConnectedEvent event) {
		ProxiedPlayer player = event.getPlayer();
		UUID uuid = player.getUniqueId();

		if (joinTimestamps.containsKey(uuid)) {
			long joinTime = joinTimestamps.remove(uuid);
			long duration = System.currentTimeMillis() - joinTime;
			CoreAPI.debug(uuid, "took §b" + duration + "ms §fto load instance.");
		}
	}

	@EventHandler
	public void onDisconnect(PlayerDisconnectEvent event) {
		ProxiedPlayer player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		Guild guild = Guild.getGuildFromPlayer(player);
		User user = User.getUser(uuid);

		joinTimestamps.remove(uuid);

		if (Punishment.isBanned(uuid, player.getAddress().getAddress().getHostAddress()) || !Authentication.shouldAutoLogin(player)) {
			return;
		}

		user.setData("last_logout", System.currentTimeMillis());

		for (UUID friendId : FriendsDB.getFriends(uuid)) {
			ProxiedPlayer friend = CoreAPI.getProxyPlayerUsingUUID(friendId);
			User friendUser = User.getUser(friendId);
			if (friend != null && friend.isConnected() && User.getUser(friendId).getBoolean("friend.join_leave_msg")) {
				friend.sendMessage("§aFriend > " + CoreAPI.getPlayerRank(uuid).getPrefix() + player.getName() + " §eleft.");
			}
			
			if (friend != null) {
				friendUser.decrementFCount();
			}
		}

		if (CoreAPI.getPlayerRank(player.getUniqueId()).isStaff()) {
			for (ProxiedPlayer poo : ProxyServer.getInstance().getPlayers()) {
				if (CoreAPI.getPlayerRank(poo.getUniqueId()).isStaff()) {
					poo.sendMessage("§b[STAFF] " + CoreAPI.getPlayerRank(player.getUniqueId()).getPrefix() + player.getName() + " §edisconnected.");
				}
			}
		}

		if (guild != null) {
			String playerName = player.getName();
			String guildTagColor = guild.getTagColor();
			String guildTag = guild.getTag().isEmpty() ? "" : "[" + guild.getTag() + "] ";

			for (ProxiedPlayer popo : guild.getOnlinePlayers()) {
				if (!popo.equals(player)) {
					popo.sendMessage("§" + guildTagColor + guildTag + CoreAPI.getPlayerRank(player.getUniqueId()).getPrefix() + playerName + " §eleft.");
				}
			}
			
			guild.getDatabase().decrementOnline();
		}
	}

	@EventHandler
	public void onPluginMessage(PluginMessageEvent messageEvent) {
		String channel = messageEvent.getTag();
		String message = new String(messageEvent.getData());

		if (ProxyServer.getInstance().getPlayer(message) != null) {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("MySubChannel");
			ProxyServer.getInstance().getPlayer(message).getServer().getInfo().sendData("guilds:tag", out.toByteArray());
		}
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


// »