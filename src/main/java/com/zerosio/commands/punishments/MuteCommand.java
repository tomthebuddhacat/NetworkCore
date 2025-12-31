package com.zerosio.commands.punishments;

import com.zerosio.api.CoreAPI;
import com.zerosio.commands.impl.CommandBase;
import com.zerosio.database.Punishment;
import com.zerosio.rank.Rank;
import com.zerosio.utility.CooldownManager;
import com.zerosio.utility.RandomStringUtils;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.ProxyServer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MuteCommand extends CommandBase {

	private static final Pattern periodPattern = Pattern.compile("([0-9]+)(mo|[hdwmy])");

	@Override
	public String getName() {
		return "mute";
	}

	@Override
	public List<String> getAliases() {
		return Collections.singletonList("tempmute");
	}

	@Override
	public Rank getRequiredRank() {
		return Rank.HELPER;
	}

	@Override
	public String getDescription() {
		return "Mute a player for a certain time.";
	}

	@Override
	public String getUsage() {
		return "/mute <name> <length> <reason>";
	}

	@Override
	public void execute(ProxiedPlayer player, String[] args) {
		if (!CoreAPI.getPlayerRank(player.getUniqueId()).isAboveOrEqual(Rank.OWNER)) {
			if (CooldownManager.isOnCooldown(player.getUniqueId(), "mute")) {
				long remaining = CooldownManager.getRemainingCooldown(player.getUniqueId(), "mute") / 1000;
				player.sendMessage(new TextComponent("§cYou must wait " + remaining + " seconds before using this command again!"));
				return;
			}
			CooldownManager.setCooldown(player.getUniqueId(), "mute");
		}

		if (args.length < 3) {
			player.sendMessage(new TextComponent("§cUsage: /mute <name> <length> <reason>"));
			return;
		}

		String targetName = args[0];
		String lengthStr = args[1];
		String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
		long muteLength = parsePeriod(lengthStr);

		if (muteLength <= 0) {
			player.sendMessage(new TextComponent("§cInvalid time format. Example: 10m, 1h, 2d"));
			return;
		}

		if (muteLength < 60) {
			player.sendMessage(new TextComponent("§cYou cannot mute someone for less than 1 minute."));
			return;
		}

		ProxiedPlayer target = ProxyServer.getInstance().getPlayer(targetName);
		if (target == null) {
			player.sendMessage(new TextComponent("§cPlayer must be online to mute."));
			return;
		}

		if (Punishment.isMuted(target.getUniqueId())) {
			player.sendMessage(new TextComponent("§cPlayer is already muted!"));
			return;
		}

		String muteId = RandomStringUtils.random(8, "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
		long expiry = System.currentTimeMillis() + (muteLength * 1000);

		Punishment.addMute(target.getUniqueId(), targetName, reason, expiry, muteId);

		player.sendMessage(new TextComponent("§aMuted §e" + targetName + " §afor §e" + lengthStr + " §afor §f" + reason));

		target.sendMessage(new TextComponent("§c§l§m---------------------------------------------"));
		target.sendMessage(new TextComponent("§cYou are currently muted for " + reason + "."));
		target.sendMessage(new TextComponent("§7Your mute will expire in §c" + calculateTime(muteLength)));
		target.sendMessage(new TextComponent("§7Mute ID: §f#" + muteId));
		target.sendMessage(new TextComponent("§c§l§m---------------------------------------------"));
	}

	public static String calculateTime(long seconds) {
		int days = (int) TimeUnit.SECONDS.toDays(seconds);
		long hours = TimeUnit.SECONDS.toHours(seconds) - (days * 24);
		long minutes = TimeUnit.SECONDS.toMinutes(seconds) - TimeUnit.SECONDS.toHours(seconds) * 60;
		long sec = TimeUnit.SECONDS.toSeconds(seconds) - TimeUnit.SECONDS.toMinutes(seconds) * 60;

		return (days > 0 ? days + "d " : "") +
			   (hours > 0 ? hours + "h " : "") +
			   (minutes > 0 ? minutes + "m " : "") +
			   (sec > 0 ? sec + "s" : "");
	}

	public static long parsePeriod(String period) {
		period = period.toLowerCase(Locale.ENGLISH);
		Matcher matcher = periodPattern.matcher(period);
		long totalSeconds = 0;

		while (matcher.find()) {
			int num = Integer.parseInt(matcher.group(1));
			String type = matcher.group(2);
			switch (type) {
			case "h":
				totalSeconds += num * 3600L;
				break;
			case "d":
				totalSeconds += num * 86400L;
				break;
			case "w":
				totalSeconds += num * 604800L;
				break;
			case "m":
				totalSeconds += num * 60L;
				break;
			case "mo":
				totalSeconds += num * 2592000L;
				break;
			case "y":
				totalSeconds += num * 31536000L;
				break;
			}
		}

		return totalSeconds;
	}
}
