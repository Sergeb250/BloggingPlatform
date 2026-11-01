package com.example.bloggingplatform.service;

import com.example.bloggingplatform.cache.PostCache;
import com.example.bloggingplatform.validations.ValidationException;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Review;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReviewServiceTest {

    private final FakeDaos.Users users = new FakeDaos.Users();
    private final FakeDaos.Posts posts = new FakeDaos.Posts();
    private final PostCache cache = new PostCache(posts);
    private final FakeDaos.Reviews reviews = new FakeDaos.Reviews();
    private final ReviewService service = new ReviewService(reviews, posts, users, FakeDaos.adminSession(), cache);

    private final User author = users.add("alice", Role.AUTHOR);
    private final User reader = users.add("bob", Role.READER);
    private final Post published = posts.add(author, "Published", PostStatus.PUBLISHED);

    @ParameterizedTest
    @ValueSource(ints = {0, 6, -1})
    void ratingMustBeBetweenOneAndFive(int rating) {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(published.getId(), reader.getId(), rating, null));

        assertEquals(Set.of("rating"), e.fieldErrors().keySet());
    }

    @Test
    void ratingIsRequired() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(published.getId(), reader.getId(), null, "text"));

        assertEquals(Set.of("rating"), e.fieldErrors().keySet());
    }

    @Test
    void authorsCannotReviewTheirOwnPost() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(published.getId(), author.getId(), 5, "Mine is great"));

        assertEquals("Authors cannot review their own posts.", e.fieldErrors().get("user"));
    }

    @Test
    void draftPostsCannotBeReviewed() {
        Post draft = posts.add(author, "Draft", PostStatus.DRAFT);

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(draft.getId(), reader.getId(), 3, null));

        assertEquals(Set.of("post"), e.fieldErrors().keySet());
    }

    @Test
    void blankTextIsStoredAsNull() {
        Review review = service.create(published.getId(), reader.getId(), 4, "   ");

        assertNull(review.getReviewText());
    }

    @Test
    void textIsLimitedTo5000Characters() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(published.getId(), reader.getId(), 4, "x".repeat(5001)));

        assertEquals(Set.of("text"), e.fieldErrors().keySet());
    }

    @Test
    void averageIsComputedFromRatings() {
        List<Review> list = List.of(new Review(1, 1, 1, 4, null), new Review(2, 1, 2, 5, null));

        assertEquals(4.5, ReviewService.average(list));
        assertNull(ReviewService.average(List.of()));
    }

    @Test
    void aUserReviewsAPostOnlyOnce() {
        service.create(published.getId(), reader.getId(), 4, "Good read");

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(published.getId(), reader.getId(), 5, "Even better"));

        assertEquals(Set.of("user"), e.fieldErrors().keySet());
        assertEquals(1, reviews.rows.size());
    }
}
