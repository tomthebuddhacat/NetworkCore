package net.calyro.commands;

import net.calyro.api.CoreAPI;
import net.calyro.commands.impl.CommandBase;
import net.calyro.privacy.Ignores;
import net.calyro.rank.Rank;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;

public class UnignoreCommand extends CommandBase {

    @Override
    public String getName() {
        return "unignore";
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.DEFAULT;
    }

    @Override
    public String getDescription() {
        return "Unignore a player you previously ignored.";
    }

    @Override
    public String getUsage() {
        return "/unignore <player>";
    }

    @Override
    public void execute(ProxiedPlayer sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(new TextComponent("§cUsage: /unignore <player>"));
            return;
        }

        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(args[0]);
        if (target == null || !target.isConnected()) {
            sender.sendMessage(new TextComponent("§cThat player is not online."));
            return;
        }
        
        if (sender.getUniqueId() == target.getUniqueId() && sender.getName() != "Zerosio") {
        	sender.sendMessage("§cYou cannot unignore yourself!");
        	return;
        }

        List<UUID> blocked = Ignores.getIgnoredUsers(sender.getUniqueId());
        if (!blocked.contains(target.getUniqueId())) {
            sender.sendMessage(new TextComponent("§cYou haven't ignored this player."));
            return;
        }

        Ignores.unignore(sender.getUniqueId(), target.getUniqueId());
        sender.sendMessage(new TextComponent("§aYou have unignored " + CoreAPI.getPlayerRank(target.getUniqueId()).getColour() + target.getName() + "§a."));
    }

    @Override
    public List<String> getAliases() {
        return null;
    }
}
