package com.example.bloggingplatform.service;

import com.example.bloggingplatform.cache.PostCache;
import com.example.bloggingplatform.config.MongoConnection;
import com.example.bloggingplatform.dao.CommentDAO;
import com.example.bloggingplatform.dao.mongo.CommentMongoDAO;
import com.example.bloggingplatform.model.Comment;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.validations.ValidationException;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 4: the unchanged {@link CommentService} working on MongoDB Atlas through
 * {@link CommentMongoDAO} instead of MySQL (open/closed principle). Posts and users come from the
 * in-memory fake DAOs. Runs in its own database "bloggingPlatform_test", dropped at the end; the
 * test is skipped when Atlas cannot be reached.
 */
class CommentMongoIntegrationTest {

    private static MongoClient client;
    private static MongoDatabase database;

    private final FakeDaos.Users users = new FakeDaos.Users();
    private final FakeDaos.Posts posts = new FakeDaos.Posts();
    private final CommentDAO mongo = new CommentMongoDAO(database, posts, users);
    private final CommentService service = new CommentService(mongo, posts, users, FakeDaos.adminSession(), new PostCache(posts));

    private final User author = users.add("alice", Role.AUTHOR);
    private final User reader = users.add("bob", Role.READER);
    private final Post published = posts.add(author, "Indexes in MongoDB", PostStatus.PUBLISHED);
    private final Post draft = posts.add(author, "Unfinished", PostStatus.DRAFT);

    @BeforeAll
    static void connect() {
        try {
            client = MongoClients.create(MongoConnection.URI);
            database = client.getDatabase("bloggingPlatform_test");
            database.drop();   // start empty
        } catch (MongoException | IllegalArgumentException e) {
            Assumptions.abort("MongoDB Atlas is not available: " + e.getMessage());
        }
    }

    @AfterAll
    static void dropTestDatabase() {
        if (database != null) {
            database.drop();
            client.close();
        }
    }

    @Test
    void aCommentIsOneDocumentWithCopiesOfThePostAndWriter() {
        Comment saved = service.create(published.getId(), reader.getId(), "  Great overview!  ");

        Document doc = database.getCollection("comments").find(Filters.eq("_id", saved.getId())).first();
        assertNotNull(doc);
        assertEquals(published.getId(), doc.getInteger("postId"));
        assertEquals("Indexes in MongoDB", doc.get("post", Document.class).getString("title"));
        assertEquals(author.getId(), doc.get("post", Document.class).getInteger("authorId"));
        assertEquals("bob", doc.get("user", Document.class).getString("username"));
        assertEquals("Great overview!", doc.getString("content"));   // trimmed by the service, as with MySQL

        assertEquals("Indexes in MongoDB", saved.getPostTitle());
        assertEquals("bob", saved.getUsername());
    }

    @Test
    void commentsAreFoundByPostByWriterAndByPostAuthorNewestFirst() {
        Comment first = service.create(published.getId(), reader.getId(), "First comment");
        Comment second = service.create(published.getId(), author.getId(), "Thanks for reading!");

        assertEquals(List.of(second.getId(), first.getId()), ids(mongo.getCommentsByPost(published.getId())));
        assertEquals(List.of(first.getId()), ids(mongo.getCommentsByUser(reader.getId())));
        assertEquals(List.of(second.getId(), first.getId()), ids(mongo.getCommentsOnPostsOf(author.getId())));
        assertTrue(second.getId() > first.getId(), "ids come from the counters collection, one up each time");
    }

    @Test
    void editAndDeleteWorkLikeWithMySql() {
        Comment saved = service.create(published.getId(), reader.getId(), "Typo hree");

        assertEquals("Typo here", service.update(saved.getId(), "Typo here").getContent());
        service.delete(saved.getId());

        assertNull(mongo.getCommentById(saved.getId()));
        assertFalse(mongo.deleteComment(saved.getId()));
        assertThrows(ValidationException.class, () -> service.delete(saved.getId()), "already deleted");
    }

    @Test
    void theServiceRulesStillApply() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(draft.getId(), reader.getId(), "Too early"));

        assertTrue(e.fieldErrors().containsKey("post"), "no comments on drafts, whatever the database");
        assertEquals(0, database.getCollection("comments").countDocuments(Filters.eq("postId", draft.getId())));
    }

    private static List<Integer> ids(List<Comment> comments) {
        return comments.stream().map(Comment::getId).toList();
    }
}
