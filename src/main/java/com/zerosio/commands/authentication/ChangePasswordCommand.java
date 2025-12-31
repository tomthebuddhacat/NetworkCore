package com.zerosio.commands.authentication;

import com.zerosio.authentication.AuthDB;
import com.zerosio.authentication.Authentication;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.rank.Rank;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;
import java.util.List;

public class ChangePasswordCommand extends CommandBase {

	@Override
	public String getName() {
		return "changepassword";
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("changepass");
	}

	@Override
	public Rank getRequiredRank() {
		return Rank.DEFAULT;
	}

	@Override
	public String getDescription() {
		return "Change password if cracked mode";
	}

	@Override
	public String getUsage() {
		return "/changepassword <old password> <new password>";
	}

	@Override
	public void execute(ProxiedPlayer player, String[] args) {
		if (!Authentication.shouldAutoLogin(player)) {
			player.sendMessage("§cLogin first.");
			return;
		}
		
		if (AuthDB.isPremium(player.getUniqueId())) {
			player.sendMessage("§cYou're in premium mode!");
			return;
		}
		
		if (args.length < 2) {
            player.sendMessage("§cUsage: " + getUsage());
            return;
        }

        String oldPass = args[0];
        String newPass = args[1];

        if (!AuthDB.checkPassword(player.getUniqueId(), oldPass)) {
            player.sendMessage("§cWrong password!");
            return;
        }
        
        AuthDB.setPassword(player.getUniqueId(), newPass);
        player.sendMessage("§aChanged your password!");
	}
}


// on /premium if player switches to non premium and isn't registered tell it to register and warn him. (note so that I don't forget cuz I'm a dumbfuck)