package com.example.bloggingplatform.service;

import com.example.bloggingplatform.cache.PostCache;
import com.example.bloggingplatform.validations.ValidationException;
import com.example.bloggingplatform.model.Page;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostFilter;
import com.example.bloggingplatform.model.PostSort;
import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.Tag;
import com.example.bloggingplatform.model.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostServiceTest {

    private final FakeDaos.Users users = new FakeDaos.Users();
    private final FakeDaos.Posts posts = new FakeDaos.Posts();
    private final PostCache cache = new PostCache(posts);
    private final FakeDaos.Tags tags = new FakeDaos.Tags(posts);
    private final Session session = FakeDaos.adminSession();
    private final PostService service = new PostService(posts, new TagService(tags, session, cache), users, session, cache);
    private final User author = users.add("alice", Role.AUTHOR);

    @Test
    void createTrimsTextAndDropsDuplicateTags() {
        Post post = service.create(author.getId(), "  Hello  ", "  A body of at least twenty characters.  ", PostStatus.DRAFT, List.of("Java", " java "));

        assertEquals("Hello", post.getTitle());
        assertEquals("A body of at least twenty characters.", post.getContent());
        assertEquals(List.of("java"), post.getTags().stream().map(Tag::getName).toList());
    }

    @Test
    void typedTagsAreCreatedOnSaveAndExistingNamesAreReused() {
        int existing = tags.addTag(new Tag(0, "java"));

        Post post = service.create(author.getId(), "Tags", "A body of at least twenty characters.", PostStatus.DRAFT, List.of("java", "Spring Boot"));

        assertEquals(List.of(new Tag(existing, "java"), tags.getTagByName("spring-boot")), post.getTags());
        assertEquals(2, tags.rows.size(), "java is reused, spring-boot is new");
    }

    @Test
    void invalidTagNamesAreRejectedBeforeAnythingIsSaved() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(author.getId(), "Tags", "A body of at least twenty characters.", PostStatus.DRAFT, List.of("ok", "c#")));

        assertEquals(Set.of("tags"), e.fieldErrors().keySet());
        assertTrue(tags.rows.isEmpty());
        assertTrue(posts.rows.isEmpty());
    }

    @Test
    void missingValuesAreReportedPerField() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(null, "  ", "", null, List.of()));

        assertEquals(Set.of("author", "title", "content", "status"), e.fieldErrors().keySet());
    }

    @Test
    void titleIsLimitedTo200Characters() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(author.getId(), "x".repeat(201), "A body of at least twenty characters.", PostStatus.DRAFT, List.of()));

        assertEquals(Set.of("title"), e.fieldErrors().keySet());
    }

    @Test
    void contentIsLimitedTo65535Bytes() {
        String content = "é".repeat(40_000);   // 40,000 characters but 80,000 UTF-8 bytes

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(author.getId(), "Long", content, PostStatus.DRAFT, List.of()));

        assertEquals(Set.of("content"), e.fieldErrors().keySet());
    }

    @Test
    void readersCannotWritePosts() {
        User reader = users.add("fan", Role.READER);

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(reader.getId(), "Title", "A body of at least twenty characters.", PostStatus.DRAFT, List.of()));

        assertTrue(e.fieldErrors().get("author").contains("READER"));
        assertTrue(posts.rows.isEmpty());
    }

    @Test
    void unknownAuthorIsRejected() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(999, "Title", "A body of at least twenty characters.", PostStatus.DRAFT, List.of()));

        assertEquals(Set.of("author"), e.fieldErrors().keySet());
    }

    @Test
    void pageIndexPastTheEndMovesBackToTheLastPage() {
        for (int i = 0; i < 25; i++) {
            posts.add(author, "Post " + i, PostStatus.PUBLISHED);
        }

        Page<Post> page = service.findPage(PostFilter.none(), 7, 10);

        assertEquals(2, page.getPageIndex());
        assertEquals(3, page.getPageCount());
        assertEquals(5, page.getItems().size());
        assertEquals(20, posts.lastOffset);
        assertEquals(21, page.getFirstItemNumber());
        assertEquals(25, page.getLastItemNumber());
    }

    @Test
    void emptyResultIsOneEmptyPageWithoutQueryingRows() {
        Page<Post> page = service.findPage(PostFilter.none(), 3, 10);

        assertEquals(0, page.getPageIndex());
        assertEquals(1, page.getPageCount());
        assertTrue(page.getItems().isEmpty());
        assertEquals(-1, posts.lastOffset, "findPage must not run when count is 0");
    }

    @Test
    void updatingADeletedPostFails() {
        assertThrows(ValidationException.class,
                () -> service.update(77, author.getId(), "Title", "A body of at least twenty characters.", PostStatus.DRAFT, List.of()));
    }

    @Test
    void anAuthorCannotUseTheSameTitleTwice() {
        Post first = service.create(author.getId(), "Java records", "A body of at least twenty characters.", PostStatus.DRAFT, List.of());

        ValidationException e = assertThrows(ValidationException.class, () -> service.create(author.getId(),
                "Java records", "Another body of twenty characters.", PostStatus.DRAFT, List.of()));

        assertEquals(Set.of("title"), e.fieldErrors().keySet());
        assertEquals("Java records", service.update(first.getId(), author.getId(), "Java records",
                "An edited body of twenty characters.", PostStatus.PUBLISHED, List.of()).getTitle(), "its own title is fine");
    }

    // ---------- Discover (Phase 3: cache, search index, merge sort) ----------

    @Test
    void discoverShowsOnlyPublishedPostsNewestFirst() {
        Post older = posts.add(author, "Older", PostStatus.PUBLISHED);
        posts.add(author, "Hidden draft", PostStatus.DRAFT);
        Post newer = posts.add(author, "Newer", PostStatus.PUBLISHED);

        assertEquals(List.of(newer, older), discover(null, null, null, PostSort.NEWEST));
    }

    @Test
    void discoverSearchIgnoresCaseAndFindsWordsTypedPartly() {
        Post java = posts.add(author, "Learning Java", PostStatus.PUBLISHED);
        posts.add(author, "MySQL tips", PostStatus.PUBLISHED);

        assertEquals(List.of(java), discover("JAVA", null, null, PostSort.NEWEST));
        assertEquals(List.of(java), discover("learn", null, null, PostSort.NEWEST));
        assertEquals(List.of(), discover("python", null, null, PostSort.NEWEST));
    }

    @Test
    void discoverFindsPostsByTagNameAndFiltersByAuthorAndTag() {
        User bob = users.add("bob", Role.AUTHOR);
        String body = "A body of at least twenty characters.";
        Post alicesJava = service.create(author.getId(), "Alice on records", body, PostStatus.PUBLISHED, List.of("java"));
        Post bobsJava = service.create(bob.getId(), "Bob on records", body, PostStatus.PUBLISHED, List.of("java"));
        service.create(author.getId(), "Alice on SQL", body, PostStatus.PUBLISHED, List.of("mysql"));
        int javaTag = tags.getTagByName("java").getId();

        assertEquals(List.of(bobsJava, alicesJava), discover("Java", null, null, PostSort.NEWEST));   // found by tag name
        assertEquals(List.of(bobsJava, alicesJava), discover(null, null, javaTag, PostSort.NEWEST));
        assertEquals(List.of(alicesJava), discover(null, author.getId(), javaTag, PostSort.NEWEST));
    }

    @Test
    void discoverSortsByDateTitleAndRating() {
        Post beta = posts.add(author, "beta", PostStatus.PUBLISHED);
        Post alpha = posts.add(author, "Alpha", PostStatus.PUBLISHED);
        Post gamma = posts.add(author, "Gamma", PostStatus.PUBLISHED);
        beta.setAverageRating(4.5);
        gamma.setAverageRating(2.0);   // alpha has no rating yet, so it comes last

        assertEquals(List.of(gamma, alpha, beta), discover(null, null, null, PostSort.NEWEST));
        assertEquals(List.of(beta, alpha, gamma), discover(null, null, null, PostSort.OLDEST));
        assertEquals(List.of(alpha, beta, gamma), discover(null, null, null, PostSort.TITLE));   // ignores case
        assertEquals(List.of(beta, gamma, alpha), discover(null, null, null, PostSort.TOP_RATED));
    }

    @Test
    void discoverPagesThroughResultsAndMovesBackFromPastTheEnd() {
        for (int i = 0; i < 8; i++) {
            posts.add(author, "Post " + i, PostStatus.PUBLISHED);
        }

        Page<Post> page = service.discover(new PostFilter(PostStatus.PUBLISHED, null, null, null), PostSort.OLDEST, 9, 3);

        assertEquals(2, page.getPageIndex());
        assertEquals(3, page.getPageCount());
        assertEquals(8, page.getTotalItems());
        assertEquals(List.of("Post 6", "Post 7"), page.getItems().stream().map(Post::getTitle).toList());
    }

    @Test
    void discoverWithoutMatchesIsOneEmptyPage() {
        posts.add(author, "Java", PostStatus.PUBLISHED);

        Page<Post> page = service.discover(new PostFilter(PostStatus.PUBLISHED, null, null, "python"), PostSort.NEWEST, 4, 6);

        assertEquals(0, page.getPageIndex());
        assertEquals(1, page.getPageCount());
        assertTrue(page.getItems().isEmpty());
    }

    @Test
    void findByIdReadsPublishedPostsFromTheCacheAndDraftsFromTheDatabase() {
        Post published = posts.add(author, "Published", PostStatus.PUBLISHED);
        Post draft = posts.add(author, "Draft", PostStatus.DRAFT);

        assertEquals(published, service.findById(published.getId()));
        assertEquals(draft, service.findById(draft.getId()));   // the admin may see drafts
        assertNull(service.findById(999));
        assertEquals(1, posts.getAllPostsCalls, "loaded once, then served from memory");
    }

    private List<Post> discover(String search, Integer authorId, Integer tagId, PostSort sort) {
        return service.discover(new PostFilter(PostStatus.PUBLISHED, authorId, tagId, search), sort, 0, 50).getItems();
    }
}
