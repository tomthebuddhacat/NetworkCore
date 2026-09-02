package com.zerosio.commands;

import com.velocitypowered.api.proxy.Player;
import com.zerosio.Messages;
import com.zerosio.api.ControllerAPI;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.instance.AvailableInstance;
import com.zerosio.rank.Rank;
import net.kyori.adventure.text.Component;

import java.util.*;

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
	public void execute(Player player, String[] args) {

		if (args.length < 1) {
            player.sendMessage(Messages.get("available-instances-command-usage").replaceText(builder -> builder
					.match("%availableInstancesCommandUsage%")
					.replacement(Component.text(getUsage()))));
            return;
        }
        
        String template = args[0];
        
        List<AvailableInstance> insts = ControllerAPI.getAvailableInstances(template);

		StringBuilder stringBuilder = new StringBuilder();

		for (AvailableInstance availableInstance : insts) {
			stringBuilder.append(Messages.getString("available-instance")
							.replace("%instanceName%", availableInstance.getName())
							.replace("%instanceOnlinePlayerCount%", String.valueOf(availableInstance.getPlayers()))
							.replace("%instanceMaxPlayerCount%", String.valueOf(availableInstance.getMaxPlayers()))
							.replace("%instanceConnectionAddress%", availableInstance.getAddress())
							.replace("%instanceConnectionPort%", String.valueOf(availableInstance.getPort())))
					.append("\n");
		}

		// The code below was written for JDK 9+ (future reference)
		// player.sendMessage(Messages.get("available-instances", Map.of("instanceTemplate", template, "%availableInstances%", stringBuilder.toString())));

		// This should work on JDK 8 and future versions, hopefully
		Map<String, String> messagePlaceholders = new HashMap<>();
		messagePlaceholders.put("instanceTemplate", template);
		messagePlaceholders.put("availableInstances", stringBuilder.toString());

		player.sendMessage(Messages.get("available-instances", messagePlaceholders));
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("av", "avs");
	}
}
