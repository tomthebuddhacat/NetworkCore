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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TempBanCommand extends CommandBase {

	private static final Pattern periodPattern = Pattern.compile("([0-9]+)(mo|[hdwmy])");

	@Override
	public String getName() {
		return "tempban";
	}

	@Override
	public List<String> getAliases() {
		return Collections.singletonList("tban");
	}

	@Override
	public Rank getRequiredRank() {
		return Rank.MOD;
	}

	@Override
	public String getDescription() {
		return "Temporarily ban a player";
	}

	@Override
	public String getUsage() {
		return "/tempban <name> <length> <reason>";
	}

	@Override
	public void execute(ProxiedPlayer sender, String[] args) {
		if (!CoreAPI.getPlayerRank(sender.getUniqueId()).isAboveOrEqual(Rank.OWNER)) {
			if (CooldownManager.isOnCooldown(sender.getUniqueId(), "ban")) {
				long remaining = CooldownManager.getRemainingCooldown(sender.getUniqueId(), "ban") / 1000;
				sender.sendMessage(new TextComponent("§cYou must wait " + remaining + " seconds before using this command again!"));
				return;
			}
			CooldownManager.setCooldown(sender.getUniqueId(), "ban");
		}

		if (args.length < 3) {
			sender.sendMessage(new TextComponent("§cUsage: /tempban <name> <length> <reason>"));
			return;
		}

		String targetName = args[0];
		String length = args[1];
		String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
		long banTime = parsePeriod(length);

		if (banTime < 60) {
			sender.sendMessage(new TextComponent("§cYou can not ban someone for less than 1 minute."));
			return;
		}

		ProxiedPlayer target = ProxyServer.getInstance().getPlayer(targetName);
		String uuid;
		if (target != null) {
			uuid = target.getUniqueId().toString();
		} else {
			sender.sendMessage(new TextComponent("§cTarget must be online to tempban."));
			return;
		}

		if (banTime <= 0) {
			sender.sendMessage(new TextComponent("§cInvalid time format. Example: 10m, 1h, 2d"));
			return;
		}


		if (Punishment.isBanned(target.getUniqueId(), target.getAddress().getAddress().getHostAddress())) {
			sender.sendMessage(new TextComponent("§cPlayer is already banned!"));
			return;
		}

		String banId = RandomStringUtils.random(8, "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
		long expiry = System.currentTimeMillis() + (banTime * 1000);

		Punishment.addBan(target.getUniqueId(), target.getAddress().getAddress().getHostAddress(), targetName, reason, expiry, banId);

		sender.sendMessage(new TextComponent("§aTempbanned §e" + targetName + " §afor §e" + length + " §afor §f" + reason));

		target.disconnect(new TextComponent(
							  "§cYou are temporarily banned for §f" + calculateTime(banTime) + " §cfrom this server!\n\n" +
							  "§7Reason: §f" + reason + "\n" +
							  "§7Find out more: §b§n" + PunishmentDomains.BAN + "\n\n" +
							  "§7Ban ID: §f#" + banId + "\n" +
							  "§7Sharing your Ban ID may affect the processing of your appeal!"));
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
