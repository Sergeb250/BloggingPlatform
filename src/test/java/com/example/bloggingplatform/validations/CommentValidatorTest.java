package com.example.bloggingplatform.validations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentValidatorTest {

    @Test
    void commentNeedsRealText() {
        assertNull(CommentValidator.comment("Hi"));
        assertEquals("Comment is required.", CommentValidator.comment(""));
        assertEquals("Comment must be at least 2 characters.", CommentValidator.comment("x"));
        assertEquals("Comment must contain words, not only symbols.", CommentValidator.comment("??"));
        assertTrue(CommentValidator.comment("x".repeat(2001)).contains("at most 2000"));
    }
}
