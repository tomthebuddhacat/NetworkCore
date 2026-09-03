package com.zerosio.party;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.zerosio.Core;
import com.zerosio.api.CoreAPI;
import com.zerosio.database.User;
import com.zerosio.party.database.PartyDB;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.UUID;

public class ProxiedParty {

    private final UUID partyId;

    public ProxiedParty(UUID partyId) {
        this.partyId = partyId;
    }

    public UUID getPartyId() {
        return partyId;
    }

    public UUID getLeader() {
        return PartyDB.getLeader(partyId);
    }

    public List<UUID> getMembers() {
        return PartyDB.getMembers(partyId);
    }

    public void broadcast(String message) {
        for (UUID member : getMembers()) {
            Player player = Core.getInstance().getProxy().getPlayer(member).orElse(null);
            if (player != null) {
                player.sendMessage(Component.text(message));
            }
        }
    }

    public void addMember(UUID player) {
        PartyDB.addMember(partyId, player);
    }

    public void removeMember(UUID player) {
        PartyDB.removeMember(partyId, player);
    }

    public void disband() {
        PartyDB.disbandParty(partyId);
    }

    public void warp(ServerInfo server) {
        for (UUID member : getMembers()) {
            Player player = Core.getInstance().getProxy().getPlayer(member).orElse(null);
            if (player != null && player.isConnected()) {
                player.connect(server);
            }
        }
    }

    public int kickOffline() {
        int count = 0;
        for (UUID member : getMembers()) {
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(member);
            if (player == null || !player.isConnected()) {
                removeMember(member);
                count++;
            }
        }
        return count;
    }

    public void chat(Player sender, String message) {
        String formatted = "§9Party > " + CoreAPI.getPlayerRank(sender.getUniqueId()).getPrefix() +
                sender.getUsername() + "§f: §f" + message;
        broadcast(formatted);
    }

    public void sendPartyList(Player requester) {
        Party.sendDivider(requester);

        List<UUID> members = getMembers();
        int memberCount = members.size();

        requester.sendMessage(Component.text("§eParty Members §6(" + memberCount + ")");
        requester.sendMessage(Component.text(""));

        UUID leaderId = getLeader();
        String leaderName = User.retrieveLastKnownName(leaderId);

        requester.sendMessage("§eParty Leader: " + CoreAPI.getPlayerRank(leaderId).getPrefix() + leaderName);

        StringBuilder membersList = new StringBuilder("§eParty Members: ");
        for (int i = 0; i < members.size(); i++) {
            UUID memberId = members.get(i);
            if (!memberId.equals(leaderId)) {
                String memberName = User.retrieveLastKnownName(memberId);
                membersList.append(CoreAPI.getPlayerRank(memberId).getPrefix()).append(memberName);

                if (i < members.size() - 1) {
                    membersList.append("§e, ");
                }
            }
        }

        requester.sendMessage(membersList.toString());
        Party.sendDivider(requester);
    }
}