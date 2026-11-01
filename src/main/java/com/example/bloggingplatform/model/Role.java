package com.example.bloggingplatform.model;

/** Account role; mirrors the {@code users.role} ENUM column. */
public enum Role {
    ADMIN,
    AUTHOR,
    READER;

    /** Only authors and admins may write posts (database design §8.3). */
    public boolean canWritePosts() {
        return this == ADMIN || this == AUTHOR;
    }
}
