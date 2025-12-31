package com.zerosio.commands;

import com.zerosio.api.CoreAPI;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.party.Party;
import com.zerosio.rank.Rank;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;
import java.util.List;

public class PartyCommand extends CommandBase {

    @Override
    public String getName() {
        return "party";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("p");
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.DEFAULT;
    }

    @Override
    public String getDescription() {
        return "Manage your party";
    }

    @Override
    public String getUsage() {
        return "/party <invite|join|leave|disband|kick|warp|setleader|kickoffline|chat|list|invites>";
    }

    @Override
    public void execute(ProxiedPlayer player, String[] args) {
        if (args.length == 0) {
            Party.sendHelp(player);
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "invite":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /party invite <player>");
                    return;
                }
                ProxiedPlayer target = CoreAPI.getProxyPlayer(args[1]);
                if (target != null) {
                    Party.handleInvite(player, target);
                } else {
                    player.sendMessage("§cPlayer not found.");
                }
                break;
            case "join":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /party join <player>");
                    return;
                }
                Party.handleJoin(player, args[1]);
                break;
            case "leave":
                Party.handleLeave(player);
                break;
            case "disband":
                Party.handleDisband(player);
                break;
            case "kick":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /party kick <player>");
                    return;
                }
                ProxiedPlayer kickTarget = CoreAPI.getProxyPlayer(args[1]);
                if (kickTarget != null) {
                    Party.handleKick(player, kickTarget);
                } else {
                    player.sendMessage("§cPlayer not found.");
                }
                break;
            case "warp":
                Party.handleWarp(player);
                break;
            case "setleader":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /party setleader <player>");
                    return;
                }
                ProxiedPlayer leaderTarget = CoreAPI.getProxyPlayer(args[1]);
                if (leaderTarget != null) {
                    Party.handleSetLeader(player, leaderTarget);
                } else {
                    player.sendMessage("§cPlayer not found.");
                }
                break;
            case "kickoffline":
                Party.handleKickOffline(player);
                break;
            case "chat":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /party chat <message>");
                    return;
                }
                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                Party.handleChat(player, message);
                break;
            case "list":
                Party.handleList(player);
                break;
            case "invites":
                Party.handleInvites(player);
                break;
            default:
                Party.sendHelp(player);
                break;
        }
    }
}