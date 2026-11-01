package com.example.bloggingplatform.model;

import java.time.LocalDateTime;

/** A row of the {@code reviews} table: a 1-5 star rating with optional text. */
public class Review {

    private int id;                    // 0 until the review is saved
    private int postId;
    private String postTitle;          // title of the reviewed post (read from a join)
    private int userId;
    private String username;           // name of the reviewer (read from a join)
    private int rating;                // 1 to 5 stars
    private String reviewText;         // null when the reviewer left only a rating
    private LocalDateTime createdAt;

    public Review() {
    }

    /** A review as typed in the form: only the values the user controls. */
    public Review(int id, int postId, int userId, int rating, String reviewText) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.rating = rating;
        this.reviewText = reviewText;
    }

    public Review(int id, int postId, String postTitle, int userId, String username, int rating,
                  String reviewText, LocalDateTime createdAt) {
        this(id, postId, userId, rating, reviewText);
        this.postTitle = postTitle;
        this.username = username;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** Two Review objects are the same review when they have the same id. */
    @Override
    public boolean equals(Object other) {
        return other instanceof Review review && review.id == id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
