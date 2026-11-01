package com.example.bloggingplatform.validations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"name@example.com", "first.last+blog@mail.example.co.uk", "a_b-c%d@ex-ample.org",
            "  bob@example.com  ", "x1@a1.io"})
    void validEmailsPass(String email) {
        assertNull(UserValidator.email(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {"name.example.com", "name@@example.com", "a@b@example.com", "na me@example.com",
            ".name@example.com", "name.@example.com", "na..me@example.com", "name@example", "name@-example.com",
            "name@example-.com", "name@example.c", "name@example..com", "name@exa_mple.com", "name@example.c0m",
            "námé@example.com", "name@.com"})
    void invalidEmailsAreRejected(String email) {
        assertNotNull(UserValidator.email(email), email);
    }

    @Test
    void emailMessagesNameTheProblem() {
        assertEquals("Email is required.", UserValidator.email("   "));
        assertEquals("Email cannot contain spaces.", UserValidator.email("na me@example.com"));
        assertTrue(UserValidator.email("name.example.com").contains("exactly one '@'"));
        assertTrue(UserValidator.email("x".repeat(65) + "@example.com").contains("at most 64"));
        assertTrue(UserValidator.email("a@" + "b".repeat(250) + ".com").contains("at most 254"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc12!", "s3cret-pass", "Pa$$w0rd", "émile-2024"})
    void passwordsWithLettersNumbersAndSymbolsPass(String password) {
        assertNull(UserValidator.password(password));
    }

    @Test
    void passwordMessageListsEverythingMissing() {
        assertEquals("Password must also contain a symbol such as ! @ # $ -.", UserValidator.password("abcdef12"));
        assertEquals("Password must also contain a number.", UserValidator.password("abcdef!"));
        assertEquals("Password must also contain a letter.", UserValidator.password("123456!"));
        assertEquals("Password must also contain a number and a symbol such as ! @ # $ -.", UserValidator.password("password"));
        assertEquals("Password must be at least 6 characters long and contain a number.", UserValidator.password("ab!"));
        assertEquals("Password must be at least 6 characters long.", UserValidator.password("a1!"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void passwordIsRequired(String password) {
        assertEquals("Password is required.", UserValidator.password(password));
    }

    @Test
    void passwordsWithSpacesOrTooLongAreRejected() {
        assertEquals("Password cannot contain spaces.", UserValidator.password("abc 12!"));
        assertEquals("Password must be at most 64 characters.", UserValidator.password("a1!" + "x".repeat(62)));
        assertNull(UserValidator.password("a1!" + "x".repeat(61)), "exactly 64 characters is allowed");
    }

    @Test
    void repeatedPasswordMustMatch() {
        assertNull(UserValidator.confirmPassword("abc12!", "abc12!"));
        assertEquals("The two passwords do not match.", UserValidator.confirmPassword("abc12!", "abc12?"));
        assertEquals("Repeat the password.", UserValidator.confirmPassword("abc12!", ""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"alice", "alice_92", "a.b-c", "Bob", "abc"})
    void validUsernamesPass(String username) {
        assertNull(UserValidator.username(username));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "1alice", "_alice", "alice_", "al..ice", "al_-ice", "bad name", "alice!", "émile"})
    void invalidUsernamesAreRejected(String username) {
        assertNotNull(UserValidator.username(username), username);
    }

    @Test
    void usernameLengthIsLimited() {
        assertNull(UserValidator.username("a".repeat(50)));
        assertTrue(UserValidator.username("a".repeat(51)).contains("3-50"));
    }

    @Test
    void signInAcceptsAUsernameOrAnEmail() {
        assertNull(UserValidator.login("bob"));
        assertNull(UserValidator.login(" BOB@example.com "));
        assertEquals("Enter your username or email.", UserValidator.login(" "));
        assertNotNull(UserValidator.login("bob@"));
        assertNotNull(UserValidator.login("b o b"));
        assertEquals("Enter your password.", UserValidator.loginPassword(""));
        assertNull(UserValidator.loginPassword("any old password"), "older passwords may not follow today's rules");
    }
}
