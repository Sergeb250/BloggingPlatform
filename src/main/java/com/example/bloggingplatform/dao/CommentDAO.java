package com.example.bloggingplatform.dao;

import com.example.bloggingplatform.model.Comment;

import java.util.List;

/**
 * Reads and writes the comments table. The services only know this interface; the MySQL code is in
 * {@code CommentDAOImpl}, so it could be replaced (e.g. by a test fake) without changing a service.
 */
public interface CommentDAO {

    List<Comment> getAllComments();

    List<Comment> getCommentsByPost(int postId);

    /** Comments written by one user. */
    List<Comment> getCommentsByUser(int userId);

    /** Comments on the posts of one author. */
    List<Comment> getCommentsOnPostsOf(int authorId);

    /** The comment, or null if there is none with this id. */
    Comment getCommentById(int id);

    /** Saves a new comment and returns the id MySQL gave it. */
    int addComment(Comment comment);

    /** Changes the text; returns false if the comment no longer exists. */
    boolean updateComment(Comment comment);

    boolean deleteComment(int id);
}
