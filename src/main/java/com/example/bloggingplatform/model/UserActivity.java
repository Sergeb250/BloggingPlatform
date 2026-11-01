package com.example.bloggingplatform.model;

import java.time.LocalDateTime;

/** What one user has written, for the admin dashboard. */
public class UserActivity {

    private final int userId;
    private final String username;
    private final Role role;
    private final LocalDateTime joined;
    private final long posts;
    private final long published;
    private final long comments;
    private final long reviews;
    private final Double ratingReceived;   // average rating of the reviews on this user's posts; null if none

    public UserActivity(int userId, String username, Role role, LocalDateTime joined, long posts, long published,
                        long comments, long reviews, Double ratingReceived) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.joined = joined;
        this.posts = posts;
        this.published = published;
        this.comments = comments;
        this.reviews = reviews;
        this.ratingReceived = ratingReceived;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public LocalDateTime getJoined() {
        return joined;
    }

    public long getPosts() {
        return posts;
    }

    public long getPublished() {
        return published;
    }

    public long getDrafts() {
        return posts - published;
    }

    public long getComments() {
        return comments;
    }

    public long getReviews() {
        return reviews;
    }

    public Double getRatingReceived() {
        return ratingReceived;
    }
}
