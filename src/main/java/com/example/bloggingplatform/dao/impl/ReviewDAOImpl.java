package com.example.bloggingplatform.dao.impl;

import com.example.bloggingplatform.config.DBConnection;
import com.example.bloggingplatform.dao.DaoException;
import com.example.bloggingplatform.dao.ReviewDAO;
import com.example.bloggingplatform.model.Review;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** The MySQL version of {@link ReviewDAO}. Every value goes into a ? placeholder, so typed text can never change the SQL. */
public class ReviewDAOImpl implements ReviewDAO {

    /** Every column of a review, plus the post title and the reviewer's name. */
    private static final String SELECT_REVIEWS = """
            SELECT r.review_id, r.post_id, p.title AS post_title, r.user_id, u.username,
                   r.rating, r.review_text, r.created_at
            FROM reviews r
            JOIN posts p ON p.post_id = r.post_id
            JOIN users u ON u.user_id = r.user_id
            """;

    @Override
    public List<Review> getAllReviews() {
        List<Review> reviews = new ArrayList<>();
        String sql = SELECT_REVIEWS + " ORDER BY r.created_at DESC, r.review_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reviews.add(readReview(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load the reviews.", e);
        }
        return reviews;
    }

    @Override
    public List<Review> getReviewsByPost(int postId) {
        List<Review> reviews = new ArrayList<>();
        String sql = SELECT_REVIEWS + " WHERE r.post_id = ? ORDER BY r.created_at DESC, r.review_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reviews.add(readReview(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load the reviews of this post.", e);
        }
        return reviews;
    }

    /** Reviews written by one user. */
    @Override
    public List<Review> getReviewsByUser(int userId) {
        List<Review> reviews = new ArrayList<>();
        String sql = SELECT_REVIEWS + " WHERE r.user_id = ? ORDER BY r.created_at DESC, r.review_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reviews.add(readReview(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load your reviews.", e);
        }
        return reviews;
    }

    /** Reviews of the posts of one author. */
    @Override
    public List<Review> getReviewsOnPostsOf(int authorId) {
        List<Review> reviews = new ArrayList<>();
        String sql = SELECT_REVIEWS + " WHERE p.author_id = ? ORDER BY r.created_at DESC, r.review_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, authorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reviews.add(readReview(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load the reviews of your posts.", e);
        }
        return reviews;
    }

    /** The review, or null if there is none with this id. */
    @Override
    public Review getReviewById(int id) {
        String sql = SELECT_REVIEWS + " WHERE r.review_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return readReview(rs);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load the review.", e);
        }
        return null;
    }

    /** Saves a new review and returns the id MySQL gave it. */
    @Override
    public int addReview(Review review) {
        String sql = "INSERT INTO reviews (post_id, user_id, rating, review_text) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, review.getPostId());
            ps.setInt(2, review.getUserId());
            ps.setInt(3, review.getRating());
            if (review.getReviewText() == null) {
                ps.setNull(4, Types.VARCHAR);   // the text is optional
            } else {
                ps.setString(4, review.getReviewText());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DaoException("Could not save the review.", e);
        }
    }

    /** Changes rating and text; returns false if the review no longer exists. */
    @Override
    public boolean updateReview(Review review) {
        String sql = "UPDATE reviews SET rating = ?, review_text = ? WHERE review_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, review.getRating());
            if (review.getReviewText() == null) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, review.getReviewText());
            }
            ps.setInt(3, review.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Could not update the review.", e);
        }
    }

    @Override
    public boolean deleteReview(int id) {
        String sql = "DELETE FROM reviews WHERE review_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Could not delete the review.", e);
        }
    }

    /** Turns the current row into a Review object. */
    private Review readReview(ResultSet rs) throws SQLException {
        Review review = new Review();
        review.setId(rs.getInt("review_id"));
        review.setPostId(rs.getInt("post_id"));
        review.setPostTitle(rs.getString("post_title"));
        review.setUserId(rs.getInt("user_id"));
        review.setUsername(rs.getString("username"));
        review.setRating(rs.getInt("rating"));
        review.setReviewText(rs.getString("review_text"));
        review.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return review;
    }
}
