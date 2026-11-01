package com.example.bloggingplatform.service;

import com.example.bloggingplatform.cache.PostCache;
import com.example.bloggingplatform.validations.ValidationException;
import com.example.bloggingplatform.model.Comment;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostFilter;
import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Review;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.util.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The role rules: what readers, authors and admins may and may not do through the services. */
class AccessRulesTest {

    private final FakeDaos.Users users = new FakeDaos.Users();
    private final FakeDaos.Posts posts = new FakeDaos.Posts();
    private final PostCache cache = new PostCache(posts);
    private final FakeDaos.Comments comments = new FakeDaos.Comments();
    private final FakeDaos.Reviews reviews = new FakeDaos.Reviews();
    private final FakeDaos.Tags tags = new FakeDaos.Tags(posts);

    private final User admin = users.add("alice", Role.ADMIN);
    private final User author = users.add("bob", Role.AUTHOR);
    private final User otherAuthor = users.add("carol", Role.AUTHOR);
    private final User reader = users.add("dave", Role.READER);
    private final Post bobsPost = posts.add(author, "Bob's post", PostStatus.PUBLISHED);
    private final Post bobsDraft = posts.add(author, "Bob's draft", PostStatus.DRAFT);

    private PostService postsAs(User user) {
        Session session = FakeDaos.sessionOf(user);
        return new PostService(posts, new TagService(tags, session, cache), users, session, cache);
    }

    private CommentService commentsAs(User user) {
        return new CommentService(comments, posts, users, FakeDaos.sessionOf(user), cache);
    }

    private ReviewService reviewsAs(User user) {
        return new ReviewService(reviews, posts, users, FakeDaos.sessionOf(user), cache);
    }

    @Test
    void readersCannotWritePosts() {
        assertThrows(AccessDeniedException.class,
                () -> postsAs(reader).create(reader.getId(), "Mine", "A body of at least twenty characters.", PostStatus.DRAFT, List.of()));
    }

    @Test
    void authorsAlwaysWriteAsThemselves() {
        Post post = postsAs(author).create(otherAuthor.getId(), "Mine", "A body of at least twenty characters.", PostStatus.DRAFT, List.of());

        assertEquals(author.getId(), post.getAuthorId());
    }

    @Test
    void authorsCannotChangeOrDeleteSomeoneElsesPost() {
        PostService carol = postsAs(otherAuthor);

        assertThrows(AccessDeniedException.class, () -> carol.changeStatus(bobsPost.getId(), PostStatus.DRAFT));
        assertThrows(AccessDeniedException.class, () -> carol.delete(bobsPost.getId()));
        assertDoesNotThrow(() -> postsAs(author).changeStatus(bobsDraft.getId(), PostStatus.PUBLISHED));
        assertDoesNotThrow(() -> postsAs(admin).delete(bobsPost.getId()));
    }

    @Test
    void draftsAreVisibleOnlyToTheirAuthorAndAdmins() {
        assertNull(postsAs(reader).findById(bobsDraft.getId()));
        assertNull(postsAs(otherAuthor).findById(bobsDraft.getId()));
        assertNotNull(postsAs(author).findById(bobsDraft.getId()));
        assertNotNull(postsAs(admin).findById(bobsDraft.getId()));

        PostFilter anyStatus = new PostFilter(null, author.getId(), null, null);
        assertEquals(PostStatus.PUBLISHED, postsAs(reader).visible(anyStatus).getStatus());
        assertNull(postsAs(author).visible(anyStatus).getStatus());
    }

    @Test
    void readersCommentAsThemselvesAndEditOnlyTheirOwnComments() {
        Comment comment = commentsAs(reader).create(bobsPost.getId(), admin.getId(), "Nice post");
        assertEquals(reader.getId(), comment.getUserId());

        assertThrows(AccessDeniedException.class, () -> commentsAs(otherAuthor).update(comment.getId(), "Changed"));
        assertEquals("Edited", commentsAs(reader).update(comment.getId(), "Edited").getContent());
    }

    @Test
    void postAuthorsMayDeleteCommentsOnTheirPostsButNotOthers() {
        Comment comment = commentsAs(reader).create(bobsPost.getId(), null, "Spam");

        assertFalse(commentsAs(otherAuthor).canDelete(comment));
        assertThrows(AccessDeniedException.class, () -> commentsAs(otherAuthor).delete(comment.getId()));
        assertTrue(commentsAs(author).canDelete(comment));
        assertFalse(commentsAs(author).canEdit(comment));
        assertDoesNotThrow(() -> commentsAs(author).delete(comment.getId()));
    }

    @Test
    void postAuthorsCannotDeleteReviewsOfTheirPosts() {
        Review review = reviewsAs(reader).create(bobsPost.getId(), null, 2, "Meh");

        assertThrows(AccessDeniedException.class, () -> reviewsAs(author).delete(review.getId()));
        assertDoesNotThrow(() -> reviewsAs(reader).update(review.getId(), 4, "Better on second read"));
    }

    @Test
    void onlyAdminsManageUsersAndTagsAndSeeReports() {
        UserService asAuthor = new UserService(users, new PasswordHasher(1_000), FakeDaos.sessionOf(author), cache);
        assertThrows(AccessDeniedException.class, asAuthor::findAll);
        assertThrows(AccessDeniedException.class, () -> asAuthor.delete(reader.getId()));

        TagService tagsAsAuthor = new TagService(tags, FakeDaos.sessionOf(author), cache);
        int tagId = new TagService(tags, FakeDaos.sessionOf(admin), cache).create("java").getId();
        assertThrows(AccessDeniedException.class, () -> tagsAsAuthor.create("jvm"));   // authors add tags in a post
        assertThrows(AccessDeniedException.class, () -> tagsAsAuthor.rename(tagId, "jvm"));
        assertThrows(AccessDeniedException.class, () -> tagsAsAuthor.delete(tagId));

        ReportService reportsAsAuthor = new ReportService(null, posts, FakeDaos.sessionOf(author));
        assertThrows(AccessDeniedException.class, () -> reportsAsAuthor.topRated(5));
    }

    @Test
    void authorsAreOfferedOnlyTheTagsOfTheirOwnPosts() {
        postsAs(author).create(null, "Bob's tagged post", "A body of at least twenty characters.", PostStatus.DRAFT, List.of("bobs-tag"));
        postsAs(otherAuthor).create(null, "Carol's tagged post", "A body of at least twenty characters.", PostStatus.DRAFT, List.of("carols-tag"));

        List<String> bobs = new TagService(tags, FakeDaos.sessionOf(author), cache).findForWriting().stream()
                .map(tag -> tag.getName()).toList();
        List<String> everything = new TagService(tags, FakeDaos.sessionOf(admin), cache).findForWriting().stream()
                .map(tag -> tag.getName()).toList();

        assertEquals(List.of("bobs-tag"), bobs);
        assertEquals(List.of("bobs-tag", "carols-tag"), everything);
    }

    @Test
    void adminsCannotDeleteOrDemoteThemselves() {
        UserService asAdmin = new UserService(users, new PasswordHasher(1_000), FakeDaos.sessionOf(admin), cache);

        assertThrows(ValidationException.class, () -> asAdmin.delete(admin.getId()));
        ValidationException e = assertThrows(ValidationException.class,
                () -> asAdmin.update(admin.getId(), "alice", "alice@example.com", "", Role.READER));
        assertTrue(e.fieldErrors().containsKey("role"));
    }

    @Test
    void nobodySignedInCannotChangeAnything() {
        Session nobody = new Session();
        CommentService service = new CommentService(comments, posts, users, nobody, cache);

        assertThrows(AccessDeniedException.class, () -> service.create(bobsPost.getId(), reader.getId(), "Hi"));
        assertEquals(PostStatus.PUBLISHED,
                new PostService(posts, new TagService(tags, nobody, cache), users, nobody, cache).visible(PostFilter.none()).getStatus());
    }
}
