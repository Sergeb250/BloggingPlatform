package com.example.bloggingplatform.validations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostValidatorTest {

    @Test
    void titleNeedsThreeToTwoHundredRealCharacters() {
        assertNull(PostValidator.title("Java 21"));
        assertEquals("Title is required.", PostValidator.title("   "));
        assertEquals("Title must be at least 3 characters.", PostValidator.title("Hi"));
        assertTrue(PostValidator.title("x".repeat(201)).contains("at most 200"));
        assertEquals("Title must contain letters or digits, not only symbols.", PostValidator.title("!!!"));
    }

    @Test
    void contentNeedsTwentyCharactersAndFitsATextColumn() {
        assertNull(PostValidator.content("Records make data classes short."));
        assertEquals("Content must be at least 20 characters (now 5).", PostValidator.content("Short"));
        assertTrue(PostValidator.content("é".repeat(40_000)).contains("65,535 bytes"));
        assertEquals("Content must contain words, not only symbols.", PostValidator.content("-".repeat(25)));
    }
}
