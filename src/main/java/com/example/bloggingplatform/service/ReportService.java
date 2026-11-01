package com.example.bloggingplatform.service;

import com.example.bloggingplatform.dao.PostDAO;
import com.example.bloggingplatform.dao.ReportDAO;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.SiteSummary;
import com.example.bloggingplatform.model.UserActivity;

import java.util.ArrayList;
import java.util.List;

/** The admin dashboard: site totals, activity per user and the stand-out posts. Admins only. */
public class ReportService {

    private final ReportDAO reports;
    private final PostDAO posts;
    private final Session session;

    public ReportService(ReportDAO reports, PostDAO posts, Session session) {
        this.reports = reports;
        this.posts = posts;
        this.session = session;
    }

    public SiteSummary summary() {
        session.requireAdmin("see reports");
        return reports.getSummary();
    }

    public List<UserActivity> userActivity() {
        session.requireAdmin("see reports");
        return reports.getUserActivity();
    }

    /** The {@code limit} best-rated posts (only posts with at least one review); ties: more comments first. */
    public List<Post> topRated(int limit) {
        session.requireAdmin("see reports");
        List<Post> rated = new ArrayList<>();
        for (Post post : posts.getAllPosts()) {
            if (post.getAverageRating() != null) {
                rated.add(post);
            }
        }
        rated.sort((a, b) -> {
            int byRating = Double.compare(b.getAverageRating(), a.getAverageRating());
            return byRating != 0 ? byRating : Integer.compare(b.getCommentCount(), a.getCommentCount());
        });
        return rated.subList(0, Math.min(limit, rated.size()));
    }

    /** The {@code limit} posts with the most comments (only posts that have comments). */
    public List<Post> mostCommented(int limit) {
        session.requireAdmin("see reports");
        List<Post> commented = new ArrayList<>();
        for (Post post : posts.getAllPosts()) {
            if (post.getCommentCount() > 0) {
                commented.add(post);
            }
        }
        commented.sort((a, b) -> Integer.compare(b.getCommentCount(), a.getCommentCount()));
        return commented.subList(0, Math.min(limit, commented.size()));
    }
}
