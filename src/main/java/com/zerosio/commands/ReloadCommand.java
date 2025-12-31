package com.zerosio.commands;

import com.zerosio.Config;
import com.zerosio.Core;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.rank.Rank;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;
import java.util.List;

public class ReloadCommand extends CommandBase {

    @Override
    public String getName() {
        return "networkreload";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("nwreload", "ncreload");
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }

    @Override
    public String getDescription() {
        return "Reload the network core plugin configuration";
    }

    @Override
    public String getUsage() {
        return "/networkreload";
    }

    @Override
    public void execute(ProxiedPlayer sender, String[] args) {
        Config.reload();

        Core.getMotdListener().refresh();

        sender.sendMessage(new TextComponent("§aNetwork Core configuration reloaded successfully!"));
    }
}