package com.zerosio.commands.authentication;

import com.velocitypowered.api.proxy.Player;
import com.zerosio.Messages;
import com.zerosio.authentication.AuthDB;
import com.zerosio.authentication.Authentication;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.rank.Rank;
import net.kyori.adventure.text.Component;

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
	public void execute(Player player, String[] args) {
		if (Authentication.shouldAutoLogin(player)) {
			player.sendMessage(Messages.get("you-are-already-logged-in"));
			return;
		}
		
		if (AuthDB.isPremium(player.getUniqueId())) {
			player.sendMessage(Messages.get("premium-mode-session"));
			return;
		}
		
		if (args.length < 1) {
			player.sendMessage(Messages.get("login-command-usage").replaceText(builder -> builder
					.match("%loginCommandUsage%")
					.replacement(Component.text(getUsage()))));
            return;
        }
        
        String password = args[0];
        
        if (!AuthDB.checkPassword(player.getUniqueId(), password)) {
			player.sendMessage(Messages.get("invalid-password-entered-please-check-again"));
		} else {
        	Authentication.stopLoginTask(player);
        }
	}
}