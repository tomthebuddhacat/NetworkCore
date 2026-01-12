package com.zerosio.commands;

import com.zerosio.commands.impl.CommandBase;
import com.zerosio.database.User;
import com.zerosio.rank.Rank;
import com.zerosio.sync.database.Pending;
import com.zerosio.sync.database.Synced;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;
import java.util.List;

public class LinkCommand extends CommandBase {

	@Override
	public String getName() {
		return "link";
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("linkk");
	}

	@Override
	public Rank getRequiredRank() {
		return Rank.DEFAULT;
	}

	@Override
	public String getDescription() {
		return "Generate link code";
	}

	@Override
	public String getUsage() {
		return "/link";
	}

	@Override
	public void execute(ProxiedPlayer player, String[] args) {
		if (Synced.isSynced(player.getUniqueId())) {
            player.sendMessage("§cYour account is already linked.");
            return;
        }

        String code = Pending.createRequest(player.getName(), player.getUniqueId());

        player.sendMessage("§eHere is your link code: ");

        TextComponent codeComponent = new TextComponent("§d§l" + code);
        codeComponent.setClickEvent(
                new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, code)
        );
        codeComponent.setHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new TextComponent[]{
                                new TextComponent("§eClick to copy!")
                        }
                )
        );

        player.sendMessage(codeComponent);
	}
}
