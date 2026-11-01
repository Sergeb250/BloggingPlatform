package com.example.bloggingplatform.dao.mongo;

import com.example.bloggingplatform.dao.CommentDAO;
import com.example.bloggingplatform.dao.DaoException;
import com.example.bloggingplatform.dao.PostDAO;
import com.example.bloggingplatform.dao.UserDAO;
import com.example.bloggingplatform.model.Comment;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.User;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * The MongoDB version of {@link CommentDAO} (Phase 4): every comment is one document in the
 * {@code comments} collection. It has the same methods as the MySQL version, so
 * {@code CommentService} works with either one without a change.
 * <p>
 * Each document also keeps a copy of the post title, the post's author and the writer's name
 * (taken from MySQL when the comment is saved), so reading comments needs no join:
 * <pre>
 * { _id: 15, postId: 3, post: { title: "Foreign Keys", authorId: 2 },
 *   user: { id: 7, username: "bob" }, content: "Nice post!", createdAt: ISODate("2026-11-01T10:15:00Z") }
 * </pre>
 * The collection, its validator and its indexes are created by {@code docs/nosql/mongo-schema.js}.
 */
public class CommentMongoDAO implements CommentDAO {

    private final MongoCollection<Document> comments;
    private final MongoCollection<Document> counters;   // { _id: "comments", seq: 15 } hands out the ids
    private final PostDAO posts;
    private final UserDAO users;

    public CommentMongoDAO(MongoDatabase database, PostDAO posts, UserDAO users) {
        this.comments = database.getCollection("comments");
        this.counters = database.getCollection("counters");
        this.posts = posts;
        this.users = users;
    }

    @Override
    public List<Comment> getAllComments() {
        return find(new Document(), "Could not load the comments.");
    }

    @Override
    public List<Comment> getCommentsByPost(int postId) {
        return find(Filters.eq("postId", postId), "Could not load the comments on this post.");
    }

    /** Comments written by one user. */
    @Override
    public List<Comment> getCommentsByUser(int userId) {
        return find(Filters.eq("user.id", userId), "Could not load your comments.");
    }

    /** Comments on the posts of one author (the author id is copied into every comment). */
    @Override
    public List<Comment> getCommentsOnPostsOf(int authorId) {
        return find(Filters.eq("post.authorId", authorId), "Could not load the comments on your posts.");
    }

    /** The comment, or null if there is none with this id. */
    @Override
    public Comment getCommentById(int id) {
        try {
            Document doc = comments.find(Filters.eq("_id", id)).first();
            return doc == null ? null : readComment(doc);
        } catch (MongoException e) {
            throw new DaoException("Could not load the comment.", e);
        }
    }

    /**
     * Saves a new comment and returns its id. The post and the writer must exist
     * ({@code CommentService} checks that first); their names are copied into the document.
     */
    @Override
    public int addComment(Comment comment) {
        Post post = posts.getPostById(comment.getPostId());
        User user = users.getUserById(comment.getUserId());
        try {
            int id = nextId();
            Document doc = new Document("_id", id)
                    .append("postId", post.getId())
                    .append("post", new Document("title", post.getTitle()).append("authorId", post.getAuthorId()))
                    .append("user", new Document("id", user.getId()).append("username", user.getUsername()))
                    .append("content", comment.getContent())
                    .append("createdAt", new Date());
            comments.insertOne(doc);
            return id;
        } catch (MongoException e) {
            throw new DaoException("Could not save the comment.", e);
        }
    }

    /** Changes the text; returns false if the comment no longer exists. */
    @Override
    public boolean updateComment(Comment comment) {
        try {
            return comments.updateOne(Filters.eq("_id", comment.getId()), Updates.set("content", comment.getContent()))
                    .getMatchedCount() > 0;
        } catch (MongoException e) {
            throw new DaoException("Could not update the comment.", e);
        }
    }

    @Override
    public boolean deleteComment(int id) {
        try {
            return comments.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
        } catch (MongoException e) {
            throw new DaoException("Could not delete the comment.", e);
        }
    }

    /** The comments matching the filter, newest first (like ORDER BY created_at DESC, comment_id DESC). */
    private List<Comment> find(Bson filter, String errorMessage) {
        List<Comment> result = new ArrayList<>();
        try {
            for (Document doc : comments.find(filter).sort(Sorts.descending("createdAt", "_id"))) {
                result.add(readComment(doc));
            }
        } catch (MongoException e) {
            throw new DaoException(errorMessage, e);
        }
        return result;
    }

    /** The next comment id: adds 1 to the "comments" counter (which starts at 1 the first time). */
    private int nextId() {
        Document counter = counters.findOneAndUpdate(Filters.eq("_id", "comments"), Updates.inc("seq", 1),
                new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
        return Objects.requireNonNull(counter, "upsert always returns the counter").getInteger("seq");
    }

    /** Turns a document into a Comment object. */
    private Comment readComment(Document doc) {
        Document post = doc.get("post", Document.class);
        Document user = doc.get("user", Document.class);
        Comment comment = new Comment();
        comment.setId(doc.getInteger("_id"));
        comment.setPostId(doc.getInteger("postId"));
        comment.setPostTitle(post.getString("title"));
        comment.setUserId(user.getInteger("id"));
        comment.setUsername(user.getString("username"));
        comment.setContent(doc.getString("content"));
        comment.setCreatedAt(LocalDateTime.ofInstant(doc.getDate("createdAt").toInstant(), ZoneId.systemDefault()));
        return comment;
    }
}
