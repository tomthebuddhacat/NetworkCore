package com.zerosio.commands.impl;

import com.velocitypowered.api.command.SimpleCommand;
import com.zerosio.Core;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

public class CommandManager {

    private static final String COMMAND_PACKAGE = "com.zerosio.commands";

    private static Logger logger;

    public CommandManager(Logger logger) {
        CommandManager.logger = logger;
    }

    public static void registerCommands(Core plugin) {
        int registered = 0;

        for (Class<? extends CommandBase> clazz : Reflections.getSubTypesOf(COMMAND_PACKAGE)) {
            try {
                CommandBase commandBase = clazz.getDeclaredConstructor().newInstance();
                register(plugin, commandBase);
                registered++;
            } catch (Exception e) {
                logger.warn("Failed to register command: " + clazz.getSimpleName());
                e.printStackTrace();
            }
        }

        logger.info("§aSuccessfully registered " + registered + " commands.");
    }

    private static void register(Core plugin, CommandBase commandBase) {
        List<String> aliases = commandBase.getAliases();
        if (aliases == null) {
            aliases = Collections.emptyList();
        }

        SimpleCommand simpleCommand = invocation -> commandBase.executeCommand(invocation.source(), invocation.arguments());

        plugin.getProxy().getCommandManager().register(plugin.getProxy().getCommandManager().metaBuilder(commandBase.getName())
                .aliases(aliases.toArray(new String[0]))
                .build(), simpleCommand);
    }
}
