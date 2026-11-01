package com.example.bloggingplatform.dao;

import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostFilter;

import java.util.List;

/**
 * Reads and writes the posts and post_tags tables. The services only know this interface; the MySQL code is in
 * {@code PostDAOImpl}, so it could be replaced (e.g. by a test fake) without changing a service.
 */
public interface PostDAO {

    List<Post> getAllPosts();

    /** The post, or null if there is none with this id. */
    Post getPostById(int id);

    /** One page of the posts that match the filter, newest first. */
    List<Post> getPostsPage(PostFilter filter, int offset, int limit);

    /** How many posts match the filter (for the page count). */
    long countPosts(PostFilter filter);

    /** True if the author has another post with this title ({@code exceptPostId} is 0 for a new post). */
    boolean isTitleTaken(int authorId, String title, int exceptPostId);

    /** Saves a new post and its tag links together (one transaction) and returns the id MySQL gave it. */
    int addPost(Post post);

    /** Updates the post and replaces its tag links (one transaction). Returns false if the post no longer exists. */
    boolean updatePost(Post post);

    /** Deletes the post; its comments, reviews and tag links go too (ON DELETE CASCADE). */
    boolean deletePost(int id);
}
