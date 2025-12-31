package com.zerosio.listeners;

import com.zerosio.api.CoreAPI;
import com.zerosio.chat.ChatModes;
import com.zerosio.commands.punishments.PunishmentDomains;
import com.zerosio.database.Punishment;
import com.zerosio.database.User;
import com.zerosio.guilds.Guild;
import com.zerosio.party.database.PartyDB;
import com.zerosio.rank.Rank;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.bson.Document;

public class ChatListener implements Listener {

    @EventHandler
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer))
            return;

        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        User user = User.getUser(player.getUniqueId());
        ChatModes userChatMode = user.getChatMode();
        
        if (userChatMode == null) {
        	userChatMode = ChatModes.PUBLIC;
        }
        
        if (player != null && CoreAPI.getPlayerRank(player.getUniqueId()).isAboveOrEqual(Rank.MVP_PLUS_PLUS)) {
            event.setMessage(event.getMessage()
                    .replace("<3", "§c❤")
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
                    .replace(":typing:", "§e✎§6..."));
        }
        
        if (userChatMode == ChatModes.GUILD) {
        	if(!Guild.inGuild(player)) {
        		player.sendMessage("§eYour chat mode has been set to §2ALL §ebecause you are currently not in a Guild.");
        		user.setChatMode(ChatModes.PUBLIC);
        		event.setCancelled(true);
        		return;
        	}
        	
        	ProxyServer.getInstance().getPluginManager().dispatchCommand(player, "gc " + event.getMessage());
        	event.setCancelled(true);
        	return;
        }
        
        if (userChatMode == ChatModes.PARTY) {
        	if (!PartyDB.isInParty(player.getUniqueId())) {
        		player.sendMessage("§eYour chat mode has been set to §2ALL §ebecause you are currently not in a Party.");
        		user.setChatMode(ChatModes.PUBLIC);
        		event.setCancelled(true);
        		return;
        	}
        	
        	ProxyServer.getInstance().getPluginManager().dispatchCommand(player, "pc " + event.getMessage());
        	event.setCancelled(true);
        	return;
        }
        
        if (userChatMode == ChatModes.MESSAGE) {
        	ProxiedPlayer dming = CoreAPI.getProxyPlayer(user.getCurrentlyMessaging());
        	
        	if (dming == null || !dming.isConnected()) {
        		player.sendMessage("§eYour chat mode has been set to §2ALL §ethe person you were messaging has went offline.");
        		user.setChatMode(ChatModes.PUBLIC);
        		event.setCancelled(true);
        		return;
        	}
        	
        	ProxyServer.getInstance().getPluginManager().dispatchCommand(player, "msg " + dming.getName() + " " + event.getMessage());
        	event.setCancelled(true);
        	return;
        }

        if (Punishment.isMuted(player.getUniqueId())) {
            Document mute = Punishment.getActiveMute(player.getUniqueId());
            if (mute != null) {
                String reason = mute.getString("reason");
                String id = mute.getString("id");
                long length = mute.getLong("length");
                long timestamp = mute.getLong("timestamp");

                if (length != -1) {
                    long expiry = timestamp + (length * 1000L);
                    long timeLeft = (expiry - System.currentTimeMillis()) / 1000L;
                    if (timeLeft <= 0) {
                        Punishment.unmute(player.getUniqueId());
                        return;
                    }
                    player.sendMessage("§c§l§m---------------------------------------------");
                    player.sendMessage("§cYou are currently muted for: " + reason);
                    player.sendMessage("§7Your mute will expire in §c" + calculateTime(timeLeft));
                    player.sendMessage("§7Find out more here: §e" + PunishmentDomains.MUTE);
                    player.sendMessage("§7Mute ID: §f#" + id);
                    player.sendMessage("§c§l§m---------------------------------------------");
                    event.setCancelled(true);
                    return;
                } else {
                    player.sendMessage("§c§l§m---------------------------------------------");
                    player.sendMessage("§cYou are permanently muted for: " + reason);
                    player.sendMessage("§7Find out more here: §e" + PunishmentDomains.MUTE);
                    player.sendMessage("§7Mute ID: §f#" + id);
                    player.sendMessage("§c§l§m---------------------------------------------");
                    event.setCancelled(true);
                    return;
                }
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
