package com.zerosio.commands;

import com.zerosio.commands.impl.CommandBase;
import com.zerosio.database.User;
import com.zerosio.rank.Rank;
import com.zerosio.sync.database.Synced;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;
import java.util.List;

public class UnlinkCommand extends CommandBase {

	@Override
	public String getName() {
		return "unlink";
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("unlinkk");
	}

	@Override
	public Rank getRequiredRank() {
		return Rank.ADMIN;
	}

	@Override
	public String getDescription() {
		return "Unlink to discord account";
	}

	@Override
	public String getUsage() {
		return "/unlink";
	}

	@Override
	public void execute(ProxiedPlayer player, String[] args) {
		if (!Synced.isSynced(player.getUniqueId())) {
            player.sendMessage("§cYou are not linked! First link with your account.");
            return;
        }

        Synced.unlink(player.getUniqueId());
        
        player.sendMessage("§aSuccessfully unlinked!");
	}
}
