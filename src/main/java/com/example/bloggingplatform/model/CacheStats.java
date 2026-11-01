package com.example.bloggingplatform.model;

/** How the post cache is doing, for the dashboard: reads served from memory (hits) or the database (misses). */
public class CacheStats {

    private final int hits;
    private final int misses;
    private final int cachedPosts;
    private final long lastLoadMillis;   // how long the last load from MySQL took

    public CacheStats(int hits, int misses, int cachedPosts, long lastLoadMillis) {
        this.hits = hits;
        this.misses = misses;
        this.cachedPosts = cachedPosts;
        this.lastLoadMillis = lastLoadMillis;
    }

    public int getHits() {
        return hits;
    }

    public int getMisses() {
        return misses;
    }

    public int getCachedPosts() {
        return cachedPosts;
    }

    public long getLastLoadMillis() {
        return lastLoadMillis;
    }

    /** Percentage of reads served from memory, 0 to 100; 0 before the first read. */
    public double getHitRate() {
        int reads = hits + misses;
        return reads == 0 ? 0 : 100.0 * hits / reads;
    }
}
