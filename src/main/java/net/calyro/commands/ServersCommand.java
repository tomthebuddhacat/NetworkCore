package net.calyro.commands;

import net.calyro.commands.impl.CommandBase;
import net.calyro.rank.Rank;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.*;
import java.util.Map.Entry;

public class ServersCommand extends CommandBase {

	@Override
	public String getName() {
		return "servers";
	}

	@Override
	public Rank getRequiredRank() {
		return Rank.OWNER;
	}

	@Override
	public String getDescription() {
		return "List of registered bungeecord servers";
	}

	@Override
	public String getUsage() {
		return "/servers";
	}

	@Override
	public void execute(ProxiedPlayer sender, String[] args) {

		Map<String, ServerInfo> infos = ProxyServer.getInstance().getServers();
		
		sender.sendMessage("Available instances");

		for (ServerInfo info : infos.values()) {
			sender.sendMessage("§a" + info.getName() + "   §7[" + info.getPlayers() + "]   §2§lRUNNING");
		}

	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("svs");
	}
}
