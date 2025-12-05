package net.calyro.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class DatabaseManager {
    public static MongoCollection<Document> bans;
    public static MongoCollection<Document> punishments;

    public static void init() {
        MongoDatabase database = MongoManager.getDatabase();
        bans = database.getCollection("bans");
        punishments = database.getCollection("punishments");
    }
}