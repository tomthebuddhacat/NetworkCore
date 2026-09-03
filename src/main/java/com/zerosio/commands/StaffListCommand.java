package com.zerosio.commands;

import com.velocitypowered.api.proxy.Player;
import com.zerosio.Core;
import com.zerosio.api.CoreAPI;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
    public void execute(Player sender, String[] args) {
        List<Player> staff = Core.getInstance().getProxy().getAllPlayers().stream()
                .filter(p -> CoreAPI.getPlayerRank(p.getUniqueId()).isAboveOrEqual(Rank.HELPER))
                .collect(Collectors.toList());

        sender.sendMessage(Component.text("      STAFF [")
                .color(NamedTextColor.GOLD)
                .append(Component.text(staff.size(), NamedTextColor.GREEN))
                .append(Component.text("]", NamedTextColor.GOLD)));

        if (!staff.isEmpty()) {
            for (Player p : staff) {
                Rank rank = CoreAPI.getPlayerRank(p.getUniqueId());
                sender.sendMessage(Component.text(rank.getPrefix() + p.getUsername()));
            }
        }
    }
}
