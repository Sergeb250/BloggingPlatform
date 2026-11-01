package com.example.bloggingplatform.controller;

/** Which records a table screen shows; the same screen serves several roles. */
public enum Scope {
    /** Every record (admins). */
    ALL,
    /** Records the signed-in user wrote. */
    MINE,
    /** Comments or reviews on the signed-in author's posts. */
    ON_MY_POSTS
}
