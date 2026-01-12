package com.zerosio.sync.database;

import java.time.Instant;
import java.util.UUID;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.zerosio.database.DatabaseManager;

import org.bson.Document;

public class Synced {

    private static final MongoCollection<Document> collection = DatabaseManager.synced;

    public static boolean isSynced(UUID uuid) {
        return collection.find(Filters.eq("uuid", uuid.toString())).first() != null;
    }
    
    public static boolean isSynced(String discordId) {
        return collection.find(Filters.eq("discordId", discordId)).first() != null;
    }

    public static void link(UUID uuid, String username, String discordId) {
        unlink(uuid);

        Document doc = new Document("uuid", uuid.toString())
                .append("username", username)
                .append("discordId", discordId)
                .append("syncedAtSysMilis", System.currentTimeMillis())
                .append("timestamp", Instant.now());

        collection.insertOne(doc);
    }

    public static void unlink(UUID uuid) {
        collection.deleteMany(Filters.eq("uuid", uuid.toString()));
    }
    
    public static void unlink(String discordId) {
        collection.deleteMany(Filters.eq("discordId", discordId));
    }

    public static String getDiscordId(UUID uuid) {
        Document doc = collection.find(Filters.eq("uuid", uuid.toString())).first();
        return doc != null ? doc.getString("discordId") : null;
    }
}
