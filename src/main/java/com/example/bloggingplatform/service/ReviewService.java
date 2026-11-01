package com.example.bloggingplatform.service;

import com.example.bloggingplatform.cache.PostCache;
import com.example.bloggingplatform.dao.PostDAO;
import com.example.bloggingplatform.dao.ReviewDAO;
import com.example.bloggingplatform.dao.UserDAO;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Review;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.validations.ReviewValidator;
import com.example.bloggingplatform.validations.Validation;
import com.example.bloggingplatform.validations.ValidationException;

import java.util.List;

/**
 * Reviews: a rating of 1 to 5 with optional text, only on published posts, never on your own post,
 * and one per user per post. Everyone reviews as themselves and changes only their own reviews
 * (admins may change any). Post authors see the reviews of their posts but cannot change them.
 * Every change moves the post's average rating, so it empties the {@link PostCache}.
 */
public class ReviewService {

    private final ReviewDAO reviews;
    private final PostDAO posts;
    private final UserDAO users;
    private final Session session;
    private final PostCache cache;

    public ReviewService(ReviewDAO reviews, PostDAO posts, UserDAO users, Session session, PostCache cache) {
        this.reviews = reviews;
        this.posts = posts;
        this.users = users;
        this.session = session;
        this.cache = cache;
    }

    /** The reviews of one post; or, for admins, every review when {@code postId} is null. */
    public List<Review> find(Integer postId) {
        if (postId != null) {
            return reviews.getReviewsByPost(postId);
        }
        session.requireAdmin("list every review");
        return reviews.getAllReviews();
    }

    /** The reviews the signed-in user wrote. */
    public List<Review> findMine() {
        return reviews.getReviewsByUser(session.require().getId());
    }

    /** The reviews of the signed-in author's posts. */
    public List<Review> findOnMyPosts() {
        return reviews.getReviewsOnPostsOf(session.require().getId());
    }

    /** Adds a review. Users review as themselves; only an admin may pick another reviewer. */
    public Review create(Integer postId, Integer userId, Integer rating, String text) {
        User me = session.require();
        Integer reviewer = userId;
        if (me.getRole() != Role.ADMIN) {
            reviewer = me.getId();
        }

        Validation v = new Validation();
        v.required("post", "Post", postId);
        v.required("user", "Reviewer", reviewer);
        v.rule("rating", ReviewValidator.rating(rating));
        String reviewText = v.optionalText("text", text, ReviewValidator::reviewText);
        if (postId != null) {
            Post post = posts.getPostById(postId);
            if (post == null) {
                v.reject("post", "The selected post no longer exists.");
            } else if (post.getStatus() != PostStatus.PUBLISHED) {
                v.reject("post", "Reviews are only allowed on published posts.");
            } else if (reviewer != null && post.getAuthorId() == reviewer) {
                v.reject("user", "Authors cannot review their own posts.");
            } else if (reviewer != null && hasReviewed(postId, reviewer)) {
                v.reject("user", "This user has already reviewed this post; edit that review instead.");
            }
        }
        if (reviewer != null && users.getUserById(reviewer) == null) {
            v.reject("user", "The selected user no longer exists.");
        }
        v.throwIfInvalid();

        int id = reviews.addReview(new Review(0, postId, reviewer, rating, reviewText));
        cache.clear();
        return reviews.getReviewById(id);
    }

    /** Changes rating and text; the post and reviewer stay the same. */
    public Review update(int id, Integer rating, String text) {
        Review existing = reviews.getReviewById(id);
        if (existing == null) {
            throw notFound();
        }
        session.requireOwnerOrAdmin(existing.getUserId(), "You can only edit your own reviews.");
        Validation v = new Validation();
        v.rule("rating", ReviewValidator.rating(rating));
        String reviewText = v.optionalText("text", text, ReviewValidator::reviewText);
        v.throwIfInvalid();

        existing.setRating(rating);
        existing.setReviewText(reviewText);
        if (!reviews.updateReview(existing)) {
            throw notFound();
        }
        cache.clear();
        return reviews.getReviewById(id);
    }

    public void delete(int id) {
        Review existing = reviews.getReviewById(id);
        if (existing == null) {
            throw notFound();
        }
        session.requireOwnerOrAdmin(existing.getUserId(), "You can only delete your own reviews.");
        if (!reviews.deleteReview(id)) {
            throw notFound();
        }
        cache.clear();
    }

    /** True if the signed-in user may edit or delete the review. */
    public boolean canManage(Review review) {
        return session.isAdmin() || session.is(review.getUserId());
    }

    /** The average rating of the given reviews, or null if there are none. */
    public static Double average(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return null;
        }
        int total = 0;
        for (Review review : reviews) {
            total += review.getRating();
        }
        return (double) total / reviews.size();
    }

    /** True if this user already reviewed this post (the database allows one review per user per post). */
    private boolean hasReviewed(int postId, int userId) {
        for (Review review : reviews.getReviewsByPost(postId)) {
            if (review.getUserId() == userId) {
                return true;
            }
        }
        return false;
    }

    private static ValidationException notFound() {
        return ValidationException.of("This review no longer exists. Refresh the list.");
    }
}
