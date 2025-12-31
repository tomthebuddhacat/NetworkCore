package com.zerosio.commands;

import com.zerosio.api.CoreAPI;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.rank.Rank;
import com.zerosio.utility.ChatUtils;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;
import java.util.List;

public class StaffChatCommand extends CommandBase {

    @Override
    public String getName() {
        return "staffchat";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("sc");
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.HELPER;
    }

    @Override
    public String getDescription() {
        return "Talk privately with other staff members.";
    }

    @Override
    public String getUsage() {
        return "/staffchat <message>";
    }

    @Override
    public void execute(ProxiedPlayer sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /staffchat <message>");
            return;
        }

        String messageText = String.join(" ", args);
        Rank rank = CoreAPI.getPlayerRank(sender.getUniqueId());
        String formatted = "§b[STAFF] " + rank.getPrefix() + sender.getName() + "§f: §f" + messageText;

        for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
            if (CoreAPI.getPlayerRank(p.getUniqueId()).isAboveOrEqual(Rank.HELPER)) {
                ChatUtils.sendClickableText(p, formatted, "§eClick to chat", "/sc ", ClickEvent.Action.SUGGEST_COMMAND);
            }
        }
    }
}
