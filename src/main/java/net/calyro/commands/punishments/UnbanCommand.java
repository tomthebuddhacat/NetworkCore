package net.calyro.commands.punishments;

import net.calyro.commands.impl.CommandBase;
import net.calyro.rank.Rank;
import net.calyro.database.Punishment;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Collections;

public class UnbanCommand extends CommandBase {

    @Override
    public String getName() {
        return "unban";
    }

    @Override
    public java.util.List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.MOD;
    }

    @Override
    public String getDescription() {
        return "Unban a player";
    }

    @Override
    public String getUsage() {
        return "/unban <name>";
    }

    @Override
    public void execute(ProxiedPlayer sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(new TextComponent("§cInvalid syntax. Correct: /unban <name>"));
            return;
        }

        String targetName = args[0];

        try {
        Punishment.unbanByName(targetName);
        sender.sendMessage(new TextComponent("§aUnbanned §e" + targetName));
        } catch (Exception e) {
        	sender.sendMessage(new TextComponent("§cFailed to identify player named '" + targetName + "'"));
        }
    }
}
