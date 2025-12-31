package com.zerosio.commands;

import com.zerosio.commands.impl.CommandBase;
import com.zerosio.rank.Rank;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.*;

public class CreateServerCommand extends CommandBase {

    @Override
    public String getName() {
        return "createinstance";
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.DEFAULT;
    }

    @Override
    public String getDescription() {
        return "Create an instance of any type";
    }

    @Override
    public String getUsage() {
        return "/cs <template>";
    }

    @Override
    public void execute(ProxiedPlayer sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(new TextComponent("§cUsage: ") + getUsage());
            return;
        }
        
        String template = args[0];
        
        // TODO will do later
        //ControllerAPI.
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("cs");
    }
}
