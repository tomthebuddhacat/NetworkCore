package net.calyro.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import net.calyro.Config;
import net.calyro.chat.ChatModes;
import net.calyro.rank.Rank;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.bson.Document;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class User {

	private static String MONGO_URI;
	private static String MONGO_DB;

	private static MongoClient mongoClient;
	private static MongoCollection<Document> collection;
	private static final Map<UUID, User> userCache = new ConcurrentHashMap<>();

	private final UUID uuid;
	private final Document data;

	public static void loadConfig() {
		MONGO_URI = Config.getString("database.uri", "mongodb://localhost:27017");
		MONGO_DB = Config.getString("database.name", "network_test");
		System.out.println("[MongoDB] Loaded config - URI: " + MONGO_URI + ", DB: " + MONGO_DB);
	}

	public User(String username, UUID premiumUUID) {
		if (premiumUUID != null) {
			this.uuid = premiumUUID;
		} else {
			this.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
		}

		Document found = collection.find(Filters.eq("uuid", uuid.toString())).first();

		if (found == null) {
			this.data = new Document();
			initializeDefaults();
			save();
		} else {
			this.data = found;
		}
	}

	private User(UUID uuid) {
		this.uuid = uuid;

		Document found = collection.find(Filters.eq("uuid", uuid.toString())).first();
		if (found == null) {
			this.data = new Document();
			initializeDefaults();
			save();
		} else {
			this.data = found;
		}
	}

	public static void connect() {
		loadConfig(); // Load config first

		try {
			ConnectionString connString = new ConnectionString(MONGO_URI);
			MongoClientSettings settings = MongoClientSettings.builder()
					.applyConnectionString(connString)
					.build();

			mongoClient = MongoClients.create(settings);
			MongoDatabase database = mongoClient.getDatabase(MONGO_DB);
			collection = database.getCollection("users");

			System.out.println("[MongoDB] Connected to: " + MONGO_DB);
		} catch (Exception e) {
			System.err.println("[MongoDB] Connection failed: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void disconnect() {
		try {
			if (mongoClient != null) {
				mongoClient.close();
				System.out.println("[MongoDB] Disconnected from database");
			}
		} catch (Exception e) {
			System.err.println("[MongoDB] Error disconnecting: " + e.getMessage());
		}
	}

	public static boolean exists(UUID uuid) {
		try {
			return collection.find(Filters.eq("uuid", uuid.toString())).first() != null;
		} catch (Exception e) {
			System.err.println("[MongoDB] Error checking if user exists: " + e.getMessage());
			return false;
		}
	}

	private void initializeDefaults() {
		data.put("uuid", uuid.toString());
		data.put("last_known_name", "Unknown");
		data.put("first_login", System.currentTimeMillis());
		data.put("last_login", 1L);
		data.put("last_logout", 2L);
		data.put("createdAt", System.currentTimeMillis());
		data.put("rank", Rank.DEFAULT.name().toUpperCase());
		data.put("package_rank", Rank.DEFAULT.name());
		data.put("new_package_rank", Rank.DEFAULT.name());
		data.put("monthly_rank", Rank.DEFAULT.name());
		data.put("monthly_rank_bought_time", -1L);
		data.put("authentication", new Document()
				.append("isPremium", false)
				.append("lastSessionValidation", -1L)
				.append("premiumUuid", null)
	          	.append("password", "temp")
	          	.append("isRegistered", false)
	          	.append("registeredAt", Instant.now()));
		data.put("debug_mode", false);
		data.put("active_chat", ChatModes.PUBLIC.name());
		data.put("currently_messaging", "nobody");
		data.put("friend", new Document()
				.append("join_leave_msg", true)
				.append("friends", new ArrayList<String>())
				.append("best_friends", new ArrayList<String>())
				.append("blocked_users", new ArrayList<String>()));
	}

	public static User getByPremiumUUID(UUID premiumUUID) {
		return collection.find(Filters.eq("authentication.premiumUuid", premiumUUID.toString()))
				.map(doc -> {
					UUID uuid = UUID.fromString(doc.getString("uuid"));
					return userCache.computeIfAbsent(uuid, User::new);
				})
				.first();
	}

	public static boolean existsUserInDatabase(UUID uuid) {
		try {
			return collection.find(Filters.eq("uuid", uuid.toString())).first() != null;
		} catch (Exception e) {
			System.err.println("[MongoDB] Error checking if user exists: " + e.getMessage());
			return false;
		}
	}

	public static User getUser(UUID uuid) {
		return userCache.computeIfAbsent(uuid, User::new);
	}

	public static User getUser(String username, UUID premiumUUID) {
		UUID uuid = premiumUUID != null
				? premiumUUID
				: UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));

		return userCache.computeIfAbsent(uuid, key -> new User(username, premiumUUID));
	}


	public static User getUser(String name) {
		return userCache.values().stream()
				.filter(user -> user.getString("last_known_name").equalsIgnoreCase(name))
				.findFirst()
				.orElse(null);
	}

	public void setData(String key, Object value) {
		data.put(key, value);
		saveAsync();
	}

	public String getString(String key) {
		Object val = data.get(key);
		return val != null ? val.toString() : null;
	}

	public boolean getBoolean(String key) {
		Object val = data.get(key);
		return val instanceof Boolean && (Boolean) val;
	}

	public long getLong(String key) {
		Object val = data.get(key);
		if (val instanceof Number) {
			return ((Number) val).longValue();
		}
		try {
			return Long.parseLong(val.toString());
		} catch (Exception e) {
			return 0L;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getData(String key) {
		return (T) data.get(key);
	}

	public <T extends Enum<T>> T getEnum(String key, Class<T> enumClass, T def) {
		String value = getString(key);
		if (value == null)
			return def;
		try {
			return Enum.valueOf(enumClass, value.toUpperCase());
		} catch (Exception e) {
			return def;
		}
	}

	public void save() {
		try {
			collection.replaceOne(
					Filters.eq("uuid", uuid.toString()),
					data,
					new ReplaceOptions().upsert(true));
		} catch (Exception e) {
			System.err.println("[MongoDB] Error saving user data for " + uuid + ": " + e.getMessage());
		}
	}

	public void saveAsync() {
		ProxyServer.getInstance().getScheduler().runAsync(
				ProxyServer.getInstance().getPluginManager().getPlugin("NetworkCore"),
				this::save);
	}

	public static void saveAll() {
		for (User user : userCache.values()) {
			user.saveAsync();
		}
	}

	public void debug(ProxiedPlayer player, String message) {
		if (getBoolean("debug_mode")) {
			player.sendMessage(new TextComponent("§c[SYSTEM] §f" + message));
		}
	}

	public Rank getRank() {
		Rank storedRank = getEnum("rank", Rank.class, Rank.DEFAULT);
		Rank packageRank = getEnum("package_rank", Rank.class, Rank.DEFAULT);
		Rank newPackageRank = getEnum("new_package_rank", Rank.class, packageRank);
		Rank monthlyRank = getEnum("monthly_rank", Rank.class, Rank.DEFAULT);

		if (storedRank != null && storedRank.isStaff()) {
			return storedRank;
		}

		long currentTime = System.currentTimeMillis();
		Object monthlyBoughtTimeObj = data.get("monthly_rank_bought_time");
		long monthlyBoughtTime = monthlyBoughtTimeObj instanceof Number ? ((Number) monthlyBoughtTimeObj).longValue()
				: -1L;

		if (monthlyRank != null && monthlyBoughtTime > 0 &&
				(currentTime - monthlyBoughtTime <= 2592000000L)) {
			return monthlyRank;
		}

		return newPackageRank != null ? newPackageRank : Rank.DEFAULT;
	}

	public void setRank(Rank rank) {
		setData("rank", rank.name().toUpperCase());
	}

	public void setPackageRank(Rank rank) {
		setData("package_rank", rank.name());
	}

	public void setNewPackageRank(Rank rank) {
		setData("new_package_rank", rank.name());
	}

	public void setMonthlyRank(Rank rank) {
		setData("monthly_rank", rank.name());
		setData("monthly_rank_bought_time", System.currentTimeMillis());
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setLastKnownName(String name) {
		setData("last_known_name", name);
	}

	public static String retrieveLastKnownName(UUID uuid) {
		return getUser(uuid).getString("last_known_name");
	}

	public String retrieveLastKnownName() {
		return getString("last_known_name");
	}

	public static Collection<User> getAllUsers() {
		return userCache.values();
	}
	
	public ChatModes getChatMode() {
		return ChatModes.valueOf(getString("active_chat"));
	}
	
	public void setChatMode(ChatModes chatMode) {
		setData("active_chat", chatMode.name());
	}
	
	public String getCurrentlyMessaging() {
		return getString("currently_messaging");
	}
	
	public void setCurrentlyMessaging(String username) {
		setData("currently_messaging", username);
	}
}