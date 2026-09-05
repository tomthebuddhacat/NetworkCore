package com.zerosio.commands.authentication;

import com.velocitypowered.api.proxy.Player;
import com.zerosio.Messages;
import com.zerosio.authentication.AuthDB;
import com.zerosio.authentication.Authentication;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.rank.Rank;

import java.util.Arrays;
import java.util.List;

public class RegisterCommand extends CommandBase {

	@Override
	public String getName() {
		return "register";
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("reg");
	}

	@Override
	public Rank getRequiredRank() {
		return Rank.DEFAULT;
	}

	@Override
	public String getDescription() {
		return "Register if cracked mode";
	}

	@Override
	public String getUsage() {
		return "/register <password> <confirm password>";
	}

	@Override
	public void execute(Player player, String[] args) {
		if (Authentication.shouldAutoLogin(player)) {
			player.sendMessage(Messages.get("you-are-already-logged-in"));
			return;
		}

		if (AuthDB.isPremium(player.getUniqueId())) {
			player.sendMessage(Messages.get("premium-mode-session"));
			return;
		}
		
		if (args.length < 2) {
			player.sendMessage(Messages.get("register-command-usage"));
			return;
        }

        String password = args[0];
        String confirm = args[1];

        if (!password.equals(confirm)) {
			player.sendMessage(Messages.get("entered-password-does-not-match"));
			return;
        }
        
        if (AuthDB.isRegistered(player.getUniqueId())) {
			player.sendMessage(Messages.get("account-already-registered"));
        	return;
        }
        
        AuthDB.register(player.getUniqueId(), password);
        Authentication.stopRegisterTask(player);
	}
}