package com.zerosio.commands;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Joiner;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.guilds.Guild;
import com.zerosio.rank.Rank;
import com.zerosio.utility.Utilities;

import static com.zerosio.commands.GuildCommand.cooldown;

public class GuildChatCommand extends CommandBase {

    @Override
    public String getName() {
        return "guildchat";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("gc");
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.DEFAULT;
    }

    @Override
    public String getDescription() {
        return "Guild chat";
    }

    @Override
    public String getUsage() {
        return "/gc <message>";
    }

    @Override
    public void execute(ProxiedPlayer player, String[] args) {
        handle(player, args);
    }

    public static void handle(ProxiedPlayer player, String[] args) {
        if (cooldown.containsKey(player.getUniqueId())) {
            if (System.currentTimeMillis() - cooldown.get(player.getUniqueId()) < 1000) {
                player.sendMessage("§cYou are sending commands too fast!");
                return;
            }
        }
        cooldown.put(player.getUniqueId(), System.currentTimeMillis());

        if (!Guild.inGuild(player)) {
            player.sendMessage(getDividerAqua());
            player.sendMessage("§cYou must be in a guild to use this command!");
            player.sendMessage(getDividerAqua());
            return;
        }

        if (args.length == 0) {
            player.sendMessage("§cYou must provide a message to send!");
            return;
        }

        Guild guild = Guild.getGuildFromPlayer(player);

        String roleTag = "";
        if (guild.getLeader().equals(player.getUniqueId().toString())) {
            roleTag = "§" + guild.getTagColor() + "[GM]";
        } else if (guild.getOfficer().contains(player.getUniqueId().toString())) {
            roleTag = "§" + guild.getTagColor() + "[OF]";
        } else if (guild.getMembers().contains(player.getUniqueId().toString())) {
            roleTag = "§" + guild.getTagColor() + "[M]";
        }
        
        final String roleGyat = roleTag;

        String message = Joiner.on(" ").join(args);

        guild.getOnlinePlayers().forEach(onlinePlayer -> {
            onlinePlayer.sendMessage("§2Guild > " + Utilities.getRankFromPlayer(player) + player.getName() + " " + roleGyat + "§f: " + message);
        });
    }

    public static String getDividerAqua() {
        return "§b§m-----------------------------------------------------";
    }
}
