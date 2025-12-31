package com.zerosio.commands.impl;

import com.zerosio.api.CoreAPI;
import com.zerosio.rank.Rank;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;

public abstract class CommandBase {

    public abstract String getName();

    public abstract List<String> getAliases();

    public abstract Rank getRequiredRank();

    public abstract String getDescription();

    public abstract String getUsage();

    public abstract void execute(ProxiedPlayer sender, String[] args);

    public void executeCommand(CommandSender sender, String[] args) {
        if (!consoleCommand() && !(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent("§cOnly players can use this command."));
            return;
        }

        ProxiedPlayer player = sender instanceof ProxiedPlayer ? (ProxiedPlayer) sender : null;

        if (!consoleCommand() && player != null) {
            Rank rank = getPlayerRank(player);
            if (!rank.isAboveOrEqual(getRequiredRank())) {
                player.sendMessage(new TextComponent(
                        "§cYou need " + getRequiredRank().getPrefixColoured() + "§c or higher to use this command."));
                return;
            }
        }

        if (player == null) {
            if (args.length > 0) {
                System.out.println("[COMMANDS] Console executed command: " + getName());
            }
            return;
        }

        execute(player, args);
    }

    private Rank getPlayerRank(ProxiedPlayer player) {
        return CoreAPI.getPlayerRank(player.getUniqueId());
    }

    public boolean consoleCommand() {
        return false;
    }
}