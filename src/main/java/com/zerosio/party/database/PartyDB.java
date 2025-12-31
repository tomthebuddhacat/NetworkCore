package com.zerosio.party.database;

import java.util.*;

public class PartyDB {

    private static final Map<UUID, UUID> partyLeaders = new HashMap<>(); 
    private static final Map<UUID, Set<UUID>> partyMembers = new HashMap<>(); 
    private static final Map<UUID, UUID> playerParties = new HashMap<>();
    private static final Map<UUID, Set<UUID>> partyInvites = new HashMap<>(); 

    public static UUID createParty(UUID leaderId) {
        UUID partyId = UUID.randomUUID();

        partyLeaders.put(partyId, leaderId);
        partyMembers.put(partyId, new HashSet<>(Collections.singletonList(leaderId)));
        playerParties.put(leaderId, partyId);

        return partyId;
    }

    public static void disbandParty(UUID partyId) {
        Set<UUID> members = partyMembers.remove(partyId);
        if (members != null) {
            for (UUID member : members) {
                playerParties.remove(member);
            }
        }
        partyLeaders.remove(partyId);
    }

    public static void addMember(UUID partyId, UUID player) {
        partyMembers.computeIfAbsent(partyId, k -> new HashSet<>()).add(player);
        playerParties.put(player, partyId);
    }

    public static void removeMember(UUID partyId, UUID player) {
        Set<UUID> members = partyMembers.get(partyId);
        if (members != null) {
            members.remove(player);
        }
        playerParties.remove(player);

        if (Objects.equals(partyLeaders.get(partyId), player)) {
            partyLeaders.remove(partyId);
        }
    }

    public static boolean isMember(UUID player, UUID partyId) {
        return Objects.equals(playerParties.get(player), partyId);
    }

    public static boolean isLeader(UUID player, UUID partyId) {
        return Objects.equals(partyLeaders.get(partyId), player);
    }

    public static void setLeader(UUID partyId, UUID player) {
        if (partyMembers.containsKey(partyId) && partyMembers.get(partyId).contains(player)) {
            partyLeaders.put(partyId, player);
        }
    }

    public static UUID getLeader(UUID partyId) {
        return partyLeaders.get(partyId);
    }

    public static List<UUID> getMembers(UUID partyId) {
        return new ArrayList<>(partyMembers.getOrDefault(partyId, Collections.emptySet()));
    }

    public static void setCurrentParty(UUID playerId, UUID partyId) {
        if (partyId == null) {
            playerParties.remove(playerId);
        } else {
            playerParties.put(playerId, partyId);
        }
    }

    public static UUID getCurrentParty(UUID player) {
        return playerParties.get(player);
    }

    public static void addInvite(UUID player, UUID partyId) {
        partyInvites.computeIfAbsent(player, k -> new HashSet<>()).add(partyId);
    }

    public static void removeInvite(UUID player, UUID partyId) {
        Set<UUID> invites = partyInvites.get(player);
        if (invites != null) {
            invites.remove(partyId);
            if (invites.isEmpty()) {
                partyInvites.remove(player);
            }
        }
    }

    public static List<UUID> getInvites(UUID player) {
        return new ArrayList<>(partyInvites.getOrDefault(player, Collections.emptySet()));
    }

    public static boolean isInParty(UUID playerId) {
        return playerParties.containsKey(playerId);
    }
}
