package com.zerosio.commands.impl;

import com.zerosio.Core;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import java.util.Collections;
import java.util.List;

public class CommandManager {

    private static final String COMMAND_PACKAGE = "com.zerosio.commands";

    public static void registerCommands(Core plugin) {
        int registered = 0;

        for (Class<? extends CommandBase> clazz : Reflections.getSubTypesOf(COMMAND_PACKAGE)) {
            try {
                CommandBase commandBase = clazz.getDeclaredConstructor().newInstance();
                register(plugin, commandBase);
                registered++;
            } catch (Exception e) {
                plugin.getLogger().warning("§cFailed to register command: " + clazz.getSimpleName());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("§aSuccessfully registered " + registered + " commands.");
    }

    private static void register(Core plugin, CommandBase commandBase) {
        List<String> aliases = commandBase.getAliases();
        if (aliases == null) {
            aliases = Collections.emptyList();
        }

        Command command = new Command(commandBase.getName(), null,
                aliases.toArray(new String[0])) {
            @Override
            public void execute(CommandSender sender, String[] args) {
                commandBase.executeCommand(sender, args);
            }
        };
        plugin.getProxy().getPluginManager().registerCommand(plugin, command);
    }
}
