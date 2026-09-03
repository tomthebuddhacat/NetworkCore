package com.zerosio.guilds;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.zerosio.Core;
import com.zerosio.Messages;
import com.zerosio.guilds.database.GuildDatabase;
import com.zerosio.guilds.other.Color;

import org.bson.Document;

import java.util.*;

public class Guild {
	public static final Map<UUID, Guild> GUILD_CACHE = new HashMap<>();

	private String name;
	private String displayName;
	private String motd;
	private String tag;
	private String tagColor;
	private long mutedUntil;
	private UUID uuid;
	private ArrayList<String> members;
	private ArrayList<String> officer;
	private HashMap<String, Long> muted;
	private String leader;

	private long experience;
	private Map<String, Long> expHistory;

	private String discord;
	private boolean open;
	private boolean publicDiscord;
	private String description;

	private GuildDatabase database;

	public Guild(UUID uuid, String leader, String name) {
		this.uuid = uuid;
		this.leader = leader;
		this.name = name;
		this.displayName = name;

		this.experience = 0;
		this.expHistory = new HashMap<>();
		this.expHistory.put(leader, 0L);

		this.discord = "none";
		this.open = false;
		this.publicDiscord = false;
		this.description = "none";

		this.motd = "";
		this.tag = "";
		this.tagColor = "7";
		this.mutedUntil = 0;
		this.muted = new HashMap<>();
		this.members = new ArrayList<>();
		this.officer = new ArrayList<>();

		GUILD_CACHE.put(uuid, this);
		GuildDatabase.collection.insertOne(new Document("_id", uuid.toString()).append("name", name).append("displayName", displayName).append("leader", leader).append("motd", motd).append("tag",
										   tag).append("mutedUntil", mutedUntil).append("members", members).append("officer", officer).append("experience", experience).append("expHistory", expHistory).append("tagColor",
												   tagColor).append("discord", discord).append("open", open).append("publicDiscord", publicDiscord).append("description", description));
		this.database = new GuildDatabase(uuid.toString());
	}

	public Guild(UUID uuid, String leader, String name, String displayName, String motd, String tag, String tagColor, ArrayList<String> members, ArrayList<String> officer, long mutedUntil,
				 long experience, Map<String, Long> expHistory, String discord, boolean open, boolean publicDiscord, String description) {
		this.uuid = uuid;
		this.leader = leader;
		this.name = name;
		this.displayName = displayName;

		this.experience = experience;
		this.expHistory = expHistory;

		this.discord = discord;
		this.open = open;
		this.publicDiscord = publicDiscord;
		this.description = description;

		this.motd = motd;
		this.tag = tag;
		this.tagColor = tagColor;
		this.mutedUntil = mutedUntil;
		this.members = members;
		this.officer = officer;
		this.muted = new HashMap<>();

		GUILD_CACHE.put(uuid, this);
		this.database = new GuildDatabase(uuid.toString());
	}

	public static Guild getGuild(UUID uuid) {
		if (GUILD_CACHE.containsKey(uuid)) return GUILD_CACHE.get(uuid);
		return null;
	}

	public static Guild getGuildFromName(String name) {
		for (Guild guild : GUILD_CACHE.values()) {
			if (guild.getName().equalsIgnoreCase(name)) return guild;
		}
		return null;
	}

	public static Guild getGuildFromTag(String tag) {
		for (Guild guild : GUILD_CACHE.values()) {
			if (guild.getTag().equalsIgnoreCase(tag)) return guild;
		}
		return null;
	}

	public static boolean inGuild(Player player) {
		try {
			for (Guild guild : GUILD_CACHE.values()) {
				if (guild.getAllPlayers().contains(player.getUniqueId().toString())) return true;
			}
			return false;
		} catch (Exception ignored) { }
		return false;
	}

	public static Guild getGuildFromPlayer(Player player) {
		for (Guild guild : GUILD_CACHE.values()) {
			if (guild.getAllPlayers().contains(player.getUniqueId().toString())) return guild;
		}
		return null;
	}

	public ArrayList<Player> getOnlinePlayers() {
		ArrayList<Player> players = new ArrayList<>();
		/*
		You can do this in two ways:
		for (String player : getAllPlayers()) {
			Optional<Player> p = Core.getInstance().getProxy().getPlayer(UUID.fromString(player));
			if (p.isPresent()) {
				players.add(p.get());
			}
		}
		 */
		for (String player : getAllPlayers()) {
			Player p = Core.getInstance().getProxy().getPlayer(UUID.fromString(player)).orElse(null);
			if (p != null) {
				players.add(p);
			}
		}
		return players;
	}

	public void mute(String player, Long mutedUntil) {
		muted.remove(player);
		muted.put(player, mutedUntil);
	}

	public Long isMuted(String player) {
		if (muted.containsKey(player) && System.currentTimeMillis() - muted.get(player) > 0) {
			return System.currentTimeMillis() - muted.get(player);
		}
		return 0L;
	}

	public void left(String player) {
		if (members.contains(player)) {
			ArrayList<String> members2 = getMembers();
			members2.remove(player);
			setMembers(members2);
		} else if (officer.contains(player)) {
			ArrayList<String> members2 = getOfficer();
			members2.remove(player);
			setOfficer(members2);
		}
	}

	public ArrayList<Player> getOnlinePlayersExceptLeader() {
		ArrayList<Player> players = new ArrayList<>();
		for (String player : members) {
			Player p = Core.getInstance().getProxy().getPlayer(UUID.fromString(player)).orElse(null);
			if (p != null) {
				players.add(p);
			}
		}
		for (String player : officer) {
			Player p = Core.getInstance().getProxy().getPlayer(UUID.fromString(player)).orElse(null);
			if (p != null) {
				players.add(p);
			}
		}
		return players;
	}

	public ArrayList<String> getAllPlayers() {
		ArrayList<String> players = new ArrayList<>();
		players.add(leader);
		players.addAll(members);
		players.addAll(officer);
		return players;
	}

	public boolean isLeader(Player player) {
		return player.getUniqueId().toString().equals(leader);
	}

	public boolean isOfficer(Player player) {
		return officer.contains(player.getUniqueId().toString());
	}

	public boolean isMember(Player player) {
		return members.contains(player.getUniqueId().toString());
	}

	public void setName(String toSet) {
		this.displayName = toSet;
		this.name = toSet.toUpperCase();
		database.set("name", toSet.toUpperCase());
		database.set("displayName", toSet);
	}

	public void setMotd(String toSet) {
		this.motd = toSet;
		database.set("motd", toSet);
	}

	public void setTag(String toSet) {
		this.tag = toSet.toLowerCase();
		database.set("tag", toSet.toLowerCase());
	}

	public void setTagColor(String toSet) {
		this.tagColor = toSet.toLowerCase();
		database.set("tagColor", toSet.toLowerCase());
	}

	public void setMutedUntil(Long toSet) {
		this.mutedUntil = toSet;
		database.set("mutedUntil", toSet);
	}

	public void setMembers(ArrayList<String> toSet) {
		this.members = toSet;
		database.set("members", toSet);
	}

	public void setOfficer(ArrayList<String> toSet) {
		this.officer = toSet;
		database.set("officer", toSet);
	}

	public void setLeader(String toSet) {
		this.leader = toSet;
		database.set("leader", toSet);
	}

	public void setExperience(long toSet) {
		this.experience = toSet;
		database.set("experience", toSet);
	}

	public void addExperience(Player player, long toAdd) {
		if (Level.getLevelFromXP(experience + toAdd) > Level.getLevelFromXP(experience)){
			getAllPlayers().forEach(members -> {
				Player p = Core.getInstance().getProxy().getPlayer(UUID.fromString(members)).orElse(null);
				if (p != null) {
					p.sendMessage(Messages.get("guild-level-up-header"));
					p.sendMessage(Messages.get("guild-level-up-message", Collections.singletonMap("guildLevel", String.valueOf(Level.getLevelFromXP(experience + toAdd)))));
					p.sendMessage(Messages.get("guild-level-up-header"));
				}
			});
		}
		expHistory.put(player.getUniqueId().toString(), toAdd + (expHistory.containsKey(player.getUniqueId().toString()) ? expHistory.get(player.getUniqueId().toString()) : 0));
		setExpHistory(expHistory);
		experience += toAdd;
		database.set("experience", experience);
	}

	public void setExpHistory(Map<String, Long> toSet) {
		this.expHistory = toSet;
		database.set("expHistory", toSet);
	}

	public void setDiscord(String toSet) {
		this.discord = toSet;
		database.set("discord", toSet);
	}

	public void setOpen(boolean toSet) {
		this.open = toSet;
		database.set("open", toSet);
	}

	public void setDescription(String toSet) {
		this.description = toSet;
		database.set("description", toSet);
	}

	public void setPublicDiscord(boolean toSet) {
		this.publicDiscord = toSet;
		database.set("publicDiscord", toSet);
	}

	public String getName() {
		return name;
	}
	public String getDisplayName() {
		return displayName;
	}
	public String getMotd() {
		return motd;
	}
	public String getTag() {
		return tag;
	}
	public String getTagColor() {
		return tagColor;
	}
	public String getLeader() {
		return leader;
	}
	public String getDiscord() {
		return discord;
	}
	public String getDescription() {
		return description;
	}
	public long getMutedUntil() {
		return mutedUntil;
	}
	public long getExperience() {
		return experience;
	}
	public boolean isOpen() {
		return open;
	}
	public boolean isPublicDiscord() {
		return publicDiscord;
	}
	public UUID getUuid() {
		return uuid;
	}
	public ArrayList<String> getMembers() {
		return members;
	}
	public ArrayList<String> getOfficer() {
		return officer;
	}
	public Map<String, Long> getExpHistory() {
		return expHistory;
	}
	public Map<String, Long> getMuted() {
		return muted;
	}
	public GuildDatabase getDatabase() {
		return database;
	}


	public static void register() {
		Core.getInstance().getProxy().getChannelRegistrar().register(MinecraftChannelIdentifier.create("guilds", "tag"));
		new GuildDatabase();

		new Thread(() -> {
			GuildDatabase.collection.find().into(new ArrayList<>()).forEach(guild -> {
				Guild g = new Guild(UUID.fromString(guild.getString("_id")), guild.getString("leader"), guild.getString("name"), (guild.containsKey("displayName") ? guild.getString("displayName") : guild.getString("name")), guild.getString("motd"), guild.getString("tag"), guild.get("tagColor", "7"), (ArrayList<String>) guild.get("members"), (ArrayList<String>) guild.get("officer"), guild.getLong("mutedUntil"), (guild.containsKey("experience") ? guild.getLong("experience") : 0), (guild.containsKey("expHistory") ? (Map<String, Long>) guild.get("expHistory") : new HashMap<>()),
									guild.get("discord", "none"), guild.getBoolean("open", false), guild.getBoolean("publicDiscord", false), guild.get("description", "none"));
				if (!guild.containsKey("expHistory")) {
					HashMap<String, Long> expHistory = new HashMap<>();
					g.getAllPlayers().forEach((p) -> {
						expHistory.put(p, 0L);
					});
					g.setExpHistory(expHistory);
				}
				if (!guild.containsKey("displayName")) g.setName(guild.getString("name"));
				if (!guild.containsKey("tagColor")) g.setTagColor("7");
				if (!guild.containsKey("experience")) g.setExperience(0);
				if (!guild.containsKey("description")) g.setDescription("none");
				if (!guild.containsKey("open")) g.setOpen(false);
				if (!guild.containsKey("publicDiscord")) g.setPublicDiscord(false);
				if (!guild.containsKey("discord")) g.setDiscord("none");
				if (!Color.getUnlockedColors(UUID.fromString(g.getLeader()), g).contains(Color.getTagColorByCode(g.getTagColor()).name())) {
					g.setTagColor("7");
				}
			});
		}).start();
	}
}
