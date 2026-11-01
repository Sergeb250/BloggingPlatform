package com.example.bloggingplatform.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A row of the {@code posts} table, plus values the screens show next to it: the author's name,
 * the tags (from {@code post_tags}), the number of comments and the average rating.
 */
public class Post {

    private int id;                          // 0 until the post is saved
    private int authorId;
    private String authorName;               // users.username of the author (read from a join)
    private String title;
    private String content;
    private PostStatus status;
    private List<Tag> tags = new ArrayList<>();
    private int commentCount;                // counted by the query, not stored
    private Double averageRating;            // null when the post has no reviews
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Post() {
    }

    /** A post as typed in the form: only the values the user controls. */
    public Post(int id, int authorId, String title, String content, PostStatus status, List<Tag> tags) {
        this.id = id;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
        this.status = status;
        setTags(tags);
    }

    public Post(int id, int authorId, String authorName, String title, String content, PostStatus status,
                List<Tag> tags, int commentCount, Double averageRating, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, authorId, title, content, status, tags);
        this.authorName = authorName;
        this.commentCount = commentCount;
        this.averageRating = averageRating;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAuthorId() {
        return authorId;
    }

    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public PostStatus getStatus() {
        return status;
    }

    public void setStatus(PostStatus status) {
        this.status = status;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** Tag names joined for display, e.g. {@code "java, mysql"}. */
    public String getTagNames() {
        List<String> names = new ArrayList<>();
        for (Tag tag : tags) {
            names.add(tag.getName());
        }
        return String.join(", ", names);
    }

    /** Two Post objects are the same post when they have the same id. */
    @Override
    public boolean equals(Object other) {
        return other instanceof Post post && post.id == id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    /** Text shown in drop-down lists. */
    @Override
    public String toString() {
        return title;
    }
}
