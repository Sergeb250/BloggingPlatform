package com.example.bloggingplatform.dao.impl;

import com.example.bloggingplatform.config.DBConnection;
import com.example.bloggingplatform.dao.DaoException;
import com.example.bloggingplatform.dao.ReportDAO;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.SiteSummary;
import com.example.bloggingplatform.model.UserActivity;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** The MySQL version of {@link ReportDAO}. Every value goes into a ? placeholder, so typed text can never change the SQL. */
public class ReportDAOImpl implements ReportDAO {

    /** Users per role, posts per status, comments, reviews and the average rating. */
    @Override
    public SiteSummary getSummary() {
        String sql = """
                SELECT (SELECT COUNT(*) FROM users) AS users,
                       (SELECT COUNT(*) FROM users WHERE role = 'ADMIN') AS admins,
                       (SELECT COUNT(*) FROM users WHERE role = 'AUTHOR') AS authors,
                       (SELECT COUNT(*) FROM users WHERE role = 'READER') AS readers,
                       (SELECT COUNT(*) FROM posts) AS posts,
                       (SELECT COUNT(*) FROM posts WHERE status = 'PUBLISHED') AS published,
                       (SELECT COUNT(*) FROM posts WHERE status = 'DRAFT') AS drafts,
                       (SELECT COUNT(*) FROM comments) AS comments,
                       (SELECT COUNT(*) FROM reviews) AS reviews,
                       (SELECT AVG(rating) FROM reviews) AS average_rating
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            BigDecimal average = rs.getBigDecimal("average_rating");   // null when there are no reviews
            return new SiteSummary(rs.getLong("users"), rs.getLong("admins"), rs.getLong("authors"),
                    rs.getLong("readers"), rs.getLong("posts"), rs.getLong("published"), rs.getLong("drafts"),
                    rs.getLong("comments"), rs.getLong("reviews"), average == null ? null : average.doubleValue());
        } catch (SQLException e) {
            throw new DaoException("Could not load the totals.", e);
        }
    }

    /** One row per user with what they wrote; most posts first. */
    @Override
    public List<UserActivity> getUserActivity() {
        List<UserActivity> rows = new ArrayList<>();
        String sql = """
                SELECT u.user_id, u.username, u.role, u.created_at,
                       (SELECT COUNT(*) FROM posts p WHERE p.author_id = u.user_id) AS posts,
                       (SELECT COUNT(*) FROM posts p WHERE p.author_id = u.user_id AND p.status = 'PUBLISHED') AS published,
                       (SELECT COUNT(*) FROM comments c WHERE c.user_id = u.user_id) AS comments,
                       (SELECT COUNT(*) FROM reviews r WHERE r.user_id = u.user_id) AS reviews,
                       (SELECT AVG(r.rating) FROM reviews r JOIN posts p ON p.post_id = r.post_id
                         WHERE p.author_id = u.user_id) AS rating_received
                FROM users u
                ORDER BY posts DESC, u.username
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BigDecimal rating = rs.getBigDecimal("rating_received");
                rows.add(new UserActivity(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        Role.valueOf(rs.getString("role")),
                        rs.getObject("created_at", LocalDateTime.class),
                        rs.getLong("posts"),
                        rs.getLong("published"),
                        rs.getLong("comments"),
                        rs.getLong("reviews"),
                        rating == null ? null : rating.doubleValue()));
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load the activity.", e);
        }
        return rows;
    }
}
