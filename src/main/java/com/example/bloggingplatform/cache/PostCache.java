package com.example.bloggingplatform.cache;

import com.example.bloggingplatform.algorithms.MergeSort;
import com.example.bloggingplatform.dao.PostDAO;
import com.example.bloggingplatform.model.CacheStats;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostSort;
import com.example.bloggingplatform.model.PostStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps the published posts in memory, so Discover and the reading view do not ask MySQL every time.
 * <p>
 * The first read loads everything from the database (a cache miss); later reads use memory (a hit).
 * Every change to a post, or to something shown with it (author name, tags, comments, ratings),
 * calls {@link #clear()}, so the next read loads fresh data and the screens never show old values.
 */
public class PostCache {

    private final PostDAO posts;

    private List<Post> published;        // newest first; null means "not loaded" (empty cache)
    private Map<Integer, Post> byId;     // post id → post, so one post is found in O(1)
    private SearchIndex index;           // word → post ids, for the search bar

    private int hits;                    // reads answered from memory
    private int misses;                  // reads that had to load from the database
    private long lastLoadMillis;         // how long the last load took

    public PostCache(PostDAO posts) {
        this.posts = posts;
    }

    /** Every published post, newest first. The list cannot be changed. */
    public List<Post> getPublished() {
        loadIfEmpty();
        return published;
    }

    /** The published post with this id, or null (drafts are never cached). */
    public Post getPublished(int id) {
        loadIfEmpty();
        return byId.get(id);
    }

    /** The ids of the published posts matching the search text (see {@link SearchIndex#search}). */
    public Set<Integer> search(String text) {
        loadIfEmpty();
        return index.search(text);
    }

    /** Empties the cache; the next read loads everything again from the database. */
    public void clear() {
        published = null;
        byId = null;
        index = null;
    }

    /** Hits, misses, cached posts and the last load time, for the dashboard (Phase 4). */
    public CacheStats getStats() {
        int cachedPosts = published == null ? 0 : published.size();
        return new CacheStats(hits, misses, cachedPosts, lastLoadMillis);
    }

    private void loadIfEmpty() {
        if (published != null) {
            hits++;
            return;
        }
        misses++;
        long start = System.nanoTime();
        List<Post> list = new ArrayList<>();
        for (Post post : posts.getAllPosts()) {
            if (post.getStatus() == PostStatus.PUBLISHED) {
                list.add(post);
            }
        }
        MergeSort.sort(list, PostSort.NEWEST.comparator());

        Map<Integer, Post> ids = new HashMap<>();
        for (Post post : list) {
            ids.put(post.getId(), post);
        }
        index = new SearchIndex(list);
        byId = ids;
        published = Collections.unmodifiableList(list);
        lastLoadMillis = (System.nanoTime() - start) / 1_000_000;
    }
}
