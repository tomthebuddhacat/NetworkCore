package net.calyro.commands;

import net.calyro.commands.impl.CommandBase;
import net.calyro.party.Party;
import net.calyro.rank.Rank;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;
import java.util.List;

public class PartyChatCommand extends CommandBase {

    @Override
    public String getName() {
        return "partychat";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("pc");
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.DEFAULT;
    }

    @Override
    public String getDescription() {
        return "Send a message to your party";
    }

    @Override
    public String getUsage() {
        return "/pc <message>";
    }

    @Override
    public void execute(ProxiedPlayer player, String[] args) {
        if (args.length == 0) {
            player.sendMessage("§cUsage: /pc <message>");
            return;
        }

        String message = String.join(" ", args);
        Party.handleChat(player, message);
    }
}