package com.zerosio.guilds.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.zerosio.database.MongoManager;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GuildDatabase {
	public final String id;

	public static MongoCollection<Document> collection;

	public GuildDatabase(String id) {
		this.id = id;
		ensureCollection();
	}

	public GuildDatabase() {
		this.id = String.valueOf(UUID.randomUUID());
		ensureCollection();
	}
	
	public String getId() {
		return id;
	}

	private static void ensureCollection() {
		if (collection == null) {
			collection = MongoManager.getDatabase().getCollection("guilds");
		}
	}
	
	public static void ensureCollectionn() {
		if (collection == null) {
			collection = MongoManager.getDatabase().getCollection("guilds");
		}
	}

	public Document getDocument() {
		return collection.find(Filters.eq("_id", id)).first();
	}

	public void set(String key, Object value) {
		insertOrUpdate(key, value, false);
	}

	public Object get(String key, Object def) {
		Document doc = collection.find(Filters.eq("_id", id)).first();
		if (doc == null || doc.get(key) == null) {
			return def;
		}
		return doc.get(key);
	}

	public static Document findByGuildName(String guild) {
		return collection.find(Filters.eq("name", guild)).first();
	}

	public static Document findByGuildTag(String tag) {
		return collection.find(Filters.eq("tag", tag)).first();
	}

	public String getString(String key, String def) {
		Object val = get(key, def);
		return val != null ? val.toString() : def;
	}

	public int getInt(String key, int def) {
		Object val = get(key, def);
		return val != null ? Integer.parseInt(val.toString()) : def;
	}

	public long getLong(String key, long def) {
		return Long.parseLong(getString(key, String.valueOf(def)));
	}

	public boolean getBoolean(String key, boolean def) {
		Object val = get(key, def);
		return val != null ? Boolean.parseBoolean(val.toString()) : def;
	}

	public <T> List<T> getList(String key, Class<T> t) {
		Document found = getDocument();
		if (found == null) {
			return new ArrayList<>();
		}
		return found.getList(key, t);
	}

	public boolean remove(String id) {
		Document query = new Document("_id", id);
		Document found = collection.find(query).first();

		if (found == null) {
			return false;
		}

		collection.deleteOne(query);
		return true;
	}

	public void insertOrUpdate(String key, Object value, boolean async) {
		Runnable task = () -> {
			if (exists()) {
				collection.updateOne(Filters.eq("_id", id), Updates.set(key, value));
			} else {
				Document New = new Document("_id", id).append(key, value);
				collection.insertOne(New);
			}
		};

		if (async) {
			com.zerosio.utility.Utilities.runAsync(task);
		} else {
			task.run();
		}
	}

	public boolean exists() {
		return getDocument() != null;
	}

	public int getOnlineCount() {
		return getInt("onlineCount", 0);
	}

	public void incrementOnline() {
		collection.updateOne(
			Filters.eq("_id", id),
			Updates.inc("onlineCount", 1)
		);
	}

	public void decrementOnline() {
		collection.updateOne(
			Filters.eq("_id", id),
			Updates.inc("onlineCount", -1)
		);
	}

	public void resetOnlineCount() {
		collection.updateOne(
			Filters.eq("_id", id),
			Updates.set("onlineCount", 0)
		);
	}

	public static void resetAllOnlineCounts() {
		collection.updateMany(
			new Document(),
			Updates.set("onlineCount", 0)
		);
	}

}
