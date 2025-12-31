package com.zerosio.party;

import com.zerosio.Core;
import com.zerosio.api.CoreAPI;
import com.zerosio.database.User;
import com.zerosio.party.database.PartyDB;
import com.zerosio.privacy.Ignores;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Party {

    private static final Map<UUID, Map<UUID, Long>> pendingInvites = new HashMap<>();
    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final Map<UUID, ScheduledTask> disbandTasks = new HashMap<>();

    private static final long INVITE_EXPIRY = 5 * 60 * 1000L;
    private static final long INVITE_COOLDOWN = 3 * 1000L;
    private static final long DISBAND_DELAY = 5 * 60 * 1000L; // 5 minutes

    public static void sendHelp(ProxiedPlayer player) {
        sendDivider(player);
        player.sendMessage("§eAvailable commands:");
        sendPartyMsg(player, "party join <player>", "Join a public party");
        sendPartyMsg(player, "party list", "Lists the players in your current party");
        sendPartyMsg(player, "party invites", "View invitations to party");
        sendPartyMsg(player, "party kick <player>", "Remove a player from your party");
        sendPartyMsg(player, "party leave", "Leave your current party");
        sendPartyMsg(player, "party setleader <player>", "Transfers the party to another player");
        sendPartyMsg(player, "party warp", "Warps the members to a party to your current server");
        sendPartyMsg(player, "party disband", "Disbands the party");
        sendPartyMsg(player, "party kickoffline", "Remove all offline players from your party");
        sendPartyMsg(player, "party chat <message>", "Send a chat message to the entire party");
        sendPartyMsg(player, "party invite <player>", "Invite another player to party");
        sendDivider(player);
    }

    public static void handleInvite(ProxiedPlayer inviter, ProxiedPlayer target) {
        UUID inviterId = inviter.getUniqueId();
        UUID targetId = target.getUniqueId();
        long now = System.currentTimeMillis();

        if (inviterId.equals(targetId)) {
            inviter.sendMessage("§cYou cannot invite yourself!");
            return;
        }

        if (cooldowns.containsKey(inviterId)) {
            long lastSent = cooldowns.get(inviterId);
            if (now - lastSent < INVITE_COOLDOWN) {
                inviter.sendMessage("§cPlease wait before sending another party invite.");
                return;
            }
        }

        if (!target.isConnected() || target == null) {
            inviter.sendMessage("§cPlayer not found!");
            return;
        }

        UUID currentParty = PartyDB.getCurrentParty(inviterId);
        if (currentParty == null) {
            currentParty = PartyDB.createParty(inviterId);
            cancelDisbandTask(currentParty);
        }

        if (!PartyDB.isLeader(inviterId, currentParty)) {
            inviter.sendMessage("§cOnly the party leader can invite players!");
            return;
        }

        if (PartyDB.getCurrentParty(targetId) != null) {
            inviter.sendMessage("§cThis player is already in a party!");
            return;
        }
        
        if (Ignores.getIgnoredUsers(target.getUniqueId()).contains(inviter.getUniqueId())) {
        	inviter.sendMessage("§cYou've been ignored by this player.");
        	return;
        }

        if (pendingInvites.containsKey(targetId) &&
                pendingInvites.get(targetId).containsKey(inviterId)) {
            long sentTime = pendingInvites.get(targetId).get(inviterId);
            if (now - sentTime < INVITE_EXPIRY) {
                inviter.sendMessage("§cYou've already sent a party invite to this player.");
                return;
            }
        }

        pendingInvites.computeIfAbsent(targetId, k -> new HashMap<>()).put(inviterId, now);
        cooldowns.put(inviterId, now);

        sendDivider(inviter);
        inviter.sendMessage("§eYou invited " + CoreAPI.getPlayerRank(targetId).getPrefix() + target.getName()
                + "§e to your party!");
        sendDivider(inviter);

        sendDivider(target);
        target.sendMessage(
                "§eParty invite from " + CoreAPI.getPlayerRank(inviterId).getPrefix() + inviter.getName() + "§e!");

        BaseComponent accept = new TextComponent("§a§l[ACCEPT]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party join " + inviter.getName()));
        accept.setHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§bClick to accept the party invite")));

        BaseComponent deny = new TextComponent("§c§l[DENY]");
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party deny " + inviter.getName()));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§bClick to deny the party invite")));

        BaseComponent[] full = new ComponentBuilder("")
                .append(accept).append(" §8- ")
                .append(deny)
                .create();

        target.sendMessage(full);
        sendDivider(target);

        ProxyServer.getInstance().getScheduler().schedule(Core.getInstance(), () -> {
            if (hasPendingInvite(inviterId, targetId)) {
                clearInvite(inviterId, targetId);

                ProxiedPlayer inviterOnline = ProxyServer.getInstance().getPlayer(inviterId);
                ProxiedPlayer targetOnline = ProxyServer.getInstance().getPlayer(targetId);

                if (inviterOnline != null && inviterOnline.isConnected()) {
                    sendDivider(inviterOnline);
                    inviterOnline.sendMessage("§c" + CoreAPI.getPlayerRank(targetId).getPrefix() + target.getName()
                            + " did not respond to your party request.");
                    sendDivider(inviterOnline);
                }

                if (targetOnline != null && targetOnline.isConnected()) {
                    sendDivider(targetOnline);
                    targetOnline.sendMessage("§cThe party invite from " + CoreAPI.getPlayerRank(inviterId).getPrefix()
                            + inviter.getName() + " has expired.");
                    sendDivider(targetOnline);
                }
            }
        }, INVITE_EXPIRY, TimeUnit.MILLISECONDS);
    }

    public static void handleJoin(ProxiedPlayer player, String inviterName) {
        UUID playerId = player.getUniqueId();

        ProxiedPlayer inviter = ProxyServer.getInstance().getPlayer(inviterName);
        if (inviter == null || !inviter.isConnected()) {
            player.sendMessage("§cPlayer not found!");
            return;
        }

        UUID inviterId = inviter.getUniqueId();
        UUID partyId = PartyDB.getCurrentParty(inviterId);

        if (partyId == null) {
            player.sendMessage("§cThis player is not in a party!");
            return;
        }

        if (hasPendingInvite(inviterId, playerId)) {
            clearInvite(inviterId, playerId);
            PartyDB.addMember(partyId, playerId);
            PartyDB.setCurrentParty(playerId, partyId);
            cancelDisbandTask(partyId);

            sendDivider(player);
            player.sendMessage("§aYou joined the party!");
            sendDivider(player);

            ProxiedParty party = new ProxiedParty(partyId);
            party.broadcast(getDivider());
            party.broadcast(
                    "§e" + CoreAPI.getPlayerRank(playerId).getPrefix() + player.getName() + " joined the party.");
                   party.broadcast(getDivider()); 
        } else {
            player.sendMessage(getDivider());
            player.sendMessage("§cYou don't have a pending invite from this party!");
            player.sendMessage(getDivider());
        }
    }

    public static void handleLeave(ProxiedPlayer player) {
        UUID playerId = player.getUniqueId();
        UUID partyId = PartyDB.getCurrentParty(playerId);

        if (partyId == null) {
            player.sendMessage("§cYou are not in a party!");
            return;
        }

        ProxiedParty party = new ProxiedParty(partyId);
        boolean wasLeader = PartyDB.isLeader(playerId, partyId);

        party.removeMember(playerId);
        PartyDB.setCurrentParty(playerId, null);

        sendDivider(player);
        player.sendMessage("§aYou left the party!");
        sendDivider(player);
        
        party.broadcast(getDivider());
        party.broadcast("§c" + CoreAPI.getPlayerRank(playerId).getPrefix() + player.getName() + " left the party!");
        party.broadcast(getDivider());

        if (wasLeader) {
            List<UUID> members = PartyDB.getMembers(partyId);
            if (members.isEmpty()) {
                handleDisband(player, true);
            } else {
                UUID newLeader = members.get(0);
                PartyDB.setLeader(partyId, newLeader);
                ProxiedPlayer newLeaderPlayer = ProxyServer.getInstance().getPlayer(newLeader);
                if (newLeaderPlayer != null) {
                    party.broadcast(getDivider());
                    party.broadcast("§aParty leadership transferred to " + CoreAPI.getPlayerRank(newLeader).getPrefix()
                            + newLeaderPlayer.getName() + "!");
                            party.broadcast(getDivider());
                }
            }
        }

        if (PartyDB.getMembers(partyId).size() == 1) {
            scheduleDisbandTask(partyId);
        }
    }

    public static void handleDisband(ProxiedPlayer player) {
        handleDisband(player, false);
    }

    public static void handleDisband(ProxiedPlayer player, boolean silent) {
        UUID playerId = player.getUniqueId();
        UUID partyId = PartyDB.getCurrentParty(playerId);

        if (partyId == null) {
            if (!silent)
                player.sendMessage("§cYou are not in a party!");
            return;
        }

        if (!PartyDB.isLeader(playerId, partyId)) {
            if (!silent)
                player.sendMessage("§cOnly the party leader can disband the party!");
            return;
        }

        ProxiedParty party = new ProxiedParty(partyId);
        if (!silent) {
            sendDivider(player);
            player.sendMessage("§aParty disbanded!");
            sendDivider(player);
            
            party.broadcast(getDivider());
            party.broadcast("§c" + CoreAPI.getPlayerRank(playerId).getPrefix() + player.getName()
                    + " has disbanded the party!");
            party.broadcast(getDivider());
        }

        party.disband();
        cancelDisbandTask(partyId);
    }

    public static void handleKick(ProxiedPlayer player, ProxiedPlayer target) {
        UUID playerId = player.getUniqueId();
        UUID targetId = target.getUniqueId();
        UUID partyId = PartyDB.getCurrentParty(playerId);

        if (partyId == null) {
            player.sendMessage("§cYou are not in a party!");
            return;
        }
        
        if (playerId == targetId) {
        	player.sendMessage("§cYou cannot kick yourself from the party!");
        	return;
        }

        if (!PartyDB.isLeader(playerId, partyId)) {
            player.sendMessage("§cOnly the party leader can kick players!");
            return;
        }

        if (!PartyDB.isMember(targetId, partyId)) {
            player.sendMessage("§cThis player is not in your party!");
            return;
        }

        ProxiedParty party = new ProxiedParty(partyId);
        party.removeMember(targetId);
        PartyDB.setCurrentParty(targetId, null);

        sendDivider(player);
        player.sendMessage(
                "§aKicked " + CoreAPI.getPlayerRank(targetId).getPrefix() + target.getName() + " from the party!");
        sendDivider(player);

        sendDivider(target);
        target.sendMessage("§cYou were kicked from the party!");
        sendDivider(target);
        
        party.broadcast(getDivider());
        party.broadcast(
                "§c" + CoreAPI.getPlayerRank(targetId).getPrefix() + target.getName() + " was kicked from the party!");
                party.broadcast(getDivider());

        if (PartyDB.getMembers(partyId).size() == 1) {
            scheduleDisbandTask(partyId);
        }
    }

    public static void handleWarp(ProxiedPlayer player) {
        UUID playerId = player.getUniqueId();
        UUID partyId = PartyDB.getCurrentParty(playerId);

        if (partyId == null) {
            player.sendMessage("§cYou are not in a party!");
            return;
        }

        if (!PartyDB.isLeader(playerId, partyId)) {
            player.sendMessage("§cOnly the party leader can warp players!");
            return;
        }

        ProxiedParty party = new ProxiedParty(partyId);
        party.warp(player.getServer().getInfo());
        party.broadcast(getDivider());
        party.broadcast("§aParty warping to " + player.getServer().getInfo().getName() + "!");
        party.broadcast(getDivider());
    }

    public static void handleSetLeader(ProxiedPlayer player, ProxiedPlayer target) {
        UUID playerId = player.getUniqueId();
        UUID targetId = target.getUniqueId();
        UUID partyId = PartyDB.getCurrentParty(playerId);

        if (partyId == null) {
            player.sendMessage("§cYou are not in a party!");
            return;
        }

        if (!PartyDB.isLeader(playerId, partyId)) {
            player.sendMessage("§cOnly the party leader can transfer leadership!");
            return;
        }

        if (!PartyDB.isMember(targetId, partyId)) {
            player.sendMessage("§cThis player is not in your party!");
            return;
        }

        PartyDB.setLeader(partyId, targetId);
        ProxiedParty party = new ProxiedParty(partyId);
        party.broadcast(getDivider());
        party.broadcast("§aParty leadership transferred to " + CoreAPI.getPlayerRank(targetId).getPrefix()
                + target.getName() + "!");
                party.broadcast(getDivider());
    }

    public static void handleKickOffline(ProxiedPlayer player) {
        UUID playerId = player.getUniqueId();
        UUID partyId = PartyDB.getCurrentParty(playerId);

        if (partyId == null) {
            player.sendMessage("§cYou are not in a party!");
            return;
        }

        if (!PartyDB.isLeader(playerId, partyId)) {
            player.sendMessage("§cOnly the party leader can kick offline players!");
            return;
        }

        ProxiedParty party = new ProxiedParty(partyId);
        int kicked = party.kickOffline();
        player.sendMessage("§aKicked " + kicked + " offline players from the party!");

        if (PartyDB.getMembers(partyId).size() == 1) {
            scheduleDisbandTask(partyId);
        }
    }

    public static void handleChat(ProxiedPlayer player, String message) {
        UUID playerId = player.getUniqueId();
        UUID partyId = PartyDB.getCurrentParty(playerId);

        if (partyId == null) {
            player.sendMessage("§cYou are not in a party!");
            return;
        }

        ProxiedParty party = new ProxiedParty(partyId);
        party.chat(player, message);
    }

    public static void handleList(ProxiedPlayer player) {
        UUID playerId = player.getUniqueId();
        UUID partyId = PartyDB.getCurrentParty(playerId);

        if (partyId == null) {
            player.sendMessage("§cYou are not in a party!");
            return;
        }

        ProxiedParty party = new ProxiedParty(partyId);
        party.sendPartyList(player);
    }

    public static void handleInvites(ProxiedPlayer player) {
        List<UUID> invites = PartyDB.getInvites(player.getUniqueId());

        if (invites.isEmpty()) {
            player.sendMessage("§cYou have no pending party invites!");
            return;
        }

        sendDivider(player);
        player.sendMessage("§aYour Party Invites:");
        for (UUID inviterId : invites) {
            String inviterName = User.retrieveLastKnownName(inviterId);
            player.sendMessage("§7- " + CoreAPI.getPlayerRank(inviterId).getPrefix() + inviterName);
        }
        sendDivider(player);
    }

    public static boolean hasPendingInvite(UUID from, UUID to) {
        if (!pendingInvites.containsKey(to))
            return false;
        Long sent = pendingInvites.get(to).get(from);
        return sent != null && System.currentTimeMillis() - sent < INVITE_EXPIRY;
    }

    public static void clearInvite(UUID from, UUID to) {
        if (pendingInvites.containsKey(to)) {
            pendingInvites.get(to).remove(from);
            if (pendingInvites.get(to).isEmpty()) {
                pendingInvites.remove(to);
            }
        }
    }

    public static void scheduleDisbandTask(UUID partyId) {
        cancelDisbandTask(partyId);

        ScheduledTask task = ProxyServer.getInstance().getScheduler().schedule(Core.getInstance(), () -> {
            List<UUID> members = PartyDB.getMembers(partyId);
            if (members.size() == 1) {
                UUID lastMember = members.get(0);
                ProxiedPlayer player = ProxyServer.getInstance().getPlayer(lastMember);
                if (player != null) {
                    handleDisband(player, true);
                    player.sendMessage("§cYour party was disbanded because you were the only member online.");
                } else {
                    ProxiedParty party = new ProxiedParty(partyId);
                    party.disband();
                }
            }
            disbandTasks.remove(partyId);
        }, DISBAND_DELAY, TimeUnit.MILLISECONDS);

        disbandTasks.put(partyId, task);
    }

    public static void cancelDisbandTask(UUID partyId) {
        ScheduledTask task = disbandTasks.get(partyId);
        if (task != null) {
            task.cancel();
            disbandTasks.remove(partyId);
        }
    }

    public static String getDivider() {
        return "§9§m-----------------------------------------------------";
    }

    public static void sendDivider(ProxiedPlayer player) {
        player.sendMessage(getDivider());
    }

    public static void sendPartyMsg(ProxiedPlayer player, String command, String description) {
        TextComponent cmdComponent = new TextComponent("§e/" + command);
        cmdComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + command));
        cmdComponent.setHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7Click to put the command in chat")));

        BaseComponent[] finalMessage = new ComponentBuilder("")
                .append(cmdComponent)
                .append("§8 - §b" + description)
                .create();

        player.sendMessage(finalMessage);
    }
}