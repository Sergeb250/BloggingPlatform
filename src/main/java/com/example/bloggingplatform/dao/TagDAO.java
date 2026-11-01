package com.example.bloggingplatform.dao;

import com.example.bloggingplatform.model.Tag;

import java.util.List;

/**
 * Reads and writes the tags table. The services only know this interface; the MySQL code is in
 * {@code TagDAOImpl}, so it could be replaced (e.g. by a test fake) without changing a service.
 */
public interface TagDAO {

    List<Tag> getAllTags();

    /** The tag, or null if there is none with this id. */
    Tag getTagById(int id);

    /** The tag with this name, or null. */
    Tag getTagByName(String name);

    /** The tags on the author's own posts (a tag has no owner column, post_tags → posts.author_id tells). */
    List<Tag> getTagsUsedBy(int authorId);

    /** Saves a new tag and returns the id MySQL gave it. */
    int addTag(Tag tag);

    /** Renames the tag; returns false if it no longer exists. */
    boolean updateTag(Tag tag);

    /** Deletes the tag; its links to posts go too (ON DELETE CASCADE), the posts stay. */
    boolean deleteTag(int id);
}
