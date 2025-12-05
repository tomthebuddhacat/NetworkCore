package net.calyro.commands.punishments;

import net.calyro.commands.impl.CommandBase;
import net.calyro.rank.Rank;
import net.calyro.database.Punishment;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;
import java.util.List;

public class UnmuteCommand extends CommandBase {

    @Override
    public String getName() {
        return "unmute";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("unmote");
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.HELPER;
    }

    @Override
    public String getDescription() {
        return "Unmute a muted player.";
    }

    @Override
    public String getUsage() {
        return "§cUsage: /unmute <name>";
    }

    @Override
    public void execute(ProxiedPlayer player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(new TextComponent(getUsage()));
            return;
        }

        String targetName = args[0];
        
        try {
        Punishment.unmuteByName(targetName);
        player.sendMessage(new TextComponent("§aUnmuted §e" + targetName));
        } catch (Exception e) {
        	player.sendMessage(new TextComponent("§cFailed to identify player named '" + targetName + "'"));
        }
    }
}
