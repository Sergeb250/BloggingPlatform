package com.example.bloggingplatform.service;

import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.User;

/**
 * The account signed in to this copy of the app. The services check it before every change, so
 * the role rules hold no matter which screen calls them; hiding a button in the UI is only a
 * convenience on top. Only {@link UserService} signs users in and out.
 */
public class Session {

    private User user;   // null while nobody is signed in

    /** The signed-in user, or null. */
    public User getUser() {
        return user;
    }

    public boolean isAdmin() {
        return user != null && user.getRole() == Role.ADMIN;
    }

    /** True if someone is signed in and their id is {@code userId}. */
    public boolean is(int userId) {
        return user != null && user.getId() == userId;
    }

    /** The signed-in user; refuses the action if nobody is signed in. */
    public User require() {
        if (user == null) {
            throw new AccessDeniedException("Please sign in first.");
        }
        return user;
    }

    /** The signed-in user, who must be an admin; {@code action} completes "Only admins can ...". */
    public User requireAdmin(String action) {
        User current = require();
        if (current.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can " + action + ".");
        }
        return current;
    }

    /** The signed-in user, who must be the owner ({@code ownerId}) of the record or an admin. */
    public User requireOwnerOrAdmin(int ownerId, String deniedMessage) {
        User current = require();
        if (current.getRole() != Role.ADMIN && current.getId() != ownerId) {
            throw new AccessDeniedException(deniedMessage);
        }
        return current;
    }

    void signIn(User user) {
        this.user = user;
    }

    void signOut() {
        this.user = null;
    }
}
