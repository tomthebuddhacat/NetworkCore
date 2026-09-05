package com.zerosio.commands.authentication;

import com.velocitypowered.api.proxy.Player;
import com.zerosio.Messages;
import com.zerosio.authentication.AuthDB;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.database.User;
import com.zerosio.rank.Rank;
import com.zerosio.utility.PremiumUtil;

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
	public void execute(Player player, String[] args) {
		
		boolean premiumMode = AuthDB.isPremium(player.getUniqueId());
		
		if (!PremiumUtil.isPremium(player)) {
			player.sendMessage(Messages.get("must-be-a-premium-account"));
			return;
		}

		if (premiumMode) {
			
			AuthDB.setPremium(player.getUniqueId(), false);
			player.sendMessage(Messages.get("premium-mode-enabled"));
			return;
		} else if (!premiumMode) {
			AuthDB.setPremium(player.getUniqueId(), true);
			player.sendMessage(Messages.get("premium-mode-disabled"));
			if (!AuthDB.isRegistered(player.getUniqueId())) {
				player.sendMessage(Messages.get("your-account-is-not-registered-yet"));
			}
			return;
		}
	}
}
