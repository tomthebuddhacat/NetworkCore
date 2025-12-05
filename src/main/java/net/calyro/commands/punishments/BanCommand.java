package net.calyro.commands.punishments;

import net.calyro.api.CoreAPI;
import net.calyro.commands.impl.CommandBase;
import net.calyro.database.Punishment;
import net.calyro.rank.Rank;
import net.calyro.utility.CooldownManager;
import net.calyro.utility.RandomStringUtils;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.ProxyServer;

import java.util.Collections;

public class BanCommand extends CommandBase {

    @Override
    public String getName() {
        return "ban";
    }

    @Override
    public java.util.List<String> getAliases() {
        return Collections.singletonList("permban");
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.MOD;
    }

    @Override
    public String getDescription() {
        return "Ban a player";
    }

    @Override
    public String getUsage() {
        return "/ban <name> <reason>";
    }

    @Override
    public void execute(ProxiedPlayer sender, String[] args) {
        if (!CoreAPI.getPlayerRank(sender.getUniqueId()).isAboveOrEqual(Rank.OWNER))
        if (CooldownManager.isOnCooldown(sender.getUniqueId(), "ban")) {
            long remaining = CooldownManager.getRemainingCooldown(sender.getUniqueId(), "ban") / 1000;
            sender.sendMessage(new TextComponent("§cYou must wait " + remaining + " seconds before using this command again!"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(new TextComponent("§cInvalid syntax. Correct: /ban <name> <reason>"));
            return;
        }

        String targetName = args[0];
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(targetName);
        String uuid = null;

        if (target != null) {
            uuid = target.getUniqueId().toString();
        }

        if (uuid == null) {
            sender.sendMessage(new TextComponent("§cPlayer must be online to ban by name."));
            return;
        }

        if (Punishment.isBanned(target.getUniqueId(), target.getAddress().getAddress().getHostAddress())) {
            sender.sendMessage(new TextComponent("§cPlayer is already banned!"));
            return;
        }

        String banId = RandomStringUtils.random(8, "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
        String ip = target.getAddress().getAddress().getHostAddress();

        Punishment.addBan(target.getUniqueId(), ip, targetName, reason, -1, banId);

        sender.sendMessage(new TextComponent("§aPermanently banned §e" + targetName + " §afor §f" + reason));

        if (target != null) {
            target.disconnect(new TextComponent(
                    "§cYou are permanently banned from this server!\n\n" +
                            "§7Reason: §f" + reason + "\n" +
                            "§7Find out more: §b§n" + PunishmentDomains.BAN + "\n\n" +
                            "§7Ban ID: §f#" + banId + "\n" +
                            "§7Sharing your Ban ID may affect the processing of your appeal!"));
        }

        CooldownManager.setCooldown(sender.getUniqueId(), "ban");
    }
}
