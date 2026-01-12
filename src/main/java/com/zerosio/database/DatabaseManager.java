package com.zerosio.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class DatabaseManager {
    public static MongoCollection<Document> bans;
    public static MongoCollection<Document> punishments;
    public static MongoCollection<Document> pending;
    public static MongoCollection<Document> synced;

    public static void init() {
        MongoDatabase database = MongoManager.getDatabase();
        MongoDatabase sdb = MongoManager.getSyncDatabase();
        bans = database.getCollection("bans");
        punishments = database.getCollection("punishments");
        pending = sdb.getCollection("pending");
        synced = sdb.getCollection("synced");
    }
}