package net.calyro.commands;

import net.calyro.api.CoreAPI;
import net.calyro.commands.impl.CommandBase;
import net.calyro.rank.Rank;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class StaffListCommand extends CommandBase {

    @Override
    public String getName() {
        return "stafflist";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("staff");
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.HELPER;
    }

    @Override
    public String getDescription() {
        return "Shows a list of all online staff members.";
    }

    @Override
    public String getUsage() {
        return "/stafflist";
    }

    @Override
    public void execute(ProxiedPlayer sender, String[] args) {
        List<ProxiedPlayer> staff = ProxyServer.getInstance().getPlayers().stream()
                .filter(p -> CoreAPI.getPlayerRank(p.getUniqueId()).isAboveOrEqual(Rank.HELPER))
                .collect(Collectors.toList());

        sender.sendMessage(new TextComponent("      §6STAFF [§a" + staff.size() + "§6]"));

        if (!staff.isEmpty()) {
            for (ProxiedPlayer p : staff) {
                Rank rank = CoreAPI.getPlayerRank(p.getUniqueId());
                sender.sendMessage(new TextComponent(rank.getPrefix() + p.getName()));
            }
        }
    }
}
