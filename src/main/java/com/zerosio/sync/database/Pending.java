package com.zerosio.sync.database;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.zerosio.database.DatabaseManager;
import com.zerosio.utility.StringUtils;

import org.bson.Document;

public class Pending {

    private static final MongoCollection<Document> collection = DatabaseManager.pending;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static String createRequest(String username, UUID uuid) {
        collection.deleteMany(Filters.eq("uuid", uuid.toString()));

        String code = StringUtils.random(6);

        Document doc = new Document("code", code)
                .append("uuid", uuid.toString())
                .append("username", username)
                .append("createdAt", System.currentTimeMillis());

        collection.insertOne(doc);

        // delete after 30s
        scheduler.schedule(() -> collection.deleteMany(Filters.eq("uuid", uuid.toString())),
                30, TimeUnit.SECONDS);

        return code;
    }

    public static boolean isPending(UUID uuid) {
        return collection.find(Filters.eq("uuid", uuid.toString())).first() != null;
    }

    public static Document getByCode(String code) {
        return collection.find(Filters.eq("code", code)).first();
    }

    public static void removeByUUID(UUID uuid) {
        collection.deleteMany(Filters.eq("uuid", uuid.toString()));
    }
}
