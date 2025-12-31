package com.zerosio.commands;

import com.zerosio.commands.impl.CommandBase;
import com.zerosio.rank.Rank;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.Collections;
import java.util.List;

public class PingCommand extends CommandBase {

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.DEFAULT;
    }

    @Override
    public String getDescription() {
        return "Check your ping";
    }

    @Override
    public String getUsage() {
        return "/ping";
    }

    @Override
    public void execute(ProxiedPlayer sender, String[] args) {
        int ping = sender.getPing();
        sender.sendMessage(new TextComponent("§aYour ping is §e" + ping + "ms"));
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }
}
