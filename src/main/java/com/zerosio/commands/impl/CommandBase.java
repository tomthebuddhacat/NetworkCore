package com.zerosio.commands.impl;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.zerosio.Messages;
import com.zerosio.api.CoreAPI;
import com.zerosio.rank.Rank;
import net.kyori.adventure.text.Component;

import java.util.List;

public abstract class CommandBase {

    public abstract String getName();

    public abstract List<String> getAliases();

    public abstract Rank getRequiredRank();

    public abstract String getDescription();

    public abstract String getUsage();

    public abstract void execute(Player player, String[] args);

    public void executeCommand(CommandSource commandSource, String[] args) {
        if (!consoleCommand() && !(commandSource instanceof Player)) {
            commandSource.sendMessage(Messages.get("only-players-can-execute"));
            return;
        }

        Player player = commandSource instanceof Player ? (Player) commandSource : null;

        if (!consoleCommand() && player != null) {
            Rank rank = getPlayerRank(player);
            if (!rank.isAboveOrEqual(getRequiredRank())) {
                player.sendMessage(Messages.get("must-have-required-rank-or-higher").replaceText(builder -> builder
                        .match("%requiredRank%")
                        .replacement(Component.text(getRequiredRank().getPrefixColoured()))));
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

    private Rank getPlayerRank(Player player) {
        return CoreAPI.getPlayerRank(player.getUniqueId());
    }

    public boolean consoleCommand() {
        return false;
    }
}
