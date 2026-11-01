package com.example.bloggingplatform.service;

import com.example.bloggingplatform.cache.PostCache;
import com.example.bloggingplatform.validations.ValidationException;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.util.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private final PostCache cache = new PostCache(new FakeDaos.Posts());

    private final FakeDaos.Users users = new FakeDaos.Users();
    private final PasswordHasher hasher = new PasswordHasher(1_000);   // fast for tests
    private final Session session = new Session();
    private final AuthService auth = new AuthService(users, new UserService(users, hasher, session, cache), hasher, session);

    private User account(String username, String password, Role role) {
        int id = users.addUser(new User(0, username, username + "@example.com", hasher.hash(password), role, null));
        return users.getUserById(id);
    }

    @Test
    void loginWithUsernameOrEmailSignsIn() {
        User bob = account("bob", "secret-password", Role.AUTHOR);

        assertEquals(bob, auth.login("bob", "secret-password"));
        assertEquals(bob, session.require());
        auth.logout();
        assertNull(session.getUser());

        assertEquals(bob, auth.login("  BOB@example.com ", "secret-password"));
    }

    @Test
    void wrongPasswordAndUnknownUserGetTheSameMessage() {
        account("bob", "secret-password", Role.AUTHOR);

        ValidationException wrongPassword = assertThrows(ValidationException.class,
                () -> auth.login("bob", "not-the-password"));
        ValidationException unknownUser = assertThrows(ValidationException.class,
                () -> auth.login("nobody", "secret-password"));

        assertEquals(wrongPassword.getMessage(), unknownUser.getMessage());
        assertNull(session.getUser());
    }

    @Test
    void registerCreatesAHashedAccountAndSignsItIn() {
        account("alice", "admin-password", Role.ADMIN);

        User carol = auth.register(" carol ", "carol@example.com", "my-pass-1", "my-pass-1", Role.READER);

        assertEquals("carol", carol.getUsername());
        assertEquals(Role.READER, carol.getRole());
        assertTrue(hasher.matches("my-pass-1", carol.getPasswordHash()));
        assertEquals(carol, session.require());
    }

    @Test
    void passwordsMustMatchAndBeLongEnough() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> auth.register("carol", "carol@example.com", "my-pass-1", "my-pass-2", Role.READER));
        assertEquals(List.of("confirm"), List.copyOf(e.fieldErrors().keySet()));

        e = assertThrows(ValidationException.class,
                () -> auth.register("carol", "carol@example.com", "short", "short", Role.READER));
        assertTrue(e.fieldErrors().containsKey("password"));
    }

    @Test
    void adminCanOnlyBeChosenWhileNoAdminExists() {
        assertEquals(List.of(Role.READER, Role.AUTHOR, Role.ADMIN), auth.registrableRoles());
        auth.register("first", "first@example.com", "my-pass-1", "my-pass-1", Role.ADMIN);

        assertEquals(List.of(Role.READER, Role.AUTHOR), auth.registrableRoles());
        ValidationException e = assertThrows(ValidationException.class,
                () -> auth.register("second", "second@example.com", "my-pass-1", "my-pass-1", Role.ADMIN));
        assertTrue(e.fieldErrors().containsKey("role"));
    }
}
