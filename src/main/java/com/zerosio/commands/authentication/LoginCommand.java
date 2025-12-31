package com.zerosio.commands.authentication;

import com.zerosio.authentication.AuthDB;
import com.zerosio.authentication.Authentication;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.rank.Rank;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;
import java.util.List;

public class LoginCommand extends CommandBase {

	@Override
	public String getName() {
		return "login";
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("log");
	}

	@Override
	public Rank getRequiredRank() {
		return Rank.DEFAULT;
	}

	@Override
	public String getDescription() {
		return "Login if not premium";
	}

	@Override
	public String getUsage() {
		return "/login <password>";
	}

	@Override
	public void execute(ProxiedPlayer player, String[] args) {
		if (Authentication.shouldAutoLogin(player)) {
			player.sendMessage("§cYou are already logged in.");
			return;
		}
		
		if (AuthDB.isPremium(player.getUniqueId())) {
			player.sendMessage("§cYour current session is in premium mode!");
			return;
		}
		
		if (args.length < 1) {
            player.sendMessage("§cUsage: " + getUsage());
            return;
        }
        
        String password = args[0];
        
        if (!AuthDB.checkPassword(player.getUniqueId(), password)) {
            player.sendMessage("§cThis password does not match our records!");
        } else {
        	Authentication.stopLoginTask(player);
        }
	}
}