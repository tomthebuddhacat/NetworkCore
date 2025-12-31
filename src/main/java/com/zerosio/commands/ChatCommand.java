package com.zerosio.commands;

import com.zerosio.chat.ChatModes;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.database.User;
import com.zerosio.guilds.Guild;
import com.zerosio.party.database.PartyDB;
import com.zerosio.privacy.Ignores;
import com.zerosio.rank.Rank;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.ProxyServer;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ChatCommand extends CommandBase {

    @Override
    public String getName() {
        return "chat";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("xhat");
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.DEFAULT;
    }

    @Override
    public String getDescription() {
        return "Toggle between chat modes";
    }

    @Override
    public String getUsage() {
        return "/chat all|guild|party|message <player>";
    }

    @Override
    public void execute(ProxiedPlayer player, String[] args) {
        User user = User.getUser(player.getUniqueId());

        if (args.length < 1) {
            player.sendMessage("§cUsage: " + getUsage());
            return;
        }

        String mode = args[0].toLowerCase();

        switch (mode) {
            case "a":
            case "all":
                user.setChatMode(ChatModes.PUBLIC);
                player.sendMessage("§aChat mode set to §6§lALL§a.");
                break;
                
            case "g":
            case "guild":
                if (!Guild.inGuild(player)) {
                	player.sendMessage("§cYou have to be in a guild to set your active chat to guild.");
                	return;
                }
                user.setChatMode(ChatModes.GUILD);
                player.sendMessage("§aChat mode set to §2§lGUILD§a.");
                break;
                
            case "p":
            case "party":
                if (!PartyDB.isInParty(player.getUniqueId())) {
                	player.sendMessage("§cYou have to be in a party to set your active chat to party.");
                	return;
                }
            
                user.setChatMode(ChatModes.PARTY);
                player.sendMessage("§aChat mode set to §9§lPARTY§a.");
                break;
                
            case "m":
            case "msg":
            case "message":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /chat message <player>");
                    return;
                }

                String targetName = args[1];
                ProxiedPlayer target = ProxyServer.getInstance().getPlayer(targetName);

                if (target == null) {
                    player.sendMessage("§cPlayer not found with name '" + targetName + "'");
                    return;
                }
                
                if (Ignores.getIgnoredUsers(player.getUniqueId()).contains(target.getUniqueId()) || Ignores.getIgnoredUsers(target.getUniqueId()).contains(player.getUniqueId())) {
                	player.sendMessage("§cThis player has ignored you or you have ignored this player. Try using '/unignore <player>' if you think this is wrong.");
                	return;
                }
                
                user.setCurrentlyMessaging(target.getName());
                user.setChatMode(ChatModes.MESSAGE);
                player.sendMessage("§aChat mode set to §d§lMESSAGE §awith §e" + target.getName());
                break;

            default:
                player.sendMessage("§cInvalid chat mode. Usage: " + getUsage());
        }
    }
}
