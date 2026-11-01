package com.example.bloggingplatform.service;

import com.example.bloggingplatform.cache.PostCache;
import com.example.bloggingplatform.validations.ValidationException;
import com.example.bloggingplatform.model.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagServiceTest {

    private final PostCache cache = new PostCache(new FakeDaos.Posts());

    private final FakeDaos.Tags tags = new FakeDaos.Tags();
    private final TagService service = new TagService(tags, FakeDaos.adminSession(), cache);

    @Test
    void namesAreSavedAsLowerCaseSlugs() {
        Tag tag = service.create("  Spring   Boot ");

        assertEquals("spring-boot", tag.getName());
        assertEquals("spring-boot", tags.rows.get(tag.getId()).getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"c++", "java--21", "-java", "naïve"})
    void namesWithOtherCharactersAreRejected(String name) {
        ValidationException e = assertThrows(ValidationException.class, () -> service.create(name));

        assertEquals(Set.of("name"), e.fieldErrors().keySet());
        assertTrue(tags.rows.isEmpty());
    }

    @Test
    void blankAndTooLongNamesAreRejected() {
        assertThrows(ValidationException.class, () -> service.create("   "));
        assertThrows(ValidationException.class, () -> service.create("a".repeat(51)));
    }

    @Test
    void renamingADeletedTagFails() {
        assertThrows(ValidationException.class, () -> service.rename(5, "java"));
    }

    @Test
    void tagNamesMustBeFree() {
        Tag java = service.create("java");

        ValidationException e = assertThrows(ValidationException.class, () -> service.create("Java"));

        assertEquals("A tag with that name already exists.", e.fieldErrors().get("name"));
        assertEquals("java", service.rename(java.getId(), "java").getName(), "renaming to its own name is fine");
    }
}
