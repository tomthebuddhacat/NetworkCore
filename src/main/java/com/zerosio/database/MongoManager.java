package com.zerosio.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import com.zerosio.Config;

public class MongoManager {

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoDatabase syncDb;

    public static void connect() {
        if (mongoClient != null)
            return;

        String mongoUri = Config.getString("database.uri", "mongodb://localhost:27017");
        String dbName = Config.getString("database.name", "Test Database");

        ConnectionString connString = new ConnectionString(mongoUri);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connString)
                .build();

        mongoClient = MongoClients.create(settings);
        database = mongoClient.getDatabase(dbName);
        syncDb = mongoClient.getDatabase("sync");

        System.out.println("[MongoDB] Connected to database: " + dbName);
    }

    public static MongoDatabase getDatabase() {
        if (database == null) {
            throw new IllegalStateException("MongoManager is not connected! Call MongoManager.connect() first.");
        }
        return database;
    }
    
    public static MongoDatabase getSyncDatabase() {
    	if (syncDb == null) {
            throw new IllegalStateException("MongoManager is not connected! Call MongoManager.connect() first.");
        }
        
        return syncDb;
    }

    public static void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
            System.out.println("[MongoDB] Disconnected from MongoDB");
        }
    }
}