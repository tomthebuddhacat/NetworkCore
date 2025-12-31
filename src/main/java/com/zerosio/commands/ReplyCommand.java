package com.zerosio.commands;

import com.zerosio.commands.impl.CommandBase;
import com.zerosio.database.User;
import com.zerosio.privacy.Ignores;
import com.zerosio.rank.Rank;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.ProxyServer;

import java.util.*;

public class ReplyCommand extends CommandBase {

    @Override
    public String getName() {
        return "reply";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("r");
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.DEFAULT;
    }

    @Override
    public String getDescription() {
        return "Reply to the last private message across servers.";
    }

    @Override
    public String getUsage() {
        return "/reply <message>";
    }

    @Override
    public void execute(ProxiedPlayer sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(new TextComponent("§cUsage: /reply <message>"));
            return;
        }

        User senderUser = User.getUser(sender.getUniqueId());
        String targetUUIDStr = senderUser.getData("reply_target");

        if (targetUUIDStr == null) {
            sender.sendMessage(new TextComponent("§cNo one to reply to."));
            return;
        }

        UUID targetUUID = UUID.fromString(targetUUIDStr);
        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(targetUUID);

        if (target == null || !target.isConnected()) {
            sender.sendMessage(new TextComponent("§cThat player is no longer online."));
            return;
        }

        if (sender.getUniqueId() == target.getUniqueId() && sender.getName() != "Zerosio") {
            sender.sendMessage("§cYou cannot reply to yourself!");
            return;
        }

        if (Ignores.getIgnoredUsers(target.getUniqueId()).contains(sender.getUniqueId())) {
            sender.sendMessage("§cYou have been ignored by this user.");
            return;
        }

        if (Ignores.getIgnoredUsers(sender.getUniqueId()).contains(target.getUniqueId())) {
            sender.sendMessage(
                    "§cYou have ignored this player. Use '/unignore <player>' to remove from your ignore list.");
            return;
        }

        String message = String.join(" ", args);
        User targetUser = User.getUser(targetUUID);

        senderUser.setData("reply_target", target.getUniqueId().toString());
        targetUser.setData("reply_target", sender.getUniqueId().toString());

        String senderName = senderUser.getRank().getPrefix() + sender.getName();
        String targetName = targetUser.getRank().getPrefix() + target.getName();

        TextComponent received = new TextComponent("§dFrom " + senderName + "§7: §f" + message);
        received.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + sender.getName() + " "));
        received.setHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§eClick to reply").create()));
        target.sendMessage(received);

        TextComponent sent = new TextComponent("§dTo " + targetName + "§7: §f" + message);
        sent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + target.getName() + " "));
        sent.setHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§eClick to message again").create()));
        sender.sendMessage(sent);
    }
}
