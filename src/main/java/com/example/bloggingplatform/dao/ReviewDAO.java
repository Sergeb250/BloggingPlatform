package com.example.bloggingplatform.dao;

import com.example.bloggingplatform.model.Review;

import java.util.List;

/**
 * Reads and writes the reviews table. The services only know this interface; the MySQL code is in
 * {@code ReviewDAOImpl}, so it could be replaced (e.g. by a test fake) without changing a service.
 */
public interface ReviewDAO {

    List<Review> getAllReviews();

    List<Review> getReviewsByPost(int postId);

    /** Reviews written by one user. */
    List<Review> getReviewsByUser(int userId);

    /** Reviews of the posts of one author. */
    List<Review> getReviewsOnPostsOf(int authorId);

    /** The review, or null if there is none with this id. */
    Review getReviewById(int id);

    /** Saves a new review and returns the id MySQL gave it. */
    int addReview(Review review);

    /** Changes rating and text; returns false if the review no longer exists. */
    boolean updateReview(Review review);

    boolean deleteReview(int id);
}
