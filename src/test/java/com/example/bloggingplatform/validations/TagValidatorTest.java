package com.example.bloggingplatform.validations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TagValidatorTest {

    @Test
    void tagsAreSlugs() {
        assertEquals("spring-boot", TagValidator.slug("  Spring   Boot "));
        assertNull(TagValidator.name("java-21"));
        assertEquals("Tag name is required.", TagValidator.name(""));
        assertNotNull(TagValidator.name("a"));
        assertNotNull(TagValidator.name("c#"));
        assertNotNull(TagValidator.name("spring--boot"));
        assertNotNull(TagValidator.name("a".repeat(51)));
    }
}
