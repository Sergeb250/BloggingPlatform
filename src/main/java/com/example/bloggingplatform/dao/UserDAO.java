package com.example.bloggingplatform.dao;

import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.User;

import java.util.List;

/**
 * Reads and writes the users table. The services only know this interface; the MySQL code is in
 * {@code UserDAOImpl}, so it could be replaced (e.g. by a test fake) without changing a service.
 */
public interface UserDAO {

    List<User> getAllUsers();

    /** The user, or null if there is none with this id. */
    User getUserById(int id);

    /** The user whose username or email is {@code login} (MySQL ignores case here), or null. */
    User getUserByLogin(String login);

    User getUserByUsername(String username);

    User getUserByEmail(String email);

    long countUsersByRole(Role role);

    /** Saves a new user and returns the id MySQL gave it. */
    int addUser(User user);

    /** Returns false if the user no longer exists. */
    boolean updateUser(User user);

    /** Deletes the user; their posts, comments and reviews go too (ON DELETE CASCADE). */
    boolean deleteUser(int id);
}
