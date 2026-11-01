package com.example.bloggingplatform.service;

import com.example.bloggingplatform.dao.UserDAO;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.util.PasswordHasher;
import com.example.bloggingplatform.validations.UserValidator;
import com.example.bloggingplatform.validations.Validation;
import com.example.bloggingplatform.validations.ValidationException;

import java.util.List;
import java.util.Map;

/** Sign in, register and sign out. Managing other accounts is {@link UserService}'s job. */
public class AuthService {

    private static final String WRONG_LOGIN = "Wrong username/email or password.";

    private final UserDAO users;
    private final UserService accounts;
    private final PasswordHasher hasher;
    private final Session session;

    public AuthService(UserDAO users, UserService accounts, PasswordHasher hasher, Session session) {
        this.users = users;
        this.accounts = accounts;
        this.hasher = hasher;
        this.session = session;
    }

    /**
     * Signs in with a username or email address. The error never says which of the two was
     * wrong, so it does not reveal which accounts exist.
     */
    public User login(String login, String password) {
        Validation v = new Validation();
        String name = v.text("login", login, UserValidator::login);
        v.rule("password", UserValidator.loginPassword(password));
        v.throwIfInvalid();

        User user = users.getUserByLogin(name);
        if (user == null || !hasher.matches(password, user.getPasswordHash())) {
            throw new ValidationException(WRONG_LOGIN, Map.of("password", WRONG_LOGIN));
        }
        session.signIn(user);
        return user;
    }

    /** Creates an account and signs it in. Only the roles from {@link #registrableRoles()} are accepted. */
    public User register(String username, String email, String password, String confirmPassword, Role role) {
        Validation v = new Validation();
        String name = v.text("username", username, UserValidator::username);
        String mail = v.text("email", email, UserValidator::email);
        v.rule("password", UserValidator.password(password));
        v.rule("confirm", UserValidator.confirmPassword(password, confirmPassword));
        v.required("role", "Account type", role);
        if (role != null && !registrableRoles().contains(role)) {
            v.reject("role", "Admin accounts can only be created by an admin.");
        }
        accounts.checkNotTaken(v, name, mail, 0);
        v.throwIfInvalid();

        int id = users.addUser(new User(0, name, mail, hasher.hash(password), role, null));
        User user = users.getUserById(id);
        session.signIn(user);
        return user;
    }

    public void logout() {
        session.signOut();
    }

    /**
     * The account types a visitor may pick: Reader and Author. Admin is offered only while the
     * database has no admin yet, so a fresh installation can create its first one.
     */
    public List<Role> registrableRoles() {
        if (users.countUsersByRole(Role.ADMIN) == 0) {
            return List.of(Role.READER, Role.AUTHOR, Role.ADMIN);
        }
        return List.of(Role.READER, Role.AUTHOR);
    }
}
