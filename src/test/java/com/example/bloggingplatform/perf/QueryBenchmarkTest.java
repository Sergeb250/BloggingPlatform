package com.example.bloggingplatform.perf;

import com.example.bloggingplatform.config.DBConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 4 query benchmark (requirement 4.1-a): the app's post queries timed on 100,000 posts,
 * without and with the indexes of {@code benchmark/indexes.sql}.
 * <p>
 * Steps: build the separate benchmark database ({@code benchmark/bulk_data.sql}, in the test resources), drop the three indexes,
 * time every query (3 warm-up runs, then the median of 15 runs) and save its EXPLAIN ANALYZE
 * plan, create the indexes, and measure again. The results go to {@code target/benchmark/}.
 * <p>
 * It takes a few minutes, so it only runs when asked:
 * {@code .\mvnw.cmd test -Dtest=QueryBenchmarkTest -Dbenchmark=true}
 */
@EnabledIfSystemProperty(named = "benchmark", matches = "true")
class QueryBenchmarkTest {

    private static final int WARM_UP = 3;
    private static final int RUNS = 15;
    private static final Path OUT = Path.of("target", "benchmark");
    private static final List<String> INDEXES = List.of("idx_posts_status_created", "idx_posts_author_created", "idx_posts_title");

    /** The same SELECT as PostDAOImpl: a post with its author, comment count and average rating. */
    private static final String SELECT_POSTS = """
            SELECT p.post_id, p.author_id, u.username AS author_name, p.title, p.content, p.status,
                   p.created_at, p.updated_at,
                   (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.post_id) AS comment_count,
                   (SELECT AVG(r.rating) FROM reviews r WHERE r.post_id = p.post_id) AS average_rating
            FROM posts p
            JOIN users u ON u.user_id = p.author_id
            """;

    /** The queries the app runs most, with the values a user would give them. */
    private static final Map<String, String> QUERIES = new LinkedHashMap<>();

    static {
        QUERIES.put("Discover: first page, newest first",
                SELECT_POSTS + " WHERE p.status = 'PUBLISHED' ORDER BY p.created_at DESC, p.post_id DESC LIMIT 6 OFFSET 0");
        QUERIES.put("Discover: count for the page numbers",
                "SELECT COUNT(*) FROM posts p WHERE p.status = 'PUBLISHED'");
        QUERIES.put("My posts: one author, newest first",
                SELECT_POSTS + " WHERE p.author_id = 42 ORDER BY p.created_at DESC, p.post_id DESC LIMIT 10 OFFSET 0");
        QUERIES.put("Drop-down: posts in title order",
                "SELECT p.post_id, p.title FROM posts p ORDER BY p.title LIMIT 20");
        QUERIES.put("Keyword search LIKE '%...%' (control)",
                "SELECT COUNT(*) FROM posts p WHERE p.status = 'PUBLISHED' AND (p.title LIKE '%topic 7.%' OR p.content LIKE '%topic 7.%')");
    }

    @Test
    void postQueriesWithoutAndWithIndexes() throws Exception {
        runScript(Path.of("src", "test", "resources", "benchmark", "bulk_data.sql"), serverUrl());

        Map<String, Double> before;
        Map<String, Double> after;
        StringBuilder plans = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(serverUrl() + "bloggingPlatform_perf", DBConnection.USER, DBConnection.PASSWORD)) {
            for (String index : INDEXES) {
                dropIndexIfExists(conn, index);
            }
            before = measure(conn, plans, "WITHOUT the Phase 4 indexes");

            for (String sql : readStatements(Path.of("src", "test", "resources", "benchmark", "indexes.sql"))) {
                execute(conn, sql);
            }
            execute(conn, "ANALYZE TABLE posts");
            after = measure(conn, plans, "WITH the Phase 4 indexes");
        }

        StringBuilder report = new StringBuilder();
        report.append("Benchmark database bloggingPlatform_perf: 100,000 posts (80,000 published), 200,000 comments, 100,000 reviews\n");
        report.append("Median of ").append(RUNS).append(" runs after ").append(WARM_UP).append(" warm-up runs, in milliseconds\n\n");
        report.append(String.format("%-42s %12s %12s %9s%n", "query", "no index", "with index", "faster"));
        for (String name : QUERIES.keySet()) {
            report.append(String.format("%-42s %9.2f ms %9.2f ms %8.1fx%n",
                    name, before.get(name), after.get(name), before.get(name) / after.get(name)));
        }
        Files.createDirectories(OUT);
        Files.writeString(OUT.resolve("benchmark-results.txt"), report.toString());
        Files.writeString(OUT.resolve("explain-analyze.txt"), plans.toString());

        String discover = "Discover: first page, newest first";
        assertTrue(after.get(discover) < before.get(discover), "the index must make Discover's first page faster");
    }

    /** Median time of every query, and its EXPLAIN ANALYZE plan added to {@code plans}. */
    private static Map<String, Double> measure(Connection conn, StringBuilder plans, String title) throws SQLException {
        Map<String, Double> medians = new LinkedHashMap<>();
        plans.append("==================== ").append(title).append(" ====================\n\n");
        for (Map.Entry<String, String> query : QUERIES.entrySet()) {
            medians.put(query.getKey(), medianMillis(conn, query.getValue()));
            plans.append("-- ").append(query.getKey()).append('\n').append(explainAnalyze(conn, query.getValue())).append("\n\n");
        }
        return medians;
    }

    private static double medianMillis(Connection conn, String sql) throws SQLException {
        for (int i = 0; i < WARM_UP; i++) {
            readAllRows(conn, sql);   // warm-up: fills MySQL's buffer pool and the JDBC driver's caches
        }
        List<Double> times = new ArrayList<>();
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            readAllRows(conn, sql);
            times.add((System.nanoTime() - start) / 1_000_000.0);
        }
        times.sort(null);
        return times.get(times.size() / 2);
    }

    private static void readAllRows(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rs.getString(1);
            }
        }
    }

    /** MySQL 8 runs the query and prints the plan with the real time and rows of every step. */
    private static String explainAnalyze(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("EXPLAIN ANALYZE " + sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getString(1);
        }
    }

    private static void dropIndexIfExists(Connection conn, String index) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'posts' AND INDEX_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, index);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getLong(1) > 0) {
                    execute(conn, "DROP INDEX " + index + " ON posts");
                }
            }
        }
    }

    /** Runs every statement of a .sql file (statements end with ';', lines starting with -- are comments). */
    private static void runScript(Path file, String url) throws SQLException, IOException {
        try (Connection conn = DriverManager.getConnection(url, DBConnection.USER, DBConnection.PASSWORD)) {
            for (String sql : readStatements(file)) {
                execute(conn, sql);
            }
        }
    }

    private static List<String> readStatements(Path file) throws IOException {
        StringBuilder text = new StringBuilder();
        for (String line : Files.readAllLines(file)) {
            if (!line.strip().startsWith("--")) {
                text.append(line).append('\n');
            }
        }
        List<String> statements = new ArrayList<>();
        for (String sql : text.toString().split(";")) {
            if (!sql.isBlank()) {
                statements.add(sql.strip());
            }
        }
        return statements;
    }

    private static void execute(Connection conn, String sql) throws SQLException {
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
        }
    }

    /** The MySQL server address from DBConnection without the database name, e.g. jdbc:mysql://localhost:3306/ */
    private static String serverUrl() {
        return DBConnection.URL.substring(0, DBConnection.URL.lastIndexOf('/') + 1);
    }
}
