package net.calyro.authentication;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.calyro.Core;
import net.calyro.api.ControllerAPI;
import net.calyro.api.CoreAPI;
import net.calyro.database.User;
import net.calyro.friends.database.FriendsDB;
import net.calyro.guilds.Guild;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Authentication {

	private static final Map<UUID, ScheduledTask> registerTasks = new HashMap<>();
	private static final Map<UUID, ScheduledTask> loginTasks = new HashMap<>();
	private static final Map<UUID, ScheduledTask> registerTimeouts = new HashMap<>();
	private static final Map<UUID, ScheduledTask> loginTimeouts = new HashMap<>();
	private static final Map<UUID, String> authenticatedIPs = new HashMap<>();

	public static void register(ServerConnectEvent event) {
		ProxiedPlayer player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		ServerInfo limbo = ControllerAPI.getRandomAvailableInstanceServerInfo("limbo");
		cancelRegisterTask(uuid);

		event.setTarget(limbo);

		sendTitle(player,
				  ChatColor.RED + "Register!",
				  ChatColor.YELLOW + "/register <password> <password>");

		ScheduledTask task = ProxyServer.getInstance().getScheduler().schedule(
								 Core.getInstance(),
		() -> {
			if (!player.isConnected()) {
				cancelRegisterTask(uuid);
				return;
			}
			player.sendMessage(new TextComponent("§ePlease enter §6/register <password> <confirm password>"));
		},
		0L, 2L, TimeUnit.SECONDS
							 );

		registerTasks.put(uuid, task);

		// 30-second kick timer
		ScheduledTask timeout = ProxyServer.getInstance().getScheduler().schedule(
									Core.getInstance(),
		() -> {
			if (player.isConnected()) {
				player.disconnect(new TextComponent("§cYou took too long to register!"));
				cancelRegisterTask(uuid);
			}
		},
		30, TimeUnit.SECONDS
								);

		registerTimeouts.put(uuid, timeout);
	}

	public static void stopRegisterTask(ProxiedPlayer player) {
		UUID uuid = player.getUniqueId();
		ServerInfo lobby = ControllerAPI.getRandomAvailableInstanceServerInfo("lobby");
		User user = User.getUser(uuid);
		cancelRegisterTask(uuid);

		clearTitle(player);
		storeAuthenticatedIP(player);

		player.sendMessage(new TextComponent("§aSuccessfully registered."));
		player.sendMessage(new TextComponent("§aTransporting you to the Main Lobby..."));

		player.connect(lobby);

		user.setData("last_known_name", player.getName());
		user.setData("last_login", System.currentTimeMillis());
		AuthDB.setLastSessionValidation(uuid, Instant.now().toEpochMilli());

		for (UUID friendId : FriendsDB.getFriends(uuid)) {
			ProxiedPlayer friend = CoreAPI.getProxyPlayerUsingUUID(friendId);
			if (friend != null && friend.isConnected()) {
				User friendUser = User.getUser(friendId);
				if (friendUser.getBoolean("friend.join_leave_msg")) {
					friend.sendMessage("§aFriend > " + CoreAPI.getPlayerRank(uuid).getPrefix() + player.getName() + " §ejoined.");
				}
			}
		}

		if (CoreAPI.getPlayerRank(player.getUniqueId()).isStaff()) {
			for (ProxiedPlayer poo : ProxyServer.getInstance().getPlayers()) {
				if (CoreAPI.getPlayerRank(poo.getUniqueId()).isStaff()) {
					poo.sendMessage(CoreAPI.getPlayerRank(player.getUniqueId()).getPrefix() + player.getName() + " §econnected.");
				}
			}
		}

		Guild guild = Guild.getGuildFromPlayer(player);
		if (guild != null) {
			String playerName = player.getName();
			String guildTagColor = guild.getTagColor();
			String guildTag = guild.getTag().isEmpty() ? "" : "[" + guild.getTag() + "] ";

			for (ProxiedPlayer poop : guild.getOnlinePlayers()) {
				if (!poop.equals(player)) {
					poop.sendMessage("§" + guildTagColor + guildTag + CoreAPI.getPlayerRank(player.getUniqueId()).getPrefix() + playerName + " §ejoined.");
				}
			}
		}
	}

	public static void login(ServerConnectEvent event) {
		ProxiedPlayer player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		ServerInfo limbo = ControllerAPI.getRandomAvailableInstanceServerInfo("limbo");
		cancelLoginTask(uuid);

		event.setTarget(limbo);

		sendTitle(player,
				  ChatColor.GREEN + "Login!",
				  ChatColor.YELLOW + "/login <password>");

		ScheduledTask task = ProxyServer.getInstance().getScheduler().schedule(
								 Core.getInstance(),
		() -> {
			if (!player.isConnected()) {
				cancelLoginTask(uuid);
				return;
			}
			player.sendMessage(new TextComponent("§ePlease enter §a/login <password>"));
		},
		0L, 2L, TimeUnit.SECONDS
							 );

		loginTasks.put(uuid, task);

		// 30-second kick timer
		ScheduledTask timeout = ProxyServer.getInstance().getScheduler().schedule(
									Core.getInstance(),
		() -> {
			if (player.isConnected()) {
				player.disconnect(new TextComponent("§cYou took too long to login!"));
				cancelLoginTask(uuid);
			}
		},
		30, TimeUnit.SECONDS
								);

		loginTimeouts.put(uuid, timeout);
	}

	public static void stopLoginTask(ProxiedPlayer player) {
		UUID uuid = player.getUniqueId();
		ServerInfo lobby = ControllerAPI.getRandomAvailableInstanceServerInfo("lobby");
		User user = User.getUser(uuid);
		cancelLoginTask(uuid);

		clearTitle(player);
		storeAuthenticatedIP(player);

		player.sendMessage(new TextComponent("§aSuccessfully logged in."));
		player.sendMessage(new TextComponent("§aTransporting you to the Main Lobby..."));

		player.connect(lobby);

		user.setData("last_known_name", player.getName());
		user.setData("last_login", System.currentTimeMillis());
		AuthDB.setLastSessionValidation(uuid, Instant.now().toEpochMilli());

		for (UUID friendId : FriendsDB.getFriends(uuid)) {
			ProxiedPlayer friend = CoreAPI.getProxyPlayerUsingUUID(friendId);
			if (friend != null && friend.isConnected()) {
				User friendUser = User.getUser(friendId);
				if (friendUser.getBoolean("friend.join_leave_msg")) {
					friend.sendMessage("§aFriend > " + CoreAPI.getPlayerRank(uuid).getPrefix() + player.getName() + " §ejoined.");
				}
			}
		}

		if (CoreAPI.getPlayerRank(player.getUniqueId()).isStaff()) {
			for (ProxiedPlayer poo : ProxyServer.getInstance().getPlayers()) {
				if (CoreAPI.getPlayerRank(poo.getUniqueId()).isStaff()) {
					poo.sendMessage(CoreAPI.getPlayerRank(player.getUniqueId()).getPrefix() + player.getName() + " §econnected.");
				}
			}
		}

		Guild guild = Guild.getGuildFromPlayer(player);
		if (guild != null) {
			String playerName = player.getName();
			String guildTagColor = guild.getTagColor();
			String guildTag = guild.getTag().isEmpty() ? "" : "[" + guild.getTag() + "] ";

			for (ProxiedPlayer poop : guild.getOnlinePlayers()) {
				if (!poop.equals(player)) {
					poop.sendMessage("§" + guildTagColor + guildTag + CoreAPI.getPlayerRank(player.getUniqueId()).getPrefix() + playerName + " §ejoined.");
				}
			}
		}
	}

	public static boolean shouldAutoLogin(ProxiedPlayer player) {
		User user = User.getUser(player.getUniqueId());
		if (user == null) return false;

		Duration sessionTime = Duration.ofSeconds(604800);
		if (AuthDB.getPremiumUUID(user) != null) {
			return true;
		} else return sessionTime != null && AuthDB.getLastSessionValidation(player.getUniqueId()) != -1
						  && LocalDateTime.ofInstant(Instant.ofEpochMilli(AuthDB.getLastSessionValidation(player.getUniqueId())), ZoneId.systemDefault()).plus(sessionTime).isAfter(LocalDateTime.now());
	}

	public static void storeAuthenticatedIP(ProxiedPlayer player) {
		UUID uuid = player.getUniqueId();
		String ip = player.getAddress().getAddress().getHostAddress();
		authenticatedIPs.put(uuid, ip);
	}

	private static void cancelRegisterTask(UUID uuid) {
		ScheduledTask task = registerTasks.remove(uuid);
		if (task != null) task.cancel();

		ScheduledTask timeout = registerTimeouts.remove(uuid);
		if (timeout != null) timeout.cancel();
	}

	private static void cancelLoginTask(UUID uuid) {
		ScheduledTask task = loginTasks.remove(uuid);
		if (task != null) task.cancel();

		ScheduledTask timeout = loginTimeouts.remove(uuid);
		if (timeout != null) timeout.cancel();
	}

	private static void sendTitle(ProxiedPlayer player, String title, String subtitle) {
		Title t = ProxyServer.getInstance().createTitle();
		t.title(new TextComponent(title));
		t.subTitle(new TextComponent(subtitle));
		t.fadeIn(10);
		t.stay(800);
		t.fadeOut(0);
		t.send(player);
	}

	private static void clearTitle(ProxiedPlayer player) {
		Title t = ProxyServer.getInstance().createTitle();
		t.reset();
		t.send(player);
	}
}
