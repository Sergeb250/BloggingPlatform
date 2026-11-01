package com.example.bloggingplatform.model;

import java.time.LocalDateTime;

/** A row of the {@code comments} table, plus the post title and the writer's name for display. */
public class Comment {

    private int id;                    // 0 until the comment is saved
    private int postId;
    private String postTitle;          // title of the commented post (read from a join)
    private int userId;
    private String username;           // name of the writer (read from a join)
    private String content;
    private LocalDateTime createdAt;

    public Comment() {
    }

    /** A comment as typed in the form: only the values the user controls. */
    public Comment(int id, int postId, int userId, String content) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.content = content;
    }

    public Comment(int id, int postId, String postTitle, int userId, String username, String content,
                   LocalDateTime createdAt) {
        this(id, postId, userId, content);
        this.postTitle = postTitle;
        this.username = username;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** Two Comment objects are the same comment when they have the same id. */
    @Override
    public boolean equals(Object other) {
        return other instanceof Comment comment && comment.id == id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
