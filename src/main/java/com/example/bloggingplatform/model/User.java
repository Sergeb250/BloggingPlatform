package com.example.bloggingplatform.model;

import java.time.LocalDateTime;

/** A row of the {@code users} table: a reader, an author or an admin. */
public class User {

    private int id;                    // 0 until the user is saved
    private String username;
    private String email;
    private String passwordHash;       // salted PBKDF2 hash, never the plain password
    private Role role;
    private LocalDateTime createdAt;   // set by the database

    public User() {
    }

    public User(int id, String username, String email, String passwordHash, Role role, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** Two User objects are the same user when they have the same id. */
    @Override
    public boolean equals(Object other) {
        return other instanceof User user && user.id == id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    /** Text shown in drop-down lists. It also keeps the password hash out of logs. */
    @Override
    public String toString() {
        return username + " (" + role + ")";
    }
}
