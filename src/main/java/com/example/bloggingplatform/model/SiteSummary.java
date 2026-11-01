package com.example.bloggingplatform.model;

/** Site-wide numbers for the admin dashboard, counted by one query. */
public class SiteSummary {

    private final long users;
    private final long admins;
    private final long authors;
    private final long readers;
    private final long posts;
    private final long published;
    private final long drafts;
    private final long comments;
    private final long reviews;
    private final Double averageRating;   // average of all ratings; null when there are no reviews

    public SiteSummary(long users, long admins, long authors, long readers, long posts, long published, long drafts,
                       long comments, long reviews, Double averageRating) {
        this.users = users;
        this.admins = admins;
        this.authors = authors;
        this.readers = readers;
        this.posts = posts;
        this.published = published;
        this.drafts = drafts;
        this.comments = comments;
        this.reviews = reviews;
        this.averageRating = averageRating;
    }

    public long getUsers() {
        return users;
    }

    public long getAdmins() {
        return admins;
    }

    public long getAuthors() {
        return authors;
    }

    public long getReaders() {
        return readers;
    }

    public long getPosts() {
        return posts;
    }

    public long getPublished() {
        return published;
    }

    public long getDrafts() {
        return drafts;
    }

    public long getComments() {
        return comments;
    }

    public long getReviews() {
        return reviews;
    }

    public Double getAverageRating() {
        return averageRating;
    }
}
