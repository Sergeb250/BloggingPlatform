package com.example.bloggingplatform.service;

import com.example.bloggingplatform.cache.PostCache;
import com.example.bloggingplatform.dao.UserDAO;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.util.PasswordHasher;
import com.example.bloggingplatform.validations.UserValidator;
import com.example.bloggingplatform.validations.Validation;
import com.example.bloggingplatform.validations.ValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * The Users screen: admins list, create, edit and delete accounts. Passwords are hashed here, so
 * the DAO never sees plain text. Signing in and registering are {@link AuthService}'s job.
 * Editing an account can rename an author and deleting one deletes their posts, so both empty the
 * {@link PostCache}.
 */
public class UserService {

    private final UserDAO users;
    private final PasswordHasher hasher;
    private final Session session;
    private final PostCache cache;

    public UserService(UserDAO users, PasswordHasher hasher, Session session, PostCache cache) {
        this.users = users;
        this.hasher = hasher;
        this.session = session;
        this.cache = cache;
    }

    public List<User> findAll() {
        session.requireAdmin("see the list of users");
        return users.getAllUsers();
    }

    /** The users who may write posts (authors and admins); anyone may see them, e.g. to filter by author. */
    public List<User> findAuthors() {
        List<User> authors = new ArrayList<>();
        for (User user : users.getAllUsers()) {
            if (user.getRole().canWritePosts()) {
                authors.add(user);
            }
        }
        return authors;
    }

    public User create(String username, String email, String password, Role role) {
        session.requireAdmin("create accounts");
        Validation v = new Validation();
        String name = v.text("username", username, UserValidator::username);
        String mail = v.text("email", email, UserValidator::email);
        v.rule("password", UserValidator.password(password));
        v.required("role", "Role", role);
        checkNotTaken(v, name, mail, 0);
        v.throwIfInvalid();

        int id = users.addUser(new User(0, name, mail, hasher.hash(password), role, null));
        return users.getUserById(id);
    }

    /**
     * Updates an account.
     *
     * @param newPassword a new password, or blank to keep the current one
     */
    public User update(int id, String username, String email, String newPassword, Role role) {
        User admin = session.requireAdmin("edit accounts");
        User existing = users.getUserById(id);
        if (existing == null) {
            throw notFound();
        }
        Validation v = new Validation();
        String name = v.text("username", username, UserValidator::username);
        String mail = v.text("email", email, UserValidator::email);
        v.rule("password", UserValidator.newPassword(newPassword));
        v.required("role", "Role", role);
        if (id == admin.getId() && role != null && role != Role.ADMIN) {
            v.reject("role", "You cannot remove the admin role from your own account.");
        }
        checkNotTaken(v, name, mail, id);
        v.throwIfInvalid();

        existing.setUsername(name);
        existing.setEmail(mail);
        existing.setRole(role);
        if (newPassword != null && !newPassword.isEmpty()) {
            existing.setPasswordHash(hasher.hash(newPassword));   // blank keeps the current password
        }
        if (!users.updateUser(existing)) {
            throw notFound();
        }
        cache.clear();
        User updated = users.getUserById(id);
        if (id == admin.getId()) {
            session.signIn(updated);   // keep the signed-in name and email current
        }
        return updated;
    }

    /** Deletes the account together with everything it wrote (ON DELETE CASCADE). */
    public void delete(int id) {
        User admin = session.requireAdmin("delete accounts");
        if (id == admin.getId()) {
            throw ValidationException.of("You cannot delete the account you are signed in with.");
        }
        if (!users.deleteUser(id)) {
            throw notFound();
        }
        cache.clear();
    }

    /** Usernames and emails are unique; {@code userId} is the account being edited, 0 for a new one. */
    void checkNotTaken(Validation v, String username, String email, int userId) {
        if (!v.hasError("username")) {
            User sameName = users.getUserByUsername(username);
            if (sameName != null && sameName.getId() != userId) {
                v.reject("username", "That username is already taken.");
            }
        }
        if (!v.hasError("email")) {
            User sameEmail = users.getUserByEmail(email);
            if (sameEmail != null && sameEmail.getId() != userId) {
                v.reject("email", "That email address is already registered.");
            }
        }
    }

    private static ValidationException notFound() {
        return ValidationException.of("This user no longer exists. Refresh the list.");
    }
}
