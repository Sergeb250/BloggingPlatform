package com.example.bloggingplatform.dao;

import com.example.bloggingplatform.dao.impl.CommentDAOImpl;
import com.example.bloggingplatform.dao.impl.PostDAOImpl;
import com.example.bloggingplatform.dao.impl.ReviewDAOImpl;
import com.example.bloggingplatform.dao.impl.TagDAOImpl;
import com.example.bloggingplatform.dao.impl.UserDAOImpl;
import com.example.bloggingplatform.model.Comment;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostFilter;
import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Review;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.Tag;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.config.DBConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.example.bloggingplatform.model.PostStatus.DRAFT;
import static com.example.bloggingplatform.model.PostStatus.PUBLISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the DAOs against the real MySQL database set in {@code config/DBConnection}.
 * <p>
 * Every row gets a unique per-test prefix and is deleted afterwards (deleting the test users
 * cascades to their posts, comments, reviews and tag links), so existing data is untouched.
 * The tests are skipped, not failed, when MySQL is not reachable.
 */
class DAOIntegrationTest {

    private static final UserDAO users = new UserDAOImpl();
    private static final PostDAO posts = new PostDAOImpl();
    private static final TagDAO tags = new TagDAOImpl();
    private static final CommentDAO comments = new CommentDAOImpl();
    private static final ReviewDAO reviews = new ReviewDAOImpl();

    /** Unique, lower-case prefix for this test's rows, e.g. "itm3x9k2ab". */
    private final String run = "it" + Long.toString(System.nanoTime(), 36);
    private final List<Integer> createdUsers = new ArrayList<>();
    private final List<Integer> createdTags = new ArrayList<>();

    @BeforeAll
    static void connect() {
        try (Connection connection = DBConnection.getConnection()) {
            Assumptions.assumeTrue(connection.isValid(2), "MySQL is not available");
        } catch (SQLException e) {
            Assumptions.abort("MySQL is not available, skipping integration tests: " + e.getMessage());
        }
    }

    @AfterEach
    void cleanUp() {
        createdUsers.forEach(users::deleteUser);
        createdTags.forEach(tags::deleteTag);
    }

    // ---------- users ----------

    @Test
    void userCrudRoundTrip() {
        User user = newUser("crud", Role.READER);
        assertNotNull(user.getCreatedAt());

        assertTrue(users.updateUser(new User(user.getId(), user.getUsername(), "new-" + user.getEmail(),
                user.getPasswordHash(), Role.AUTHOR, null)));
        User reloaded = users.getUserById(user.getId());
        assertEquals(Role.AUTHOR, reloaded.getRole());
        assertEquals("new-" + user.getEmail(), reloaded.getEmail());

        assertTrue(users.deleteUser(user.getId()));
        assertNull(users.getUserById(user.getId()));
        assertFalse(users.deleteUser(user.getId()));
    }

    @Test
    void duplicateUsernameIsStoppedByTheUniqueKey() {
        User user = newUser("dup", Role.READER);

        DaoException e = assertThrows(DaoException.class, () -> users.addUser(
                new User(0, user.getUsername(), "other-" + user.getEmail(), "x".repeat(64), Role.READER, null)));

        assertTrue(e.getMessage().contains("already exists"), e.getMessage());
    }

    @Test
    void usernameUniquenessIgnoresCase() {
        User user = newUser("case", Role.READER);

        DaoException e = assertThrows(DaoException.class, () -> users.addUser(new User(0,
                user.getUsername().toUpperCase(), "upper-" + user.getEmail(), "x".repeat(64), Role.READER, null)));

        assertTrue(e.getMessage().contains("already exists"), e.getMessage());
    }

    // ---------- posts and tags ----------

    @Test
    void postIsSavedAndUpdatedWithItsTags() {
        User author = newUser("author", Role.AUTHOR);
        Tag a = newTag("a");
        Tag b = newTag("b");

        Post post = newPost(author, run + " title", "body", DRAFT, List.of(a, b));
        assertEquals(List.of(a, b), post.getTags());
        assertEquals(author.getUsername(), post.getAuthorName());
        assertEquals(0, post.getCommentCount());
        assertNull(post.getAverageRating());

        assertTrue(posts.updatePost(new Post(post.getId(), author.getId(), run + " new title", "new body", PUBLISHED, List.of(b))));
        Post updated = posts.getPostById(post.getId());
        assertEquals(List.of(b), updated.getTags());
        assertEquals(run + " new title", updated.getTitle());
        assertEquals(PUBLISHED, updated.getStatus());
        assertFalse(updated.getUpdatedAt().isBefore(updated.getCreatedAt()));
    }

    @Test
    void failedTagLinkRollsBackThePostInsert() {
        User author = newUser("tx", Role.AUTHOR);
        Tag missing = new Tag(Integer.MAX_VALUE, "missing");

        DaoException e = assertThrows(DaoException.class, () -> posts.addPost(
                new Post(0, author.getId(), run + " tx", "body", DRAFT, List.of(missing))));

        assertTrue(e.getMessage().startsWith("Could not save the post."), e.getMessage());
        assertEquals(0, posts.countPosts(byAuthor(author)), "the post row must be rolled back");
    }

    @Test
    void findPageAppliesEveryFilter() {
        User author = newUser("filters", Role.AUTHOR);
        User other = newUser("other", Role.AUTHOR);
        Tag tag = newTag("t");
        String word = run + "kw";
        newPost(author, run + " one " + word, "a", PUBLISHED, List.of(tag));
        newPost(author, run + " two", "body with " + word.toUpperCase(), PUBLISHED, List.of());
        newPost(author, run + " three", "c", DRAFT, List.of(tag));
        newPost(other, run + " four " + word, "d", PUBLISHED, List.of(tag));

        assertEquals(3, posts.countPosts(byAuthor(author)));
        assertEquals(2, posts.countPosts(new PostFilter(PUBLISHED, author.getId(), null, null)));
        assertEquals(2, posts.countPosts(new PostFilter(null, author.getId(), tag.getId(), null)));
        assertEquals(3, posts.countPosts(new PostFilter(null, null, null, word)), "keyword search is case-insensitive");
        assertEquals(1, posts.countPosts(new PostFilter(PUBLISHED, author.getId(), tag.getId(), word)));
        assertEquals(1, posts.getPostsPage(new PostFilter(PUBLISHED, author.getId(), tag.getId(), word), 0, 10).size());
    }

    @Test
    void pagesAreNewestFirstAndDoNotOverlap() {
        User author = newUser("pages", Role.AUTHOR);
        for (int i = 1; i <= 5; i++) {
            newPost(author, run + " post " + i, "body", PUBLISHED, List.of());
        }

        List<Post> first = posts.getPostsPage(byAuthor(author), 0, 2);
        List<Post> second = posts.getPostsPage(byAuthor(author), 2, 2);
        List<Post> third = posts.getPostsPage(byAuthor(author), 4, 2);

        assertEquals(List.of(2, 2, 1), List.of(first.size(), second.size(), third.size()));
        assertEquals(run + " post 5", first.getFirst().getTitle());
        assertEquals(5, Stream.of(first, second, third).flatMap(List::stream).map(Post::getId).distinct().count());
    }

    @Test
    void keywordIsBoundAsDataNotAsSql() {
        User author = newUser("inject", Role.AUTHOR);
        newPost(author, run + " safe", "body", PUBLISHED, List.of());

        assertEquals(0, posts.countPosts(new PostFilter(null, author.getId(), null, "' OR '1'='1")));
        assertEquals(0, posts.getPostsPage(new PostFilter(null, null, null, "x'); DROP TABLE posts; --"), 0, 10).size());
        assertEquals(1, posts.countPosts(byAuthor(author)), "the posts table is still there");
    }

    @Test
    void likeWildcardsInAKeywordMatchLiterally() {
        User author = newUser("like", Role.AUTHOR);
        newPost(author, run + " 100% java", "body", PUBLISHED, List.of());
        newPost(author, run + " 1000 java", "body", PUBLISHED, List.of());

        assertEquals(1, posts.countPosts(new PostFilter(null, author.getId(), null, "100%")));
        assertEquals(0, posts.countPosts(new PostFilter(null, author.getId(), null, "1_0")));
    }

    // ---------- comments and reviews ----------

    @Test
    void commentsAndReviewsAreSavedAndCounted() {
        User author = newUser("agg", Role.AUTHOR);
        User reader = newUser("r1", Role.READER);
        User reader2 = newUser("r2", Role.READER);
        Post post = newPost(author, run + " agg", "body", PUBLISHED, List.of());

        int commentId = comments.addComment(new Comment(0, post.getId(), reader.getId(), "Nice"));
        reviews.addReview(new Review(0, post.getId(), reader.getId(), 4, "Good"));
        reviews.addReview(new Review(0, post.getId(), reader2.getId(), 5, null));

        Post reloaded = posts.getPostById(post.getId());
        assertEquals(1, reloaded.getCommentCount());
        assertEquals(4.5, reloaded.getAverageRating(), 1e-9);

        assertTrue(comments.updateComment(new Comment(commentId, post.getId(), reader.getId(), "Edited")));
        Comment comment = comments.getCommentById(commentId);
        assertEquals("Edited", comment.getContent());
        assertEquals(post.getTitle(), comment.getPostTitle());
        assertEquals(reader.getUsername(), comment.getUsername());
        assertEquals(2, reviews.getReviewsByPost(post.getId()).size());
        assertTrue(reviews.getReviewsByPost(post.getId()).stream().anyMatch(review -> review.getReviewText() == null));
    }

    @Test
    void secondReviewBySameUserIsStoppedByTheUniqueKey() {
        User author = newUser("rev", Role.AUTHOR);
        User reader = newUser("rv", Role.READER);
        Post post = newPost(author, run + " rev", "body", PUBLISHED, List.of());
        reviews.addReview(new Review(0, post.getId(), reader.getId(), 3, null));

        DaoException e = assertThrows(DaoException.class,
                () -> reviews.addReview(new Review(0, post.getId(), reader.getId(), 5, null)));

        assertTrue(e.getMessage().contains("already exists"), e.getMessage());
    }

    @Test
    void ratingOutsideOneToFiveIsStoppedByTheCheckConstraint() {
        User author = newUser("chk", Role.AUTHOR);
        User reader = newUser("ck", Role.READER);
        Post post = newPost(author, run + " chk", "body", PUBLISHED, List.of());

        DaoException e = assertThrows(DaoException.class,
                () -> reviews.addReview(new Review(0, post.getId(), reader.getId(), 9, null)));

        assertTrue(e.getMessage().startsWith("Could not save the review."), e.getMessage());
    }

    @Test
    void commentOnAMissingPostIsStoppedByTheForeignKey() {
        User reader = newUser("orphan", Role.READER);

        DaoException e = assertThrows(DaoException.class,
                () -> comments.addComment(new Comment(0, Integer.MAX_VALUE, reader.getId(), "Hello?")));

        assertTrue(e.getMessage().contains("missing or still in use"), e.getMessage());
    }

    @Test
    void deletingAPostCascadesButKeepsTheTag() {
        User author = newUser("cascade", Role.AUTHOR);
        User reader = newUser("cr", Role.READER);
        Tag tag = newTag("keep");
        Post post = newPost(author, run + " cascade", "body", PUBLISHED, List.of(tag));
        comments.addComment(new Comment(0, post.getId(), reader.getId(), "Bye"));
        reviews.addReview(new Review(0, post.getId(), reader.getId(), 2, null));

        assertTrue(posts.deletePost(post.getId()));

        assertTrue(comments.getCommentsByPost(post.getId()).isEmpty());
        assertTrue(reviews.getReviewsByPost(post.getId()).isEmpty());
        assertNotNull(tags.getTagById(tag.getId()));
    }

    @Test
    void deletingAUserCascadesToTheirPosts() {
        User author = newUser("gone", Role.AUTHOR);
        Post post = newPost(author, run + " gone", "body", PUBLISHED, List.of());

        assertTrue(users.deleteUser(author.getId()));

        assertNull(posts.getPostById(post.getId()));
    }

    @Test
    void anAuthorsTagsAreTheTagsOnTheirPosts() {
        User author = newUser("tagger", Role.AUTHOR);
        User other = newUser("othertagger", Role.AUTHOR);
        Tag mine = newTag("mine");
        Tag theirs = newTag("theirs");
        newPost(author, run + " mine 1", "body", DRAFT, List.of(mine));
        newPost(author, run + " mine 2", "body", PUBLISHED, List.of(mine));
        newPost(other, run + " theirs", "body", PUBLISHED, List.of(theirs));

        assertEquals(List.of(mine), tags.getTagsUsedBy(author.getId()));
        assertEquals(List.of(theirs), tags.getTagsUsedBy(other.getId()));
        assertEquals(mine, tags.getTagByName(mine.getName()));
        assertNull(tags.getTagByName(run + "-nothing"));
    }

    // ---------- helpers ----------

    private User newUser(String suffix, Role role) {
        String name = run + "_" + suffix;
        int id = users.addUser(new User(0, name, name + "@test.example", "x".repeat(64), role, null));
        createdUsers.add(id);
        return users.getUserById(id);
    }

    private Tag newTag(String suffix) {
        String name = run + "-" + suffix;
        int id = tags.addTag(new Tag(0, name));
        createdTags.add(id);
        return new Tag(id, name);
    }

    private static Post newPost(User author, String title, String content, PostStatus status, List<Tag> tagList) {
        int id = posts.addPost(new Post(0, author.getId(), title, content, status, tagList));
        return posts.getPostById(id);
    }

    private static PostFilter byAuthor(User author) {
        return new PostFilter(null, author.getId(), null, null);
    }
}
