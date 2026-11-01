package com.example.bloggingplatform.service;

import com.example.bloggingplatform.cache.PostCache;
import com.example.bloggingplatform.validations.ValidationException;
import com.example.bloggingplatform.model.Comment;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.User;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentServiceTest {

    private final FakeDaos.Users users = new FakeDaos.Users();
    private final FakeDaos.Posts posts = new FakeDaos.Posts();
    private final PostCache cache = new PostCache(posts);
    private final FakeDaos.Comments comments = new FakeDaos.Comments();
    private final CommentService service = new CommentService(comments, posts, users, FakeDaos.adminSession(), cache);

    private final User author = users.add("alice", Role.AUTHOR);
    private final User reader = users.add("bob", Role.READER);
    private final Post published = posts.add(author, "Published", PostStatus.PUBLISHED);
    private final Post draft = posts.add(author, "Draft", PostStatus.DRAFT);

    @Test
    void validCommentIsSavedTrimmed() {
        Comment comment = service.create(published.getId(), reader.getId(), "  Great post!  ");

        assertEquals("Great post!", comment.getContent());
        assertEquals(1, comments.rows.size());
    }

    @Test
    void draftPostsCannotBeCommented() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(draft.getId(), reader.getId(), "Hello"));

        assertEquals(Set.of("post"), e.fieldErrors().keySet());
        assertTrue(comments.rows.isEmpty());
    }

    @Test
    void postUserAndTextAreRequired() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(null, null, "   "));

        assertEquals(Set.of("post", "user", "content"), e.fieldErrors().keySet());
    }

    @Test
    void commentIsLimitedTo2000Characters() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(published.getId(), reader.getId(), "x".repeat(2001)));

        assertEquals(Set.of("content"), e.fieldErrors().keySet());
    }

    @Test
    void updateChangesOnlyTheText() {
        Comment comment = service.create(published.getId(), reader.getId(), "First");

        Comment updated = service.update(comment.getId(), "Second");

        assertEquals("Second", updated.getContent());
        assertEquals(published.getId(), updated.getPostId());
        assertEquals(reader.getId(), updated.getUserId());
    }
}
