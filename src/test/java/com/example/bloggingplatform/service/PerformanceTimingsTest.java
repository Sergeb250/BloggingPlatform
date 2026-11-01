package com.example.bloggingplatform.service;

import com.example.bloggingplatform.cache.SearchIndex;
import com.example.bloggingplatform.config.DBConnection;
import com.example.bloggingplatform.dao.PostDAO;
import com.example.bloggingplatform.dao.impl.PostDAOImpl;
import com.example.bloggingplatform.dao.impl.UserDAOImpl;
import com.example.bloggingplatform.cache.PostCache;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostFilter;
import com.example.bloggingplatform.model.PostSort;
import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.Tag;
import com.example.bloggingplatform.model.User;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Phase 3 before / after timings (requirement 3.1-c). The results are written to
 * {@code target/phase-3-timings-*.txt} and copied to {@code docs/testing/phase-3/timings.txt}.
 * Only correctness is asserted: timings depend on the computer.
 */
class PerformanceTimingsTest {

    private static final int POSTS = 10_000;

    /** Before: check every post's text. After: look the words up in the search index. */
    @Test
    void searchIndexVersusCheckingEveryPost() throws IOException {
        List<Post> posts = generatedPosts();
        SearchIndex[] index = new SearchIndex[1];
        double buildMicros = averageMicros(5, () -> index[0] = new SearchIndex(posts));

        StringBuilder report = new StringBuilder();
        report.append("Keyword search over ").append(POSTS).append(" generated posts (about 60 words each)\n");
        report.append(String.format("%-14s %10s %18s %16s %9s%n", "query", "matches", "scan every post", "search index", "speed-up"));
        for (String query : List.of("w7x", "w1234x", "w42x w99x", "nothing")) {
            Set<Integer> scanned = scanEveryPost(posts, query);
            assertEquals(scanned, index[0].search(query), "the index must find exactly what a full scan finds");

            double scanMicros = averageMicros(20, () -> scanEveryPost(posts, query));
            double indexMicros = averageMicros(20, () -> index[0].search(query));
            report.append(String.format("%-14s %10d %15.0f µs %13.1f µs %8.0fx%n",
                    query, scanned.size(), scanMicros, indexMicros, scanMicros / indexMicros));
        }
        report.append(String.format("Building the index once (each time the cache loads): %.0f µs%n", buildMicros));
        save("search", report.toString());
    }

    /** Before: Phase 2 read Discover from MySQL every time. After: Phase 3 reads it from the cache. */
    @Test
    void cacheVersusDatabaseOnTheRealData() throws IOException {
        try (Connection connection = DBConnection.getConnection()) {
            Assumptions.assumeTrue(connection.isValid(2), "MySQL is not available");
        } catch (SQLException e) {
            Assumptions.abort("MySQL is not available: " + e.getMessage());
        }
        PostDAO postDao = new PostDAOImpl();
        PostCache cache = new PostCache(postDao);
        Session reader = FakeDaos.sessionOf(new User(Integer.MAX_VALUE, "timing", "timing@example.com", "x", Role.READER, null));
        PostService service = new PostService(postDao, null, new UserDAOImpl(), reader, cache);
        PostFilter firstPage = new PostFilter(PostStatus.PUBLISHED, null, null, null);
        PostFilter search = new PostFilter(PostStatus.PUBLISHED, null, null, "java");
        int someId = cache.getPublished().getFirst().getId();

        StringBuilder report = new StringBuilder();
        report.append("Real MySQL data: ").append(cache.getPublished().size()).append(" published posts\n");
        report.append(String.format("%-38s %18s %14s %9s%n", "operation", "MySQL (Phase 2)", "cache (hit)", "speed-up"));
        row(report, "Discover, first page",
                averageMicros(30, () -> service.findPage(firstPage, 0, 6)),
                averageMicros(30, () -> service.discover(firstPage, PostSort.NEWEST, 0, 6)));
        row(report, "Discover, search \"java\"",
                averageMicros(30, () -> service.findPage(search, 0, 6)),
                averageMicros(30, () -> service.discover(search, PostSort.NEWEST, 0, 6)));
        row(report, "Open one post (find by id)",
                averageMicros(30, () -> postDao.getPostById(someId)),
                averageMicros(30, () -> cache.getPublished(someId)));
        report.append(String.format("Cache miss (load every post + build the index): %.0f µs%n",
                averageMicros(10, () -> {
                    cache.clear();
                    cache.getPublished();
                })));
        save("cache", report.toString());
    }

    private static void row(StringBuilder report, String operation, double databaseMicros, double cacheMicros) {
        report.append(String.format("%-38s %15.0f µs %11.1f µs %8.0fx%n",
                operation, databaseMicros, cacheMicros, databaseMicros / cacheMicros));
    }

    /** The "before" search: lower-case every post's text and check that it contains each query word. */
    private static Set<Integer> scanEveryPost(List<Post> posts, String query) {
        String[] words = query.toLowerCase(Locale.ROOT).split(" ");
        Set<Integer> found = new HashSet<>();
        for (Post post : posts) {
            String text = (post.getTitle() + " " + post.getContent() + " " + post.getTagNames() + " "
                    + post.getAuthorName()).toLowerCase(Locale.ROOT);
            boolean all = true;
            for (String word : words) {
                if (!text.contains(word)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                found.add(post.getId());
            }
        }
        return found;
    }

    /**
     * Posts made of words like "w17x" from a 2,000-word vocabulary. The closing x means no word is
     * the start of another ("w1x" is not the start of "w17x"), so a scan and the index must agree.
     */
    private static List<Post> generatedPosts() {
        Random random = new Random(7);
        List<Post> posts = new ArrayList<>();
        for (int id = 1; id <= POSTS; id++) {
            StringBuilder content = new StringBuilder();
            for (int w = 0; w < 60; w++) {
                content.append('w').append(random.nextInt(2_000)).append("x ");
            }
            Post post = new Post(id, 1, "Post " + id, content.toString(), PostStatus.PUBLISHED,
                    List.of(new Tag(1, "tag" + random.nextInt(50))));
            post.setAuthorName("author" + random.nextInt(100));
            posts.add(post);
        }
        return posts;
    }

    /** Runs {@code work} a few times to warm up, then returns the average time of {@code repeats} runs. */
    private static double averageMicros(int repeats, Runnable work) {
        for (int i = 0; i < 3; i++) {
            work.run();   // warm-up: lets the JVM compile the code first
        }
        long start = System.nanoTime();
        for (int i = 0; i < repeats; i++) {
            work.run();
        }
        return (System.nanoTime() - start) / 1_000.0 / repeats;
    }

    private static void save(String name, String report) throws IOException {
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target", "phase-3-timings-" + name + ".txt"), report);
    }
}
