package com.zerosio.authentication;

import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.zerosio.Core;
import com.zerosio.Messages;
import com.zerosio.api.ControllerAPI;
import com.zerosio.api.CoreAPI;
import com.zerosio.database.User;
import com.zerosio.friends.database.FriendsDB;
import com.zerosio.guilds.Guild;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Authentication {

	private static final Map<UUID, ScheduledTask> registerTasks = new HashMap<>();
	private static final Map<UUID, ScheduledTask> loginTasks = new HashMap<>();
	private static final Map<UUID, ScheduledTask> registerTimeouts = new HashMap<>();
	private static final Map<UUID, ScheduledTask> loginTimeouts = new HashMap<>();
	private static final Map<UUID, String> authenticatedIPs = new HashMap<>();

	public static void register(Player player) {
		UUID uuid = player.getUniqueId();
		ServerInfo limbo = ControllerAPI.getRandomAvailableInstanceServerInfo("limbo");
		cancelRegisterTask(uuid);

		Core.getInstance().getProxy().getServer(limbo.getName()).ifPresent(registeredServer -> player.createConnectionRequest(registeredServer).connect());

		sendTitle(player, Component.text("Register!", NamedTextColor.RED), Component.text("/register <password> <password>", NamedTextColor.YELLOW));

		ScheduledTask scheduledTask = Core.getInstance().getProxy().getScheduler()
						.buildTask(Core.getInstance(), () -> {
							if (!player.isActive()) {
								cancelRegisterTask(uuid);
								return;
							}
							player.sendMessage(Component.text("Please enter ", NamedTextColor.YELLOW)
									.append(Component.text("/register <password> <confirm password>", NamedTextColor.GOLD)));
						})
								.repeat(2, TimeUnit.SECONDS)
				                .schedule();

		registerTasks.put(uuid, scheduledTask);

		ScheduledTask scheduledTask1 = Core.getInstance().getProxy().getScheduler()
				.buildTask(Core.getInstance(), () -> {
					if (player.isActive()) {
						player.disconnect(Component.text("You took too long to register!", NamedTextColor.RED));
					}
					cancelRegisterTask(uuid);
				})
				.delay(30, TimeUnit.SECONDS)
				.schedule();

		registerTimeouts.put(uuid, scheduledTask1);
	}

	public static void stopRegisterTask(Player player) {
		UUID uuid = player.getUniqueId();
		ServerInfo lobby = ControllerAPI.getRandomAvailableInstanceServerInfo("lobby");
		User user = User.getUser(uuid);
		cancelRegisterTask(uuid);

		clearTitle(player);
		storeAuthenticatedIP(player);

		player.sendMessage(Component.text("Successfully registered."));
		player.sendMessage(Component.text("Transporting you to the Main Lobby..."));

		Core.getInstance().getProxy().getServer(lobby.getName()).ifPresent(registeredServer -> player.createConnectionRequest(registeredServer).connect());

		user.setData("last_known_name", player.getUsername());
		user.setData("last_login", System.currentTimeMillis());
		AuthDB.setLastSessionValidation(uuid, Instant.now().toEpochMilli());

		for (UUID friendId : FriendsDB.getFriends(uuid)) {
			Player friend = CoreAPI.getProxyPlayerUsingUUID(friendId);
			User friendUser = User.getUser(friendId);
			if (friend != null ) {
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
					poo.sendMessage(Messages.get("staff-join-message", Map.of("playerRank", CoreAPI.getPlayerRank(player.getUniqueId()).getPrefix(), "playerName", player.getUsername())));
				}
			}
		}

		Guild guild = Guild.getGuildFromPlayer(player);
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

	public static void login(Player player) {
		UUID uuid = player.getUniqueId();
		ServerInfo limbo = ControllerAPI.getRandomAvailableInstanceServerInfo("limbo");
		cancelLoginTask(uuid);

		Core.getInstance().getProxy().getServer(limbo.getName()).ifPresent(server -> player.createConnectionRequest(server).connect());

		sendTitle(player, Component.text("Login!", NamedTextColor.GREEN), Component.text("/login <password>", NamedTextColor.YELLOW));

		ScheduledTask scheduledTask = Core.getInstance().getProxy().getScheduler()
				.buildTask(Core.getInstance(), () -> {
					if (!player.isActive()) {
						cancelLoginTask(uuid);
						return;
					}

					player.sendMessage(Component.text("Please enter ", NamedTextColor.YELLOW)
							.append(Component.text("/login <password>", NamedTextColor.GREEN)));
				})
				.repeat(2, TimeUnit.SECONDS)
				.schedule();
		loginTasks.put(uuid, scheduledTask);

		ScheduledTask scheduledTask1 = Core.getInstance().getProxy().getScheduler()
				.buildTask(Core.getInstance(), () -> {
					if (player.isActive()) {
						player.disconnect(Component.text("You took too login!", NamedTextColor.RED));
					}
					cancelLoginTask(uuid);
				})
				.delay(30, TimeUnit.SECONDS)
				.schedule();
	}

	public static void stopLoginTask(Player player) {
		UUID uuid = player.getUniqueId();
		ServerInfo lobby = ControllerAPI.getRandomAvailableInstanceServerInfo("lobby");
		User user = User.getUser(uuid);
		cancelLoginTask(uuid);

		clearTitle(player);
		storeAuthenticatedIP(player);

		player.sendMessage(Component.text("§aSuccessfully logged in."));
		player.sendMessage(Component.text("§aTransporting you to the Main Lobby..."));

		Core.getInstance().getProxy().getServer(lobby.getName()).ifPresent(registeredServer -> player.createConnectionRequest(registeredServer).connect());

		user.setData("last_known_name", player.getUsername());
		user.setData("last_login", System.currentTimeMillis());
		AuthDB.setLastSessionValidation(uuid, Instant.now().toEpochMilli());

		for (UUID friendId : FriendsDB.getFriends(uuid)) {
			Player friend = CoreAPI.getProxyPlayerUsingUUID(friendId);
			User friendUser = User.getUser(friendId);
			if (friend != null) {
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
					poo.sendMessage(Messages.get("staff-join-message", Map.of("playerRank", CoreAPI.getPlayerRank(player.getUniqueId()).getPrefix(), "playerName", player.getUsername())));
				}
			}
		}

		Guild guild = Guild.getGuildFromPlayer(player);
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

	public static boolean shouldAutoLogin(Player player) {
		User user = User.getUser(player.getUniqueId());
		if (user == null) return false;

		if (AuthDB.getPremiumUUID(user) != null) {
			return true;
		}
		
		return isIPAuthenticated(player);
	}

	public static void storeAuthenticatedIP(Player player) {
		UUID uuid = player.getUniqueId();
		String ip = player.getRemoteAddress().getAddress().getHostAddress();
		authenticatedIPs.put(uuid, ip);
	}

	public static boolean isIPAuthenticated(Player player) {
		UUID uuid = player.getUniqueId();
		String currentIP = player.getRemoteAddress().getAddress().getHostAddress();
		String storedIP = authenticatedIPs.get(uuid);

		return storedIP != null && storedIP.equals(currentIP);
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

	private static void sendTitle(Player player, Component title, Component subtitle) {
		player.showTitle(Title.title(title, subtitle,
				Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(4000), Duration.ofMillis(0))
		));
	}

	private static void clearTitle(Player player) {
		player.clearTitle();
	}
}
