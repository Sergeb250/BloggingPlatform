package com.example.bloggingplatform.model;

import java.util.Objects;

/** What the post list is filtered on. A {@code null} value means "do not filter on this". */
public class PostFilter {

    private final PostStatus status;
    private final Integer authorId;
    private final Integer tagId;
    private final String keyword;      // searched in title and content, ignoring case

    public PostFilter(PostStatus status, Integer authorId, Integer tagId, String keyword) {
        this.status = status;
        this.authorId = authorId;
        this.tagId = tagId;
        this.keyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
    }

    /** No filter: every post. */
    public static PostFilter none() {
        return new PostFilter(null, null, null, null);
    }

    public PostStatus getStatus() {
        return status;
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public Integer getTagId() {
        return tagId;
    }

    public String getKeyword() {
        return keyword;
    }

    /** Two filters are equal when they filter on the same values (the screens skip reloading then). */
    @Override
    public boolean equals(Object other) {
        return other instanceof PostFilter filter && status == filter.status && Objects.equals(authorId, filter.authorId)
                && Objects.equals(tagId, filter.tagId) && Objects.equals(keyword, filter.keyword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, authorId, tagId, keyword);
    }
}
