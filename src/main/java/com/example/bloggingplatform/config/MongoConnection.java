package com.example.bloggingplatform.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import java.util.concurrent.TimeUnit;

/** Connection to MongoDB Atlas, the NoSQL database that stores comments as documents (Phase 4). */
public class MongoConnection {

    public static final String URI = "mongodb+srv://<user>:<password>@cluster0.78weq9k.mongodb.net/";   // your Atlas connection string
    public static final String DATABASE = "bloggingPlatform";

    private static MongoClient client;   // one client for the whole app: it keeps its own pool of connections

    public static MongoDatabase getDatabase() {
        if (client == null) {
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(URI))
                    .applyToClusterSettings(cluster -> cluster.serverSelectionTimeout(10, TimeUnit.SECONDS))   // fail fast when offline
                    .build();
            client = MongoClients.create(settings);
        }
        return client.getDatabase(DATABASE);
    }
}
