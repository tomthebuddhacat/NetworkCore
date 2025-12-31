package com.zerosio.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.util.UUID;

public class Punishment {
	private static MongoCollection<Document> getCollection() {
		return MongoManager.getDatabase().getCollection("punishments");
	}

	public static void addBan(UUID uuid, String ipAddress, String name, String reason, long length, String banId) {
		Document ban = new Document("_id", uuid.toString())
		.append("uuid", uuid.toString())
		.append("ip", ipAddress)
		.append("name", name)
		.append("type", "BAN")
		.append("reason", reason)
		.append("length", length)
		.append("id", banId)
		.append("timestamp", System.currentTimeMillis())
		.append("active", true);

		getCollection().replaceOne(
			Filters.eq("_id", uuid.toString()),
			ban,
			new ReplaceOptions().upsert(true));
	}

	public static boolean isBanned(UUID uuid, String ipAddress) {
		Document doc = getCollection().find(Filters.and(
												Filters.or(
														Filters.eq("uuid", uuid.toString()),
														Filters.eq("ip", ipAddress)
												),
												Filters.eq("type", "BAN"),
												Filters.eq("active", true)
											)).first();

		if (doc == null)
			return false;

		long length = doc.getLong("length");
		long timestamp = doc.getLong("timestamp");

		if (length == -1)
			return true;

		long expiry = timestamp + (length * 1000L);
		if (System.currentTimeMillis() > expiry) {
			unban(uuid, ipAddress);
			return false;
		}
		return true;
	}

	public static void unban(UUID uuid, String ipAddress) {
		getCollection().updateMany(
			Filters.and(
				Filters.or(
					Filters.eq("uuid", uuid.toString()),
					Filters.eq("ip", ipAddress)
				),
				Filters.eq("type", "BAN"),
				Filters.eq("active", true)
			),
			new Document("$set", new Document("active", false))
		);
	}

	public static Document getActiveBan(UUID uuid, String ipAddress) {
		return getCollection().find(Filters.and(
										Filters.or(
											Filters.eq("uuid", uuid.toString()),
											Filters.eq("ip", ipAddress)
										),
										Filters.eq("type", "BAN"),
										Filters.eq("active", true)
									)).first();
	}

	public static void addMute(UUID uuid, String name, String reason, long length, String muteId) {
		Document mute = new Document("_id", uuid.toString() + "-MUTE")
		.append("uuid", uuid.toString())
		.append("name", name)
		.append("type", "MUTE")
		.append("reason", reason)
		.append("length", length)
		.append("id", muteId)
		.append("timestamp", System.currentTimeMillis())
		.append("active", true);

		getCollection().replaceOne(
			Filters.eq("_id", uuid.toString() + "-MUTE"),
			mute,
			new ReplaceOptions().upsert(true));
	}

	public static boolean isMuted(UUID uuid) {
		Document doc = getCollection().find(Filters.and(
												Filters.eq("uuid", uuid.toString()),
												Filters.eq("type", "MUTE"),
												Filters.eq("active", true)
											)).first();

		if (doc == null)
			return false;

		long length = doc.getLong("length");
		long timestamp = doc.getLong("timestamp");

		if (length == -1)
			return true;

		long expiry = timestamp + (length * 1000L);
		if (System.currentTimeMillis() > expiry) {
			unmute(uuid);
			return false;
		}
		return true;
	}

	public static void unmute(UUID uuid) {
		getCollection().updateOne(
			Filters.and(
				Filters.eq("uuid", uuid.toString()),
				Filters.eq("type", "MUTE"),
				Filters.eq("active", true)
			),
			new Document("$set", new Document("active", false))
		);
	}

	public static Document getActiveMute(UUID uuid) {
		return getCollection().find(Filters.and(
										Filters.eq("uuid", uuid.toString()),
										Filters.eq("type", "MUTE"),
										Filters.eq("active", true)
									)).first();
	}

	public static void unbanByName(String name) {
		getCollection().updateMany(
			Filters.and(
				Filters.eq("name", name),
				Filters.eq("type", "BAN"),
				Filters.eq("active", true)
			),
			new Document("$set", new Document("active", false))
		);
	}

	public static void unmuteByName(String name) {
		getCollection().updateMany(
			Filters.and(
				Filters.eq("name", name),
				Filters.eq("type", "MUTE"),
				Filters.eq("active", true)
			),
			new Document("$set", new Document("active", false))
		);
	}

}
