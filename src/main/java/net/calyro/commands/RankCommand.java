package net.calyro.commands;

import net.calyro.api.CoreAPI;
import net.calyro.commands.impl.CommandBase;
import net.calyro.database.User;
import net.calyro.rank.Rank;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;
import java.util.List;

public class RankCommand extends CommandBase {

        @Override
        public String getName() {
                return "rank";
        }

        @Override
        public List<String> getAliases() {
                return Arrays.asList("setrank");
        }

        @Override
        public Rank getRequiredRank() {
                return Rank.ADMIN;
        }

        @Override
        public String getDescription() {
                return "Set a player's rank";
        }

        @Override
        public String getUsage() {
                return "/rank <player> <rank>";
        }

        @Override
        public void execute(ProxiedPlayer sender, String[] args) {
                executeCommand(sender, args);
        }

        @Override
        public void executeCommand(CommandSender sender, String[] args) {
                if (args.length < 2) {
                        sender.sendMessage(new TextComponent("§cUsage: " + getUsage()));
                        return;
                }

                String targetName = args[0];
                String rankName = args[1].toUpperCase().replace("+", "_PLUS");

                ProxiedPlayer target = ProxyServer.getInstance().getPlayer(targetName);
                if (target == null) {
                        sender.sendMessage(new TextComponent("§cPlayer not found."));
                        return;
                }

                Rank rank;
                try {
                        rank = Rank.valueOf(rankName);
                } catch (IllegalArgumentException e) {
                        sender.sendMessage(new TextComponent("§cInvalid rank. Available: " +
                                                                                                 Arrays.toString(Rank.values())));
                        return;
                }
                if (!CoreAPI.getPlayerRank(((ProxiedPlayer) sender).getUniqueId()).isAboveOrEqual(Rank.OWNER)) {
                        if (sender instanceof ProxiedPlayer && rank.isAboveOrEqual(CoreAPI.getPlayerRank(((ProxiedPlayer) sender).getUniqueId())) || rank == null) {
                                sender.sendMessage("§cYou cannot rank someone higher than your rank or equal to your rank.");
                                return;
                        }
                }

                CoreAPI.setRank(target, rank);

                sender.sendMessage(new TextComponent("§aSet rank of §e" + target.getName() +
                                                                                         " §ato " + rank.getPrefix()));
                target.sendMessage(new TextComponent("§aYour rank has been set to " + rank.getPrefixx() +
                                                                                         " §aby §e"
                                                                                         + (sender instanceof ProxiedPlayer
                                                                                                ? CoreAPI.getPlayerRank(((ProxiedPlayer) sender).getUniqueId()).getPrefix() + sender.getName()
                                                                                                : "§cConsole")));

                User user = User.getUser(target.getUniqueId());
                if (user != null) {
                        user.saveAsync();
                }
        }

        @Override
        public boolean consoleCommand() {
                return true;
        }
}