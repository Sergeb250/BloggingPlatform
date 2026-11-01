package com.example.bloggingplatform.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher(1_000);

    @Test
    void hashIsSaltedAndVerifiable() {
        String first = hasher.hash("correct horse");
        String second = hasher.hash("correct horse");

        assertNotEquals(first, second, "a random salt makes every hash different");
        assertTrue(hasher.matches("correct horse", first));
        assertTrue(hasher.matches("correct horse", second));
        assertFalse(hasher.matches("wrong horse", first));
    }

    @Test
    void defaultHashFitsThePasswordHashColumn() {
        String hash = new PasswordHasher().hash("Password123!");

        assertTrue(hash.startsWith("pbkdf2-sha256$600000$"));
        assertTrue(hash.length() <= 255, "users.password_hash is VARCHAR(255)");
    }

    @Test
    void malformedHashesNeverMatch() {
        assertFalse(hasher.matches("x", null));
        assertFalse(hasher.matches("x", "plain-text"));
        assertFalse(hasher.matches("x", "pbkdf2-sha256$abc$!!$!!"));
    }
}
