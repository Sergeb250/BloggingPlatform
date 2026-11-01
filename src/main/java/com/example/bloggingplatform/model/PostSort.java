package com.example.bloggingplatform.model;

import java.util.Comparator;

/** The orders Discover can show posts in (the "Sort" box). */
public enum PostSort {

    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    TITLE("Title A-Z"),
    TOP_RATED("Top rated");

    private final String label;

    PostSort(String label) {
        this.label = label;
    }

    /** How two posts compare in this order; ties are broken by date, newest first. */
    public Comparator<Post> comparator() {
        return switch (this) {
            case NEWEST -> PostSort::newerFirst;
            case OLDEST -> (a, b) -> newerFirst(b, a);
            case TITLE -> (a, b) -> {
                int byTitle = a.getTitle().compareToIgnoreCase(b.getTitle());
                return byTitle != 0 ? byTitle : newerFirst(a, b);
            };
            case TOP_RATED -> (a, b) -> {
                int byRating = Double.compare(ratingOf(b), ratingOf(a));   // higher rating first
                return byRating != 0 ? byRating : newerFirst(a, b);
            };
        };
    }

    /** Negative when {@code a} is newer than {@code b}; for the same second the higher id is newer. */
    private static int newerFirst(Post a, Post b) {
        int byDate = b.getCreatedAt().compareTo(a.getCreatedAt());
        return byDate != 0 ? byDate : Integer.compare(b.getId(), a.getId());
    }

    /** The average rating, or -1 for a post nobody has rated yet, so it comes last. */
    private static double ratingOf(Post post) {
        return post.getAverageRating() == null ? -1 : post.getAverageRating();
    }

    /** Text shown in the Sort box. */
    @Override
    public String toString() {
        return label;
    }
}
