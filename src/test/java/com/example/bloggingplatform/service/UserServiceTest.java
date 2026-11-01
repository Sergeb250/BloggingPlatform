package com.example.bloggingplatform.service;

import com.example.bloggingplatform.cache.PostCache;
import com.example.bloggingplatform.validations.ValidationException;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.util.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    private final PostCache cache = new PostCache(new FakeDaos.Posts());

    private final FakeDaos.Users users = new FakeDaos.Users();
    private final PasswordHasher hasher = new PasswordHasher(1_000);   // fast for tests
    private final UserService service = new UserService(users, hasher, FakeDaos.adminSession(), cache);

    @Test
    void createTrimsInputAndStoresOnlyAHash() {
        User user = service.create("  alice ", " alice@example.com ", "s3cret-pass", Role.AUTHOR);

        assertEquals("alice", user.getUsername());
        assertEquals("alice@example.com", user.getEmail());
        assertNotEquals("s3cret-pass", user.getPasswordHash());
        assertTrue(hasher.matches("s3cret-pass", user.getPasswordHash()));
    }

    @Test
    void createReportsEveryInvalidFieldAtOnce() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create("ab", "not-an-email", "short", null));

        assertEquals(Set.of("username", "email", "password", "role"), e.fieldErrors().keySet());
        assertTrue(users.rows.isEmpty(), "nothing may be saved when validation fails");
    }

    @Test
    void usernameMayNotContainSpaces() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create("bad name", "bad@example.com", "password-1", Role.READER));

        assertEquals(Set.of("username"), e.fieldErrors().keySet());
    }

    @Test
    void blankPasswordOnUpdateKeepsTheOldHash() {
        User user = service.create("bob", "bob@example.com", "password-1", Role.READER);

        User updated = service.update(user.getId(), "bobby", "bob@example.com", "", Role.AUTHOR);

        assertEquals(user.getPasswordHash(), updated.getPasswordHash());
        assertEquals("bobby", updated.getUsername());
        assertEquals(Role.AUTHOR, updated.getRole());
    }

    @Test
    void newPasswordOnUpdateIsHashed() {
        User user = service.create("carol", "carol@example.com", "password-1", Role.READER);

        User updated = service.update(user.getId(), "carol", "carol@example.com", "password-2", Role.READER);

        assertTrue(hasher.matches("password-2", updated.getPasswordHash()));
    }

    @Test
    void updatingADeletedUserFails() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.update(42, "ghost", "ghost@example.com", "", Role.READER));

        assertTrue(e.fieldErrors().isEmpty());
    }

    @Test
    void findAuthorsLeavesOutReaders() {
        users.add("writer", Role.AUTHOR);
        users.add("boss", Role.ADMIN);
        users.add("fan", Role.READER);

        List<String> names = service.findAuthors().stream().map(User::getUsername).toList();

        assertEquals(List.of("writer", "boss"), names);
    }

    @Test
    void usernamesAndEmailsMustBeFree() {
        service.create("bob", "bob@example.com", "password-1", Role.READER);

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create("BOB", "Bob@Example.com", "password-1", Role.READER));

        assertEquals(Set.of("username", "email"), e.fieldErrors().keySet(), "checked ignoring case, like MySQL");
        assertEquals("That username is already taken.", e.fieldErrors().get("username"));
    }

    @Test
    void anAccountMayKeepItsOwnUsernameAndEmail() {
        User user = service.create("carol", "carol@example.com", "password-1", Role.READER);

        User updated = service.update(user.getId(), "carol", "carol@example.com", "", Role.AUTHOR);

        assertEquals(Role.AUTHOR, updated.getRole());
    }
}
