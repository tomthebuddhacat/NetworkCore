package com.zerosio.commands;

import com.velocitypowered.api.proxy.Player;
import com.zerosio.Core;
import com.zerosio.Messages;
import com.zerosio.chat.ChatModes;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.database.User;
import com.zerosio.guilds.Guild;
import com.zerosio.party.database.PartyDB;
import com.zerosio.privacy.Ignores;
import com.zerosio.rank.Rank;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public void execute(Player player, String[] args) {
        User user = User.getUser(player.getUniqueId());

        if (args.length < 1) {
            player.sendMessage(Messages.get("chat-command-usage").replaceText(builder -> builder
                    .match("%chatCommandUsage%")
                    .replacement(Component.text(getUsage()))));
            return;
        }

        String mode = args[0].toLowerCase();
        String modifiedChatMode = "";

        switch (mode) {
            case "a":
            case "all":
                user.setChatMode(ChatModes.PUBLIC);
                modifiedChatMode = Messages.getString("chat-mode-color.all");
                break;
                
            case "g":
            case "guild":
                if (!Guild.inGuild(player)) {
                    player.sendMessage(Messages.get("player-must-be-in-a-guild"));
                	return;
                }
                user.setChatMode(ChatModes.GUILD);
                modifiedChatMode = Messages.getString("chat-mode-color.guild");
                break;
                
            case "p":
            case "party":
                if (!PartyDB.isInParty(player.getUniqueId())) {
                    player.sendMessage(Messages.get("player-must-be-in-a-party"));
                    return;
                }
            
                user.setChatMode(ChatModes.PARTY);
                modifiedChatMode = Messages.getString("chat-mode-color.party");
                break;
                
            case "m":
            case "msg":
            case "message":
                if (args.length < 2) {
                    player.sendMessage(Messages.get("insufficient-message-command-args"));
                    return;
                }

                String targetName = args[1];
                Player target = Core.getInstance().getProxy().getPlayer(targetName).orElse(null);
                
                if (target == null) {
                    player.sendMessage(Messages.get("message-command-player-not-found").replaceText(builder -> builder
                            .match("%targetPlayerName%")
                            .replacement(targetName)));
                    return;
                }
                
                if (Ignores.getIgnoredUsers(player.getUniqueId()).contains(target.getUniqueId()) || Ignores.getIgnoredUsers(target.getUniqueId()).contains(player.getUniqueId())) {
                    player.sendMessage(Messages.get("message-command-this-player-has-ignored-you"));
                	return;
                }
                
                user.setCurrentlyMessaging(target.getUsername());
                user.setChatMode(ChatModes.MESSAGE);
                modifiedChatMode = Messages.getString("chat-mode-color.message");
                break;

            default:
                player.sendMessage(Messages.get("message-command-usage").replaceText(builder -> builder
                        .match("%messageCommandUsage%")
                        .replacement(Component.text(getUsage()))));
                return;
        }

        // player.sendMessage(Messages.get("available-instances", Map.of("instanceTemplate", template, "%availableInstances%", stringBuilder.toString())));

        Map<String, String> messagePlaceholders = new HashMap<>();
        messagePlaceholders.put("chatMode", modifiedChatMode);

        player.sendMessage(Messages.get("chat-mode-set", messagePlaceholders));
    }
}
