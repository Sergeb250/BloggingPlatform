package com.example.bloggingplatform.service;

import com.example.bloggingplatform.cache.PostCache;
import com.example.bloggingplatform.dao.CommentDAO;
import com.example.bloggingplatform.dao.PostDAO;
import com.example.bloggingplatform.dao.UserDAO;
import com.example.bloggingplatform.model.Comment;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.validations.CommentValidator;
import com.example.bloggingplatform.validations.Validation;
import com.example.bloggingplatform.validations.ValidationException;

import java.util.List;

/**
 * Comments. They are only allowed on published posts. Everyone comments as themselves and edits
 * their own comments; a comment can be deleted by its writer, by the post's author or by an admin.
 * Adding or deleting a comment changes the post's comment count, so it empties the {@link PostCache}.
 */
public class CommentService {

    private final CommentDAO comments;
    private final PostDAO posts;
    private final UserDAO users;
    private final Session session;
    private final PostCache cache;

    public CommentService(CommentDAO comments, PostDAO posts, UserDAO users, Session session, PostCache cache) {
        this.comments = comments;
        this.posts = posts;
        this.users = users;
        this.session = session;
        this.cache = cache;
    }

    /** The comments of one post; or, for admins, every comment when {@code postId} is null. */
    public List<Comment> find(Integer postId) {
        if (postId != null) {
            return comments.getCommentsByPost(postId);
        }
        session.requireAdmin("list every comment");
        return comments.getAllComments();
    }

    /** The comments the signed-in user wrote. */
    public List<Comment> findMine() {
        return comments.getCommentsByUser(session.require().getId());
    }

    /** The comments on the signed-in author's posts. */
    public List<Comment> findOnMyPosts() {
        return comments.getCommentsOnPostsOf(session.require().getId());
    }

    /** Adds a comment. Users comment as themselves; only an admin may pick another writer. */
    public Comment create(Integer postId, Integer userId, String content) {
        User me = session.require();
        Integer writer = userId;
        if (me.getRole() != Role.ADMIN) {
            writer = me.getId();
        }

        Validation v = new Validation();
        v.required("post", "Post", postId);
        v.required("user", "User", writer);
        String text = v.text("content", content, CommentValidator::comment);
        if (postId != null) {
            Post post = posts.getPostById(postId);
            if (post == null) {
                v.reject("post", "The selected post no longer exists.");
            } else if (post.getStatus() != PostStatus.PUBLISHED) {
                v.reject("post", "Comments are only allowed on published posts.");
            }
        }
        if (writer != null && users.getUserById(writer) == null) {
            v.reject("user", "The selected user no longer exists.");
        }
        v.throwIfInvalid();

        int id = comments.addComment(new Comment(0, postId, writer, text));
        cache.clear();
        return comments.getCommentById(id);
    }

    /** Changes the text of a comment; its post and writer stay the same. */
    public Comment update(int id, String content) {
        Comment existing = comments.getCommentById(id);
        if (existing == null) {
            throw notFound();
        }
        session.requireOwnerOrAdmin(existing.getUserId(), "You can only edit your own comments.");
        Validation v = new Validation();
        String text = v.text("content", content, CommentValidator::comment);
        v.throwIfInvalid();

        existing.setContent(text);
        if (!comments.updateComment(existing)) {
            throw notFound();
        }
        return comments.getCommentById(id);
    }

    public void delete(int id) {
        Comment existing = comments.getCommentById(id);
        if (existing == null) {
            throw notFound();
        }
        if (!canDelete(existing)) {
            throw new AccessDeniedException("You can only delete your own comments or comments on your own posts.");
        }
        if (!comments.deleteComment(id)) {
            throw notFound();
        }
        cache.clear();
    }

    /** True if the signed-in user may edit the comment's text. */
    public boolean canEdit(Comment comment) {
        return session.isAdmin() || session.is(comment.getUserId());
    }

    /** True if the signed-in user may delete the comment: its writer, the post's author or an admin. */
    public boolean canDelete(Comment comment) {
        if (canEdit(comment)) {
            return true;
        }
        Post post = posts.getPostById(comment.getPostId());
        return post != null && session.is(post.getAuthorId());
    }

    private static ValidationException notFound() {
        return ValidationException.of("This comment no longer exists. Refresh the list.");
    }
}
