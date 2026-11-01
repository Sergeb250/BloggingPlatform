package com.example.bloggingplatform.service;

import com.example.bloggingplatform.cache.PostCache;
import com.example.bloggingplatform.dao.DaoException;
import com.example.bloggingplatform.model.CacheStats;
import com.example.bloggingplatform.model.Comment;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostFilter;
import com.example.bloggingplatform.model.PostSort;
import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Review;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.Tag;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.util.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The {@link PostCache} (cache/PostCache) together with the services that empty it. The fake post
 * DAO counts {@code getAllPosts} calls: every call is a cache miss that went to the "database".
 */
class PostCacheTest {

    private final FakeDaos.Users users = new FakeDaos.Users();
    private final FakeDaos.Posts posts = new FakeDaos.Posts();
    private final FakeDaos.Comments comments = new FakeDaos.Comments();
    private final FakeDaos.Reviews reviews = new FakeDaos.Reviews();
    private final FakeDaos.Tags tags = new FakeDaos.Tags(posts);
    private final PostCache cache = new PostCache(posts);
    private final Session session = FakeDaos.adminSession();

    private final TagService tagService = new TagService(tags, session, cache);
    private final PostService postService = new PostService(posts, tagService, users, session, cache);
    private final CommentService commentService = new CommentService(comments, posts, users, session, cache);
    private final ReviewService reviewService = new ReviewService(reviews, posts, users, session, cache);
    private final UserService userService = new UserService(users, new PasswordHasher(1_000), session, cache);

    private final User author = users.add("alice", Role.AUTHOR);
    private final User reader = users.add("bob", Role.READER);
    private final Post first = posts.add(author, "First", PostStatus.PUBLISHED);
    private final Post draft = posts.add(author, "Draft", PostStatus.DRAFT);
    private final Post second = posts.add(author, "Second", PostStatus.PUBLISHED);

    @Test
    void theFirstReadLoadsFromTheDatabaseAndLaterReadsUseMemory() {
        discover();
        discover();
        postService.findById(first.getId());

        assertEquals(1, posts.getAllPostsCalls);
    }

    @Test
    void onlyPublishedPostsAreCachedNewestFirst() {
        assertEquals(List.of(second, first), cache.getPublished());
        assertEquals(first, cache.getPublished(first.getId()));
        assertNull(cache.getPublished(draft.getId()));   // drafts always come from the database
    }

    @Test
    void statisticsCountHitsMissesAndCachedPosts() {
        assertEquals(0, cache.getStats().getHitRate(), "nothing read yet");

        discover();                          // first read: a miss (loads from the database)
        discover();                          // a hit
        postService.findById(first.getId()); // a hit
        CacheStats stats = cache.getStats();

        assertEquals(1, stats.getMisses());
        assertEquals(2, stats.getHits());
        assertEquals(2, stats.getCachedPosts());
        assertEquals(100.0 * 2 / 3, stats.getHitRate(), 0.001);

        cache.clear();
        assertEquals(0, cache.getStats().getCachedPosts(), "cleared: nothing in memory until the next read");
    }

    @Test
    void clearMakesTheNextReadLoadAgain() {
        discover();
        cache.clear();
        discover();

        assertEquals(2, posts.getAllPostsCalls);
    }

    @Test
    void aFailedLoadIsNotKeptSoTheNextReadTriesAgain() {
        posts.databaseDown = true;
        assertThrows(DaoException.class, this::discover);

        posts.databaseDown = false;
        assertEquals(2, discover().size());
    }

    @Test
    void changingAPostEmptiesTheCache() {
        assertEmptiesCache(() -> postService.create(author.getId(), "Third", "A body of at least twenty characters.",
                PostStatus.PUBLISHED, List.of()));
        assertEmptiesCache(() -> postService.update(first.getId(), author.getId(), "First, edited",
                "A body of at least twenty characters.", PostStatus.PUBLISHED, List.of()));
        assertEmptiesCache(() -> postService.changeStatus(draft.getId(), PostStatus.PUBLISHED));
        assertEmptiesCache(() -> postService.delete(second.getId()));
    }

    @Test
    void addingOrDeletingACommentEmptiesTheCache() {
        Comment[] added = new Comment[1];
        assertEmptiesCache(() -> added[0] = commentService.create(first.getId(), reader.getId(), "Nice post!"));
        assertEmptiesCache(() -> commentService.delete(added[0].getId()));
    }

    @Test
    void everyReviewChangeEmptiesTheCache() {
        Review[] added = new Review[1];
        assertEmptiesCache(() -> added[0] = reviewService.create(first.getId(), reader.getId(), 4, "Good"));
        assertEmptiesCache(() -> reviewService.update(added[0].getId(), 5, "Even better"));
        assertEmptiesCache(() -> reviewService.delete(added[0].getId()));
    }

    @Test
    void renamingOrDeletingATagEmptiesTheCache() {
        Tag tag = tagService.create("java");
        assertEmptiesCache(() -> tagService.rename(tag.getId(), "jvm"));
        assertEmptiesCache(() -> tagService.delete(tag.getId()));
    }

    @Test
    void editingOrDeletingAUserEmptiesTheCache() {
        assertEmptiesCache(() -> userService.update(author.getId(), "alicia", "alice@example.com", "", Role.AUTHOR));
        assertEmptiesCache(() -> userService.delete(author.getId()));
    }

    @Test
    void changesThatPostsDoNotShowKeepTheCache() {
        Comment comment = commentService.create(first.getId(), reader.getId(), "Nice post!");
        discover();
        int loads = posts.getAllPostsCalls;

        commentService.update(comment.getId(), "Nice post, thanks!");     // the text is not shown on a post card
        tagService.create("unused");                                       // a new tag is on no post yet
        userService.create("carol", "carol@example.com", "Secret#1", Role.READER);
        discover();

        assertEquals(loads, posts.getAllPostsCalls);
    }

    @Test
    void publishingADraftShowsItAndDeletingAPostHidesIt() {
        assertEquals(List.of(second, first), discover());

        postService.changeStatus(draft.getId(), PostStatus.PUBLISHED);
        assertEquals(3, discover().size());

        postService.delete(first.getId());
        assertEquals(2, discover().size());
    }

    /** Fills the cache, runs the change, and checks that the next read had to load again. */
    private void assertEmptiesCache(Runnable change) {
        discover();
        int loads = posts.getAllPostsCalls;
        change.run();
        discover();
        assertEquals(loads + 1, posts.getAllPostsCalls, "the change should empty the cache");
    }

    private List<Post> discover() {
        return postService.discover(new PostFilter(PostStatus.PUBLISHED, null, null, null), PostSort.NEWEST, 0, 50).getItems();
    }
}
