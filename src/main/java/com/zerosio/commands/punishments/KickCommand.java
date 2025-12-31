package com.zerosio.commands.punishments;

import com.zerosio.api.CoreAPI;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.rank.Rank;
import com.zerosio.utility.CooldownManager;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.Arrays;
import java.util.List;

public class KickCommand extends CommandBase {

	@Override
	public String getName() {
		return "kick";
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("getout");
	}

	@Override
	public Rank getRequiredRank() {
		return Rank.MOD;
	}

	@Override
	public String getDescription() {
		return "Kick a player";
	}

	@Override
	public String getUsage() {
		return "/kick <player> <reason>";
	}

	@Override
	public void execute(ProxiedPlayer player, String[] args) {
		if (!player.hasPermission("owner.bypass")) {
			if (CooldownManager.isOnCooldown(player.getUniqueId(), "kick")) {
				long remaining = CooldownManager.getRemainingCooldown(player.getUniqueId(), "kick") / 1000;
				player.sendMessage(new TextComponent(
						"§cYou must wait " + remaining + " seconds before using this command again!"));
				return;
			}
			CooldownManager.setCooldown(player.getUniqueId(), "kick");
		}

		if (args.length >= 2) {
			String reason = "";

			for (int i = 1; i < args.length; ++i) {
				reason = reason + args[i] + " ";
			}

			ProxiedPlayer target = CoreAPI.getProxyPlayer(args[0]);
			if (target == null) {
				player.sendMessage("§cPlayer does not exist or offline.");
			}

			player.sendMessage("§aKicked player " + CoreAPI.getProxyPlayer(args[0]).getName() + " for " + reason);
			target.disconnect("§cYou have been kicked!\n\n§7Reason: §f" + reason + "\n" + "§7Find out more: §b§n"
					+ PunishmentDomains.KICK);
		} else {
			player.sendMessage("§cInvalid syntax. Correct: /kick <name> <reason>");
		}
	}
}