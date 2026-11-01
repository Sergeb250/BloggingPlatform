package com.example.bloggingplatform.validations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReviewValidatorTest {

    @Test
    void reviewTextIsOptionalButRealWhenGiven() {
        assertNull(ReviewValidator.reviewText(null));
        assertNull(ReviewValidator.reviewText("   "));
        assertNull(ReviewValidator.reviewText("Great read"));
        assertNotNull(ReviewValidator.reviewText("ok"));
        assertNotNull(ReviewValidator.reviewText("..."));
        assertNotNull(ReviewValidator.reviewText("x".repeat(5001)));
    }

    @Test
    void ratingIsOneToFive() {
        assertNull(ReviewValidator.rating(1));
        assertNull(ReviewValidator.rating(5));
        assertNotNull(ReviewValidator.rating(null));
        assertNotNull(ReviewValidator.rating(0));
        assertNotNull(ReviewValidator.rating(6));
    }
}
