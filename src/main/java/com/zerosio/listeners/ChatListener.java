package com.zerosio.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.zerosio.Config;
import com.zerosio.Core;
import com.zerosio.Messages;
import com.zerosio.api.CoreAPI;
import com.zerosio.chat.ChatModes;
import com.zerosio.commands.punishments.PunishmentDomains;
import com.zerosio.database.Punishment;
import com.zerosio.database.User;
import com.zerosio.guilds.Guild;
import com.zerosio.party.database.PartyDB;
import com.zerosio.rank.Rank;
import net.kyori.adventure.text.Component;
import org.bson.Document;

import java.util.List;

public class ChatListener {

    @Subscribe
    public void onChat(PlayerChatEvent playerChatEvent) {
        Player player = playerChatEvent.getPlayer();
        User user = User.getUser(player.getUniqueId());

        ChatModes userChatMode = user.getChatMode();
        
        if (userChatMode == null) {
        	userChatMode = ChatModes.PUBLIC;
        }

        String message = playerChatEvent.getMessage();

        if (player != null && CoreAPI.getPlayerRank(player.getUniqueId()).isAboveOrEqual(Rank.MVP_PLUS_PLUS)) {
            message = message.replace("<3", "§c❤")
                    .replace("⭐", "§6✭")
                    .replace(":owo:", "§dO§5w§dO")
                    .replace("o/", "§d(/◕ヮ◕)/")
                    .replace(":OOF:", "§c§lOOF")
                    .replace(":123:", "§a1§e2§c3")
                    .replace(":shrug:", "§e¯\\(ツ)/¯")
                    .replace(":yes:", "§a✔")
                    .replace(":no:", "§c✖")
                    .replace(":java:", "§b♨")
                    .replace(":arrow:", "§e➡")
                    .replace(":typing:", "§e✎§6...");
        }
        
        if (userChatMode == ChatModes.GUILD) {
        	if(!Guild.inGuild(player)) {
                player.sendMessage(Messages.get("chat-switched-to-all-from-guild"));
        		user.setChatMode(ChatModes.PUBLIC);
                playerChatEvent.setResult(PlayerChatEvent.ChatResult.denied());
                return;
        	}

            Core.getInstance().getProxy().getCommandManager().executeAsync(player, "gc " + playerChatEvent.getMessage());
            playerChatEvent.setResult(PlayerChatEvent.ChatResult.denied());
            return;
        }
        
        if (userChatMode == ChatModes.PARTY) {
        	if (!PartyDB.isInParty(player.getUniqueId())) {
                player.sendMessage(Messages.get("chat-switched-to-all-from-party"));
        		user.setChatMode(ChatModes.PUBLIC);
                playerChatEvent.setResult(PlayerChatEvent.ChatResult.denied());
                return;
        	}

            Core.getInstance().getProxy().getCommandManager().executeAsync(player, "pc " + playerChatEvent.getMessage());
            playerChatEvent.setResult(PlayerChatEvent.ChatResult.denied());
            return;
        }
        
        if (userChatMode == ChatModes.MESSAGE) {
        	Player dming = CoreAPI.getProxyPlayer(user.getCurrentlyMessaging());
        	
        	if (dming == null || !dming.isActive()) {
                player.sendMessage(Messages.get("messaging-player-went-offline"));
        		user.setChatMode(ChatModes.PUBLIC);
                playerChatEvent.setResult(PlayerChatEvent.ChatResult.denied());
                return;
        	}

            Core.getInstance().getProxy().getCommandManager().executeAsync(player, "msg " + playerChatEvent.getMessage());
            playerChatEvent.setResult(PlayerChatEvent.ChatResult.denied());
            return;
        }

        if (Punishment.isMuted(player.getUniqueId())) {
            Document mute = Punishment.getActiveMute(player.getUniqueId());
            if (mute != null) {
                String reason = mute.getString("reason");
                String id = mute.getString("id");
                long length = mute.getLong("length");
                long timestamp = mute.getLong("timestamp");
                List<String> messages;
                String remaining = "";

                if (length == -1) {
                    messages = Config.getStringList("server.punishment-messages.permanently-muted");
                } else {
                    long expiry = timestamp + (length * 1000L);
                    remaining = calculateTime((expiry - System.currentTimeMillis() / 1000L));
                    messages = Config.getStringList("server.punishment-messages.temporarily-muted");
                }

                String msg = String.join("\n", messages).replace("%punishmentReason%", reason)
                        .replace("%punishmentId%", id)
                        .replace("%punishmentUrl%", PunishmentDomains.MUTE)
                        .replace("%remainingPunishmentTime%", remaining);

                player.sendMessage(Component.text(msg));
                playerChatEvent.setResult(PlayerChatEvent.ChatResult.denied());
                return;
            }
        }
    }

    private String calculateTime(long seconds) {
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        if (days > 0) return days + " day(s)";
        if (hours > 0) return hours + " hour(s)";
        if (minutes > 0) return minutes + " minute(s)";
        return seconds + " second(s)";
    }
}
