package com.zerosio.commands;

import com.zerosio.api.ControllerAPI;
import com.zerosio.authentication.AuthDB;
import com.zerosio.authentication.Authentication;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.instance.AvailableInstance;
import com.zerosio.rank.Rank;
import com.zerosio.utility.PremiumUtil;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;

public class LobbyCommand extends CommandBase {

    private static final List<AvailableInstance> LOBBIES = ControllerAPI.getAvailableInstances("lobby");

    @Override
    public String getName() {
        return "lobby";
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.DEFAULT;
    }

    @Override
    public String getDescription() {
        return "Send yourself to a lobby server.";
    }

    @Override
    public String getUsage() {
        return "/lobby";
    }

    @Override
    public void execute(ProxiedPlayer sender, String[] args) {
        if (!Authentication.shouldAutoLogin(sender)) {
        	return;
        }
        
        for (AvailableInstance lobby : LOBBIES) {
            ServerInfo server = ProxyServer.getInstance().getServerInfo(lobby.getName());
            if (server != null && !server.getPlayers().contains(sender)) {
                sender.connect(server);
                //sender.sendMessage(new TextComponent("§aSending you to §e" + lobby + "§a..."));
                return;
            }
        }
        sender.sendMessage(new TextComponent("§cNo available lobby servers right now."));
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("lb");
    }
}
