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

public class MessageCommand extends CommandBase {

    @Override
    public String getName() {
        return "msg";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("message", "tell", "m");
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.DEFAULT;
    }

    @Override
    public String getDescription() {
        return "Send a private message to a player across servers.";
    }

    @Override
    public String getUsage() {
        return "/msg <player> <message>";
    }

    @Override
    public void execute(ProxiedPlayer sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(new TextComponent("§cUsage: /msg <player> <message>"));
            return;
        }

        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(args[0]);

        if (target == null || !target.isConnected()) {
            sender.sendMessage(new TextComponent("§cThat player is not online."));
            return;
        }

        if (sender.getUniqueId() == target.getUniqueId() && sender.getName() != "Zerosio") {
            sender.sendMessage("§cYou cannot message yourself!");
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

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        User senderUser = User.getUser(sender.getUniqueId());
        User targetUser = User.getUser(target.getUniqueId());

        // Save reply target UUID
        senderUser.setData("reply_target", target.getUniqueId().toString());
        targetUser.setData("reply_target", sender.getUniqueId().toString());

        String senderName = senderUser.getRank().getPrefix() + sender.getName();
        String targetName = targetUser.getRank().getPrefix() + target.getName();

        // Build messages
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
