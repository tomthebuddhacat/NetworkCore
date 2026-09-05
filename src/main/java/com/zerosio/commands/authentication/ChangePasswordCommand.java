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
	public void execute(Player player, String[] args) {
		if (!Authentication.shouldAutoLogin(player)) {
			player.sendMessage(Messages.get("you-must-login-first"));
			return;
		}
		
		if (AuthDB.isPremium(player.getUniqueId())) {
			player.sendMessage(Messages.get("you-are-in-premium-mode"));
			return;
		}
		
		if (args.length < 2) {
			player.sendMessage(Messages.get("change-password-command-usage").replaceText(builder -> builder
					.match("%changePasswordCommandUsage%")
					.replacement(Component.text(getUsage()))));
            return;
        }

        String oldPass = args[0];
        String newPass = args[1];

        if (!AuthDB.checkPassword(player.getUniqueId(), oldPass)) {
			player.sendMessage(Messages.get("incorrect-password-entered"));
			return;
        }
        
        AuthDB.setPassword(player.getUniqueId(), newPass);
		player.sendMessage(Messages.get("you-can-change-your-password"));
	}
}
