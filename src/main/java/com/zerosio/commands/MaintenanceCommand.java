package com.zerosio.commands;

import com.zerosio.Config;
import com.zerosio.Core;
import com.zerosio.api.CoreAPI;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.rank.Rank;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;
import java.util.List;

public class MaintenanceCommand extends CommandBase {

    @Override
    public String getName() {
        return "maintenance";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("maintenancemode", "mtm");
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }

    @Override
    public String getDescription() {
        return "Toggle maintenance mode on the network.";
    }

    @Override
    public String getUsage() {
        return "/maintenance <true|false>";
    }

    @Override
    public void execute(ProxiedPlayer player, String[] args) {
        if (args.length != 1) {
            player.sendMessage(new TextComponent("§cUsage: " + getUsage()));
            return;
        }

        String arg = args[0].toLowerCase();
        boolean enable;

        if (arg.equals("true")) {
            enable = true;

            for (ProxiedPlayer nerds : ProxyServer.getInstance().getPlayers()) {
                if (!CoreAPI.getPlayerRank(nerds.getUniqueId()).isStaff()) {
                    nerds.disconnect(
                            "§cWe are sorry but the ShardMC network is currently down for maintenance.\n§cFor more information: §bshardmc.net");
                }
            }
        } else if (arg.equals("false")) {
            enable = false;
        } else {
            player.sendMessage(new TextComponent("§cUsage: " + getUsage()));
            return;
        }

        Config.set("maintenance", enable);
        Core.getMotdListener().refresh();

        player.sendMessage(
                new TextComponent("§eMaintenance mode has been " + (enable ? "§aenabled§e." : "§cdisabled§e.")));
    }
}
