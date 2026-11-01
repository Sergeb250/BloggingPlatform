package com.example.bloggingplatform.dao.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostDAOImplTest {

    @Test
    void likeWildcardsAndTheEscapeCharacterAreEscaped() {
        assertEquals("100\\%", PostDAOImpl.escapeLike("100%"));
        assertEquals("snake\\_case", PostDAOImpl.escapeLike("snake_case"));
        assertEquals("back\\\\slash", PostDAOImpl.escapeLike("back\\slash"));
        assertEquals("plain words", PostDAOImpl.escapeLike("plain words"));
    }
}
