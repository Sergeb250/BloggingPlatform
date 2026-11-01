package com.example.bloggingplatform.dao.impl;

import com.example.bloggingplatform.config.DBConnection;
import com.example.bloggingplatform.dao.CommentDAO;
import com.example.bloggingplatform.dao.DaoException;
import com.example.bloggingplatform.model.Comment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** The MySQL version of {@link CommentDAO}. Every value goes into a ? placeholder, so typed text can never change the SQL. */
public class CommentDAOImpl implements CommentDAO {

    /** Every column of a comment, plus the post title and the writer's name. */
    private static final String SELECT_COMMENTS = """
            SELECT c.comment_id, c.post_id, p.title AS post_title, c.user_id, u.username, c.content, c.created_at
            FROM comments c
            JOIN posts p ON p.post_id = c.post_id
            JOIN users u ON u.user_id = c.user_id
            """;

    @Override
    public List<Comment> getAllComments() {
        List<Comment> comments = new ArrayList<>();
        String sql = SELECT_COMMENTS + " ORDER BY c.created_at DESC, c.comment_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                comments.add(readComment(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load the comments.", e);
        }
        return comments;
    }

    @Override
    public List<Comment> getCommentsByPost(int postId) {
        List<Comment> comments = new ArrayList<>();
        String sql = SELECT_COMMENTS + " WHERE c.post_id = ? ORDER BY c.created_at DESC, c.comment_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comments.add(readComment(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load the comments on this post.", e);
        }
        return comments;
    }

    /** Comments written by one user. */
    @Override
    public List<Comment> getCommentsByUser(int userId) {
        List<Comment> comments = new ArrayList<>();
        String sql = SELECT_COMMENTS + " WHERE c.user_id = ? ORDER BY c.created_at DESC, c.comment_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comments.add(readComment(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load your comments.", e);
        }
        return comments;
    }

    /** Comments on the posts of one author. */
    @Override
    public List<Comment> getCommentsOnPostsOf(int authorId) {
        List<Comment> comments = new ArrayList<>();
        String sql = SELECT_COMMENTS + " WHERE p.author_id = ? ORDER BY c.created_at DESC, c.comment_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, authorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comments.add(readComment(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load the comments on your posts.", e);
        }
        return comments;
    }

    /** The comment, or null if there is none with this id. */
    @Override
    public Comment getCommentById(int id) {
        String sql = SELECT_COMMENTS + " WHERE c.comment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return readComment(rs);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load the comment.", e);
        }
        return null;
    }

    /** Saves a new comment and returns the id MySQL gave it. */
    @Override
    public int addComment(Comment comment) {
        String sql = "INSERT INTO comments (post_id, user_id, content) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, comment.getPostId());
            ps.setInt(2, comment.getUserId());
            ps.setString(3, comment.getContent());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DaoException("Could not save the comment.", e);
        }
    }

    /** Changes the text; returns false if the comment no longer exists. */
    @Override
    public boolean updateComment(Comment comment) {
        String sql = "UPDATE comments SET content = ? WHERE comment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, comment.getContent());
            ps.setInt(2, comment.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Could not update the comment.", e);
        }
    }

    @Override
    public boolean deleteComment(int id) {
        String sql = "DELETE FROM comments WHERE comment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Could not delete the comment.", e);
        }
    }

    /** Turns the current row into a Comment object. */
    private Comment readComment(ResultSet rs) throws SQLException {
        Comment comment = new Comment();
        comment.setId(rs.getInt("comment_id"));
        comment.setPostId(rs.getInt("post_id"));
        comment.setPostTitle(rs.getString("post_title"));
        comment.setUserId(rs.getInt("user_id"));
        comment.setUsername(rs.getString("username"));
        comment.setContent(rs.getString("content"));
        comment.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return comment;
    }
}
