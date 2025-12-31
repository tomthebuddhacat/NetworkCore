package com.zerosio.commands;

import com.zerosio.api.ControllerAPI;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.instance.AvailableInstance;
import com.zerosio.rank.Rank;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.*;
import java.util.Map.Entry;

public class AvailableInstances extends CommandBase {

	@Override
	public String getName() {
		return "availableinstances";
	}

	@Override
	public Rank getRequiredRank() {
		return Rank.OWNER;
	}

	@Override
	public String getDescription() {
		return "List of instances of template types";
	}

	@Override
	public String getUsage() {
		return "/av <template>";
	}

	@Override
	public void execute(ProxiedPlayer sender, String[] args) {

		if (args.length < 1) {
            sender.sendMessage(new TextComponent("§cUsage: ") + getUsage());
            return;
        }
        
        String template = args[0];
        
        List<AvailableInstance> insts = ControllerAPI.getAvailableInstances(template);
        
        sender.sendMessage("§aAvailable instances of type §e" + template + " §aare as follows:");
        
        for (AvailableInstance inst : insts) {
        	sender.sendMessage("§a" + inst.getName() + "  §7[" + inst.getPlayers() + "/" + inst.getMaxPlayers() + "]  §8(" + inst.getAddress() + ":" + inst.getPort() + ")");
        }
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("av", "avs");
	}
}
