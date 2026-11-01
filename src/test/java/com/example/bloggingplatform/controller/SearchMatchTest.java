package com.example.bloggingplatform.controller;

import com.example.bloggingplatform.model.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The in-memory search used by the comment, review, tag and user screens. */
class SearchMatchTest {

    @Test
    void matchingIgnoresCaseAndSurroundingSpaces() {
        assertTrue(BaseController.matches("  JAVA ", "Learning java records"));
        assertTrue(BaseController.matches("auth", "bob", Role.AUTHOR));
        assertFalse(BaseController.matches("python", "Learning java records", null));
    }

    @Test
    void anEmptySearchMatchesEverything() {
        assertTrue(BaseController.matches("", "anything"));
        assertTrue(BaseController.matches(null, (Object) null));
    }

    @Test
    void excerptsAndReadingTimeForCards() {
        assertTrue(BlogWidgets.excerpt("one two three four five", 12).endsWith("…"));
        assertTrue(BlogWidgets.readingTime("word ".repeat(450)).startsWith("3 "));
        assertTrue(BlogWidgets.readingTime("").startsWith("1 "));
    }
}
