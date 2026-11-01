package com.example.bloggingplatform.dao;

import com.example.bloggingplatform.model.SiteSummary;
import com.example.bloggingplatform.model.UserActivity;

import java.util.List;
import java.util.Map;

/**
 * Reads the numbers for the admin dashboard from the users, posts, comments and reviews (read-only totals for the dashboard) tables. The services only know this interface; the MySQL code is in
 * {@code ReportDAOImpl}, so it could be replaced (e.g. by a test fake) without changing a service.
 */
public interface ReportDAO {

    /** Users per role, posts per status, comments, reviews and the average rating. */
    SiteSummary getSummary();

    /** One row per user with what they wrote; most posts first. */
    List<UserActivity> getUserActivity();

    /** Every tag name with its number of posts; most used first. */
    Map<String, Long> getPostsPerTag();
}
