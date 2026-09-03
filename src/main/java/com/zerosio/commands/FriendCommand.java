package com.zerosio.commands;

import com.velocitypowered.api.proxy.Player;
import com.zerosio.Messages;
import com.zerosio.api.CoreAPI;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.friends.Friend;
import com.zerosio.rank.Rank;

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
	public void execute(Player player, String[] args) {
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
					player.sendMessage(Messages.get("friend-command-usage"));
					return;
				}

				if (args[1].equalsIgnoreCase(player.getUsername())) {

					player.sendMessage(Messages.get("friend-command-you-cannot-add-yourself"));
					return;
				}

				Player targetAdd = CoreAPI.getProxyPlayer(args[1]);
				if (targetAdd != null) {
					Friend.handleAdd(player, targetAdd);
				} else {
					player.sendMessage(Messages.get("friend-command-player-not-online"));
				}
				break;

			case "remove":
				if (args.length < 2) {
					player.sendMessage(Messages.get("remove-friend-command-usage"));
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
						player.sendMessage(Messages.get("friend-command-provide-valid-page-number"));
						return;
					}
				}
				Friend.handleList(player, page);
				break;

			case "best":
				if (args.length < 2) {
					player.sendMessage(Messages.get("best-friend-command-usage"));
					return;
				}

				if (args[1].equalsIgnoreCase(player.getUsername())) {
					player.sendMessage(Messages.get("friend-command-you-cannot-bestfriend-yourself"));
					return;
				}

				Player targetBest = CoreAPI.getProxyPlayer(args[1]);
				if (targetBest != null) {
					Friend.handleBestFriend(player, targetBest);
				} else {
					player.sendMessage(Messages.get("friend-command-player-not-online"));
				}
				break;

			case "accept":
				if (args.length < 2) {
					player.sendMessage(Messages.get("friend-command-accept-usage"));
					return;
				}
				Player targetAccept = CoreAPI.getProxyPlayer(args[1]);
				if (targetAccept != null) {
					Friend.handleAccept(player, targetAccept);
				} else {
					player.sendMessage(Messages.get("friend-command-player-not-online"));
				}
				break;

			case "deny":
				if (args.length < 2) {
					player.sendMessage(Messages.get("friend-command-deny-usage"));
					return;
				}
				Player targetDeny = CoreAPI.getProxyPlayer(args[1]);
				if (targetDeny != null) {
					Friend.handleDeny(player, targetDeny);
				} else {
					player.sendMessage(Messages.get("friend-command-player-not-online"));
				}
				break;

			case "block":
				if (args.length < 2) {
					player.sendMessage(Messages.get("friend-command-block-usage"));
					return;
				}

				// Prevent blocking yourself
				if (args[1].equalsIgnoreCase(player.getUsername())) {
					player.sendMessage(Messages.get("friend-command-cannot-block-yourself"));
					return;
				}

				Player targetBlock = CoreAPI.getProxyPlayer(args[1]);
				if (targetBlock != null) {
					Friend.handleBlock(player, targetBlock);
				} else {
					player.sendMessage(Messages.get("friend-command-player-not-online"));
				}
				break;

			case "unblock":
				if (args.length < 2) {
					player.sendMessage(Messages.get("friend-command-unblock-usage"));
					return;
				}
				Friend.handleUnblock(player, args[1]);
				break;

			case "notifications":
				Friend.handleToggleNotifications(player);
				break;

			default:
				player.sendMessage(Messages.get("unknown-friend-sub-command"));
				break;
		}
	}

	@Override
	public Rank getRequiredRank() {
		return Rank.DEFAULT;
	}
}