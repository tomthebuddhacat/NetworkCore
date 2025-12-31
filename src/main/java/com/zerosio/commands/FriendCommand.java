package com.zerosio.commands;

import com.zerosio.api.CoreAPI;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.friends.Friend;
import com.zerosio.rank.Rank;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;
import java.util.List;

public class FriendCommand extends CommandBase {

	@Override
	public String getName() {
		return "friend";
	}

	@Override
	public String getDescription() {
		return "Manage your friends";
	}

	@Override
	public String getUsage() {
		return "/friend <add|remove|removeall|list|best|accept|deny|block|unblock|notifications|help> [player]";
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("f", "friends");
	}

	@Override
	public void execute(ProxiedPlayer player, String[] args) {
		if (args.length == 0) {
			Friend.sendHelp(player);
			return;
		}

		String sub = args[0].toLowerCase();

		switch (sub) {
			case "help":
				Friend.sendHelp(player);
				break;

			case "add":
				if (args.length < 2) {
					player.sendMessage(new TextComponent("§cUsage: /friend add <player>"));
					return;
				}

				if (args[1].equalsIgnoreCase(player.getName())) {
					player.sendMessage(new TextComponent("§cYou cannot add yourself as a friend!"));
					return;
				}

				ProxiedPlayer targetAdd = CoreAPI.getProxyPlayer(args[1]);
				if (targetAdd != null) {
					Friend.handleAdd(player, targetAdd);
				} else {
					player.sendMessage(new TextComponent("§cPlayer not found."));
				}
				break;

			case "remove":
				if (args.length < 2) {
					player.sendMessage(new TextComponent("§cUsage: /friend remove <player>"));
					return;
				}
				Friend.handleRemove(player, args[1]);
				break;

			case "removeall":
				Friend.handleRemoveAll(player);
				break;

			case "list":
				int page = 1;
				if (args.length > 1) {
					try {
						page = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						player.sendMessage(new TextComponent("§cPlease provide a valid page number!"));
						return;
					}
				}
				Friend.handleList(player, page);
				break;

			case "best":
				if (args.length < 2) {
					player.sendMessage(new TextComponent("§cUsage: /friend best <player>"));
					return;
				}

				if (args[1].equalsIgnoreCase(player.getName())) {
					player.sendMessage(new TextComponent("§cYou cannot make yourself a best friend!"));
					return;
				}

				ProxiedPlayer targetBest = CoreAPI.getProxyPlayer(args[1]);
				if (targetBest != null) {
					Friend.handleBestFriend(player, targetBest);
				} else {
					player.sendMessage(new TextComponent("§cPlayer not found."));
				}
				break;

			case "accept":
				if (args.length < 2) {
					player.sendMessage(new TextComponent("§cUsage: /friend accept <player>"));
					return;
				}
				ProxiedPlayer targetAccept = CoreAPI.getProxyPlayer(args[1]);
				if (targetAccept != null) {
					Friend.handleAccept(player, targetAccept);
				} else {
					player.sendMessage(new TextComponent("§cPlayer not found."));
				}
				break;

			case "deny":
				if (args.length < 2) {
					player.sendMessage(new TextComponent("§cUsage: /friend deny <player>"));
					return;
				}
				ProxiedPlayer targetDeny = CoreAPI.getProxyPlayer(args[1]);
				if (targetDeny != null) {
					Friend.handleDeny(player, targetDeny);
				} else {
					player.sendMessage(new TextComponent("§cPlayer not found."));
				}
				break;

			case "block":
				if (args.length < 2) {
					player.sendMessage(new TextComponent("§cUsage: /friend block <player>"));
					return;
				}

				// Prevent blocking yourself
				if (args[1].equalsIgnoreCase(player.getName())) {
					player.sendMessage(new TextComponent("§cYou cannot block yourself!"));
					return;
				}

				ProxiedPlayer targetBlock = CoreAPI.getProxyPlayer(args[1]);
				if (targetBlock != null) {
					Friend.handleBlock(player, targetBlock);
				} else {
					player.sendMessage(new TextComponent("§cPlayer not found."));
				}
				break;

			case "unblock":
				if (args.length < 2) {
					player.sendMessage(new TextComponent("§cUsage: /friend unblock <player>"));
					return;
				}
				Friend.handleUnblock(player, args[1]);
				break;

			case "notifications":
				Friend.handleToggleNotifications(player);
				break;

			default:
				player.sendMessage(new TextComponent("§cUnknown subcommand. Use /friend help."));
				break;
		}
	}

	@Override
	public Rank getRequiredRank() {
		return Rank.DEFAULT;
	}
}