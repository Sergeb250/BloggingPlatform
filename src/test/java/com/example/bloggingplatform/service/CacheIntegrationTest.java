package com.example.bloggingplatform.service;

import com.example.bloggingplatform.config.DBConnection;
import com.example.bloggingplatform.dao.TagDAO;
import com.example.bloggingplatform.dao.UserDAO;
import com.example.bloggingplatform.dao.impl.TagDAOImpl;
import com.example.bloggingplatform.dao.impl.UserDAOImpl;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostFilter;
import com.example.bloggingplatform.model.PostSort;
import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.Tag;
import com.example.bloggingplatform.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Phase 3 end to end against the real MySQL database: Discover reads from the cache, and every
 * change made through the services shows up on the next read (cache invalidation). The rows use a
 * unique prefix and are deleted afterwards; the test is skipped when MySQL is not reachable.
 */
class CacheIntegrationTest {

    private final String run = "ca" + Long.toString(System.nanoTime(), 36);   // unique, lower-case word
    private final UserDAO userDao = new UserDAOImpl();
    private final TagDAO tagDao = new TagDAOImpl();
    private final AppContext app = AppContext.create();
    private final List<Integer> createdUsers = new ArrayList<>();
    private final List<Integer> createdTags = new ArrayList<>();
    private User admin;
    private User reader;

    @BeforeAll
    static void connect() {
        try (Connection connection = DBConnection.getConnection()) {
            Assumptions.assumeTrue(connection.isValid(2), "MySQL is not available");
        } catch (SQLException e) {
            Assumptions.abort("MySQL is not available, skipping integration tests: " + e.getMessage());
        }
    }

    @BeforeEach
    void signIn() {
        admin = newUser("adm", Role.ADMIN);
        reader = newUser("rdr", Role.READER);
        app.session().signIn(admin);
    }

    @AfterEach
    void cleanUp() {
        createdUsers.forEach(userDao::deleteUser);   // cascades to their posts, comments and reviews
        createdTags.forEach(tagDao::deleteTag);
    }

    @Test
    void discoverShowsTheLatestDataAfterEveryChange() {
        Post post = app.posts().create(admin.getId(), run + " caching", "A post written by the cache integration test.",
                PostStatus.PUBLISHED, List.of(run + "-tag"));
        Tag tag = post.getTags().getFirst();
        createdTags.add(tag.getId());

        Post shown = findOne(run);
        assertEquals(0, shown.getCommentCount());
        assertNull(shown.getAverageRating());

        app.comments().create(post.getId(), reader.getId(), "Nice post!");
        app.reviews().create(post.getId(), reader.getId(), 4, "Clear and short.");
        shown = findOne(run);
        assertEquals(1, shown.getCommentCount());
        assertEquals(4.0, shown.getAverageRating());

        app.tags().rename(tag.getId(), run + "-renamed");
        assertEquals(run + "-renamed", findOne(run).getTagNames());

        String newName = admin.getUsername() + "z";
        app.users().update(admin.getId(), newName, admin.getEmail(), "", Role.ADMIN);
        assertEquals(newName, findOne(run).getAuthorName());
        assertEquals(post, findOne(newName.toUpperCase()), "found by the new author name, ignoring case");

        app.posts().delete(post.getId());
        assertEquals(List.of(), find(run));
    }

    /** The published posts Discover finds for this search text, from the cache. */
    private List<Post> find(String text) {
        return app.posts().discover(new PostFilter(PostStatus.PUBLISHED, null, null, text), PostSort.NEWEST, 0, 50).getItems();
    }

    private Post findOne(String text) {
        List<Post> found = find(text);
        assertEquals(1, found.size(), "posts found for " + text);
        return found.getFirst();
    }

    private User newUser(String suffix, Role role) {
        String name = run + suffix;
        int id = userDao.addUser(new User(0, name, name + "@example.com", "x".repeat(64), role, null));
        createdUsers.add(id);
        return userDao.getUserById(id);
    }
}
