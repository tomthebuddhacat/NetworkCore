package net.calyro.commands.authentication;

import net.calyro.authentication.AuthDB;
import net.calyro.authentication.Authentication;
import net.calyro.commands.impl.CommandBase;
import net.calyro.rank.Rank;
import net.md_5.bungee.api.connection.ProxiedPlayer;

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
	public void execute(ProxiedPlayer player, String[] args) {
		if (Authentication.shouldAutoLogin(player)) {
			player.sendMessage("§cYou are already logged in.");
			return;
		}

		if (AuthDB.isPremium(player.getUniqueId())) {
			player.sendMessage("§cYour current session is in premium mode!");
			return;
		}
		
		if (args.length < 2) {
            player.sendMessage("§cUsage: /register <password> <confirm password>");
            return;
        }

        String password = args[0];
        String confirm = args[1];

        if (!password.equals(confirm)) {
            player.sendMessage("§cPasswords do not match!");
            return;
        }
        
        if (AuthDB.isRegistered(player.getUniqueId())) {
        	player.sendMessage("§cYou've already registered, use '/login <password>' to log back in.");
        	return;
        }
        
        AuthDB.register(player.getUniqueId(), password);
        Authentication.stopRegisterTask(player);
	}
}