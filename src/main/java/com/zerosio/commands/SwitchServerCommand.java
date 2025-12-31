package com.zerosio.commands;

import com.zerosio.commands.impl.CommandBase;
import com.zerosio.rank.Rank;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.Arrays;
import java.util.List;

public class SwitchServerCommand extends CommandBase {
	String serverName;

	@Override
	public String getName() {
		return "switchserver";
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("ss");
	}

	@Override
	public Rank getRequiredRank() {
		return Rank.DEFAULT;
	}

	@Override
	public String getDescription() {
		return "Switch to another server";
	}

	@Override
	public String getUsage() {
		return "/switchserver <server>";
	}

	@Override
	public void execute(ProxiedPlayer player, String[] args) {
		if (args.length < 1) {
			player.sendMessage(new TextComponent("§cUsage: " + getUsage()));
			return;
		}

		String serverName = args[0];
		ServerInfo target = ProxyServer.getInstance().getServerInfo(serverName);

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

		if (target == null || serverName.contains("sbdu") || serverName.contains("sbku")) {
			player.sendMessage(new TextComponent("§c[SYSTEM] §fServer §e" + serverName + " §fwas not found."));
			return;
		}

		if (player.getServer() != null && player.getServer().getInfo().equals(target)) {
			player.sendMessage(new TextComponent("§c[SYSTEM] §fYou are already connected to §e" + serverName + "§f."));
			return;
		}

		player.connect(target);
		player.sendMessage(new TextComponent("§a[SYSTEM] §fConnecting you to §e" + serverName + "§f..."));
	}

	public String name(String serverName, String beforeName, String afterName) {
		if (serverName.contains(beforeName)) return afterName;
		return serverName;
	}

}
