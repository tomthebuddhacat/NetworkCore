package net.calyro.party;

import net.calyro.api.CoreAPI;
import net.calyro.database.User;
import net.calyro.party.database.PartyDB;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

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
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(member);
            if (player != null) {
                player.sendMessage(message);
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
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(member);
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

    public void chat(ProxiedPlayer sender, String message) {
        String formatted = "§9Party > " + CoreAPI.getPlayerRank(sender.getUniqueId()).getPrefix() +
                sender.getName() + "§f: §f" + message;
        broadcast(formatted);
    }

    public void sendPartyList(ProxiedPlayer requester) {
        Party.sendDivider(requester);

        List<UUID> members = getMembers();
        int memberCount = members.size();

        requester.sendMessage("§eParty Members §6(" + memberCount + ")");
        requester.sendMessage("");

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