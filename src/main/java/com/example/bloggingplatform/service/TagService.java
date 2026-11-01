package com.example.bloggingplatform.service;

import com.example.bloggingplatform.cache.PostCache;
import com.example.bloggingplatform.dao.TagDAO;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.Tag;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.validations.TagValidator;
import com.example.bloggingplatform.validations.Validation;
import com.example.bloggingplatform.validations.ValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Tags. Names are stored as lower-case slugs such as {@code spring-boot}. Everyone may list the
 * tags; authors add new tags by typing them in the post form ({@link PostService} saves them), and
 * the Tags screen (create, rename, delete) is for admins. Renaming or deleting a tag changes the
 * tags shown on posts, so it empties the {@link PostCache}.
 */
public class TagService {

    private final TagDAO tags;
    private final Session session;
    private final PostCache cache;

    public TagService(TagDAO tags, Session session, PostCache cache) {
        this.tags = tags;
        this.session = session;
        this.cache = cache;
    }

    public List<Tag> findAll() {
        return tags.getAllTags();
    }

    /** The tags offered in the post form: the author's own tags, or every tag for an admin. */
    public List<Tag> findForWriting() {
        User me = session.require();
        if (me.getRole() == Role.ADMIN) {
            return tags.getAllTags();
        }
        return tags.getTagsUsedBy(me.getId());
    }

    public Tag create(String name) {
        session.requireAdmin("create tags here; authors add tags while writing a post");
        String slug = checkName(name, 0);
        int id = tags.addTag(new Tag(0, slug));
        return new Tag(id, slug);
    }

    public Tag rename(int id, String name) {
        session.requireAdmin("rename tags");
        String slug = checkName(name, id);
        if (!tags.updateTag(new Tag(id, slug))) {
            throw notFound();
        }
        cache.clear();
        return new Tag(id, slug);
    }

    /**
     * The tags with these names, for saving a post: a name that is not a tag yet becomes a new tag.
     * Any signed-in writer may do this (the names were checked by {@link PostService} first).
     */
    public List<Tag> findOrCreate(List<String> slugs) {
        List<Tag> result = new ArrayList<>();
        for (String slug : slugs) {
            Tag tag = tags.getTagByName(slug);
            if (tag == null) {
                int id = tags.addTag(new Tag(0, slug));
                tag = new Tag(id, slug);
            }
            result.add(tag);
        }
        return result;
    }

    /** Deletes the tag and removes it from every post; the posts themselves stay. */
    public void delete(int id) {
        session.requireAdmin("delete tags");
        if (!tags.deleteTag(id)) {
            throw notFound();
        }
        cache.clear();
    }

    /**
     * Turns the name into a slug ("Spring Boot" → "spring-boot"), checks it, and checks that no
     * other tag has it ({@code tagId} is 0 for a new tag).
     */
    private String checkName(String name, int tagId) {
        String slug = TagValidator.slug(name);
        Validation v = new Validation();
        v.rule("name", TagValidator.name(slug));
        if (!v.hasError("name")) {
            Tag sameName = tags.getTagByName(slug);
            if (sameName != null && sameName.getId() != tagId) {
                v.reject("name", "A tag with that name already exists.");
            }
        }
        v.throwIfInvalid();
        return slug;
    }

    private static ValidationException notFound() {
        return ValidationException.of("This tag no longer exists. Refresh the list.");
    }
}
