package net.calyro.api;

import net.calyro.database.User;
import net.calyro.rank.Rank;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public class CoreAPI {

	public static Rank getPlayerRank(UUID uuid) {
		User user = User.getUser(uuid);
		return user != null ? user.getRank() : Rank.DEFAULT;
	}

	public static Rank getPlayerRank(String uuid) {
		return getPlayerRank(UUID.fromString(uuid));
	}

	public static void setPlayerRank(UUID uuid, Rank rank) {
		User user = User.getUser(uuid);
		if (user != null)
			user.setRank(rank);
	}

	public static void setPlayerRank(String uuid, Rank rank) {
		setPlayerRank(UUID.fromString(uuid), rank);
	}

	public static void setPlayerRankToNext(UUID uuid) {
		User user = User.getUser(uuid);
		if (user == null)
			return;

		Rank[] ranks = Rank.values();
		int currentIndex = user.getRank().ordinal();
		if (currentIndex >= ranks.length - 1)
			return;

		user.setRank(ranks[currentIndex + 1]);
	}

	public static void setPlayerRankToNext(String uuid) {
		setPlayerRankToNext(UUID.fromString(uuid));
	}

	public static boolean isInAdminDebug(UUID uuid) {
		User user = User.getUser(uuid);
		return user != null && user.getBoolean("debug_mode");
	}

	public static boolean isInAdminDebug(String uuid) {
		return isInAdminDebug(UUID.fromString(uuid));
	}

	public static void setAdminDebugMode(UUID uuid, boolean enabled) {
		User user = User.getUser(uuid);
		if (user != null)
			user.setData("debug_mode", enabled);
	}

	public static void setAdminDebugMode(String uuid, boolean enabled) {
		setAdminDebugMode(UUID.fromString(uuid), enabled);
	}

	public static void debug(UUID uuid, String message) {
		ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
		if (player != null) {
			User user = User.getUser(uuid);
			if (user != null)
				user.debug(player, message);
		}
	}

	public static void debug(String uuid, String message) {
		debug(UUID.fromString(uuid), message);
	}

	public static ProxiedPlayer getProxyPlayer(String name) {
		return ProxyServer.getInstance().getPlayer(name);
	}

	public static ProxiedPlayer getProxyPlayerUsingUUID(UUID uuid) {
		return ProxyServer.getInstance().getPlayer(uuid);
	}

	public static void setRank(ProxiedPlayer player, Rank rank) {
		User user = User.getUser(player.getUniqueId());

		if (rank.isBelowOrEqual(Rank.YOUTUBE)) {
			Rank oldPkgRank = getPlayerRank(player.getUniqueId());

			user.setData("package_rank", oldPkgRank.name().toUpperCase());
			user.setData("new_package_rank", rank.name().toUpperCase());
			return;
		}

		user.setData("rank", rank.name().toUpperCase());
	}

	public static void setMonthlyRank(ProxiedPlayer player, Rank rank) {
		User.getUser(player.getUniqueId()).setMonthlyRank(rank);
	}

	public static String getPlayerServerName(ProxiedPlayer player) {
		if (player.getServer() != null) {
			ServerInfo server = player.getServer().getInfo();
			return server.getName();
		}
		return "unknown";
	}
}
