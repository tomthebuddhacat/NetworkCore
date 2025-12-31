package com.zerosio.commands;

import com.zerosio.commands.impl.CommandBase;
import com.zerosio.database.User;
import com.zerosio.rank.Rank;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;
import java.util.List;

public class DebugCommand extends CommandBase {

	@Override
	public String getName() {
		return "debug";
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("debog");
	}

	@Override
	public Rank getRequiredRank() {
		return Rank.ADMIN;
	}

	@Override
	public String getDescription() {
		return "Toggle debug mode";
	}

	@Override
	public String getUsage() {
		return "/debug";
	}

	@Override
	public void execute(ProxiedPlayer player, String[] args) {
		User user = User.getUser(player.getUniqueId());

		boolean debugMode = user.getData("debug_mode");

		if (debugMode) {
			user.setData("debug_mode", false);
			player.sendMessage(new TextComponent("§c[SYSTEM] §fDebug mode turned §coff§f."));
		} else if (!debugMode) {
			user.setData("debug_mode", true);
			player.sendMessage(new TextComponent("§c[SYSTEM] §fDebug mode turned §aon§f."));
		}
	}
}
