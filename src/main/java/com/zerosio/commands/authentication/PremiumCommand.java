package com.zerosio.commands.authentication;

import com.zerosio.authentication.AuthDB;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.database.User;
import com.zerosio.rank.Rank;
import com.zerosio.utility.PremiumUtil;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;
import java.util.List;

public class PremiumCommand extends CommandBase {

	@Override
	public String getName() {
		return "premium";
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("premiumm");
	}

	@Override
	public Rank getRequiredRank() {
		return Rank.ADMIN;
	}

	@Override
	public String getDescription() {
		return "Toggle between premium mode";
	}

	@Override
	public String getUsage() {
		return "/premium";
	}

	@Override
	public void execute(ProxiedPlayer player, String[] args) {
		
		boolean premiumMode = AuthDB.isPremium(player.getUniqueId());
		
		if (!PremiumUtil.isPremium(player)) {
			player.sendMessage("§cYou need to have a premium account to use this command!");
			return;
		}

		if (premiumMode) {
			
			AuthDB.setPremium(player.getUniqueId(), false);
			player.sendMessage("§ePlayer mode has been §aenabled§e.");
			return;
		} else if (!premiumMode) {
			AuthDB.setPremium(player.getUniqueId(), true);
			player.sendMessage("§ePlayer mode has been §cdisabled§e.");
			if (!AuthDB.isRegistered(player.getUniqueId())) {
				player.sendMessage("§e§lWARNING §r§eRegister your account using '/register <password> <confirm password>' because your account isn't registered yet!");
			}
			return;
		}
	}
}
