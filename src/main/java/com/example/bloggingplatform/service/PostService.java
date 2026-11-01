package com.example.bloggingplatform.service;

import com.example.bloggingplatform.algorithms.MergeSort;
import com.example.bloggingplatform.cache.PostCache;
import com.example.bloggingplatform.dao.PostDAO;
import com.example.bloggingplatform.dao.UserDAO;
import com.example.bloggingplatform.model.Page;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostFilter;
import com.example.bloggingplatform.model.PostSort;
import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.Tag;
import com.example.bloggingplatform.model.User;
import com.example.bloggingplatform.validations.PostValidator;
import com.example.bloggingplatform.validations.TagValidator;
import com.example.bloggingplatform.validations.Validation;
import com.example.bloggingplatform.validations.ValidationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Posts: paged listing with filters, reading, and create / edit / publish / delete with validation.
 * <p>
 * Everybody reads published posts; drafts are visible only to their author and admins. Authors
 * write, edit, publish and delete their own posts; admins may do so for any post. Readers cannot
 * write posts. Published posts are read from the {@link PostCache}; every change empties it.
 */
public class PostService {

    private final PostDAO posts;
    private final TagService tags;
    private final UserDAO users;
    private final Session session;
    private final PostCache cache;

    public PostService(PostDAO posts, TagService tags, UserDAO users, Session session, PostCache cache) {
        this.posts = posts;
        this.tags = tags;
        this.users = users;
        this.session = session;
        this.cache = cache;
    }

    /**
     * One page of the posts the signed-in user may see that match the filter, newest first, read
     * from MySQL (the post tables, which include drafts). A page number past the end (e.g. after
     * deleting the last post of the last page) moves back to the last page.
     */
    public Page<Post> findPage(PostFilter filter, int pageIndex, int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
        PostFilter visible = visible(filter);
        long total = posts.countPosts(visible);
        int page = clampPage(pageIndex, total, pageSize);
        List<Post> items = new ArrayList<>();
        if (total > 0) {
            items = posts.getPostsPage(visible, page * pageSize, pageSize);
        }
        return new Page<>(items, page, pageSize, total);
    }

    /**
     * One page of Discover: the published posts from the cache that match the filter's author, tag
     * and search text, sorted with merge sort. The search ignores case and finds words typed partly.
     */
    public Page<Post> discover(PostFilter filter, PostSort sort, int pageIndex, int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
        Set<Integer> found = null;   // null: no search text, every post matches
        if (filter.getKeyword() != null) {
            found = cache.search(filter.getKeyword());
        }
        List<Post> matches = new ArrayList<>();
        for (Post post : cache.getPublished()) {
            boolean wanted = (found == null || found.contains(post.getId()))
                    && (filter.getAuthorId() == null || filter.getAuthorId() == post.getAuthorId())
                    && (filter.getTagId() == null || hasTag(post, filter.getTagId()));
            if (wanted) {
                matches.add(post);
            }
        }
        MergeSort.sort(matches, sort.comparator());

        int page = clampPage(pageIndex, matches.size(), pageSize);
        int from = page * pageSize;
        int to = Math.min(from + pageSize, matches.size());
        return new Page<>(matches.subList(from, to), page, pageSize, matches.size());
    }

    /** Every post the signed-in user may see, ordered by title (for drop-down lists). */
    public List<Post> findAll() {
        List<Post> visible = new ArrayList<>();
        for (Post post : posts.getAllPosts()) {
            if (canSee(post)) {
                visible.add(post);
            }
        }
        return visible;
    }

    public List<Post> findPublished() {
        List<Post> published = new ArrayList<>();
        for (Post post : posts.getAllPosts()) {
            if (post.getStatus() == PostStatus.PUBLISHED) {
                published.add(post);
            }
        }
        return published;
    }

    /**
     * The post, or null if it does not exist or the signed-in user may not see it. Published posts
     * come from the cache (a hash map lookup); drafts come from the database.
     */
    public Post findById(int id) {
        Post post = cache.getPublished(id);
        if (post == null) {
            post = posts.getPostById(id);
        }
        if (post == null || !canSee(post)) {
            return null;
        }
        return post;
    }

    /**
     * Creates a post. Authors always write as themselves; only an admin may pick another author.
     *
     * @param tagNames tags as typed, e.g. "Spring Boot"; tags that do not exist yet are created
     */
    public Post create(Integer authorId, String title, String content, PostStatus status, Collection<String> tagNames) {
        User me = session.require();
        if (!me.getRole().canWritePosts()) {
            throw new AccessDeniedException("Only authors and admins can write posts. Readers can comment and rate.");
        }
        Integer author = authorId;
        if (me.getRole() != Role.ADMIN) {
            author = me.getId();
        }
        List<String> tagNamesToSave = validate(0, author, title, content, status, tagNames);

        Post post = new Post(0, author, title.strip(), content.strip(), status, tags.findOrCreate(tagNamesToSave));
        int id = posts.addPost(post);
        cache.clear();
        return posts.getPostById(id);
    }

    /** Edits a post. Authors may edit only their own posts and cannot hand them to another author. */
    public Post update(int id, Integer authorId, String title, String content, PostStatus status,
                       Collection<String> tagNames) {
        Post existing = posts.getPostById(id);
        if (existing == null) {
            throw notFound();
        }
        User me = session.requireOwnerOrAdmin(existing.getAuthorId(), "You can only edit your own posts.");
        Integer author = authorId;
        if (me.getRole() != Role.ADMIN) {
            author = existing.getAuthorId();
        }
        List<String> tagNamesToSave = validate(id, author, title, content, status, tagNames);

        Post post = new Post(id, author, title.strip(), content.strip(), status, tags.findOrCreate(tagNamesToSave));
        if (!posts.updatePost(post)) {
            throw notFound();
        }
        cache.clear();
        return posts.getPostById(id);
    }

    /** Publishes a draft or turns a published post back into a draft. */
    public Post changeStatus(int id, PostStatus status) {
        Post existing = posts.getPostById(id);
        if (existing == null) {
            throw notFound();
        }
        List<String> tagNames = new ArrayList<>();
        for (Tag tag : existing.getTags()) {
            tagNames.add(tag.getName());
        }
        return update(id, existing.getAuthorId(), existing.getTitle(), existing.getContent(), status, tagNames);
    }

    /** Deletes the post with its comments, reviews and tag links (ON DELETE CASCADE). */
    public void delete(int id) {
        Post existing = posts.getPostById(id);
        if (existing == null) {
            throw notFound();
        }
        session.requireOwnerOrAdmin(existing.getAuthorId(), "You can only delete your own posts.");
        if (!posts.deletePost(id)) {
            throw notFound();
        }
        cache.clear();
    }

    /** True if the signed-in user may edit, publish or delete the post. */
    public boolean canManage(Post post) {
        return session.isAdmin() || session.is(post.getAuthorId());
    }

    /**
     * The filter limited to what the signed-in user may see: admins see everything, an author
     * listing their own posts sees their drafts too, everybody else sees published posts only.
     */
    PostFilter visible(PostFilter filter) {
        Integer authorId = filter.getAuthorId();
        if (session.isAdmin() || (authorId != null && session.is(authorId))) {
            return filter;
        }
        return new PostFilter(PostStatus.PUBLISHED, authorId, filter.getTagId(), filter.getKeyword());
    }

    private boolean canSee(Post post) {
        return post.getStatus() == PostStatus.PUBLISHED || canManage(post);
    }

    private static boolean hasTag(Post post, int tagId) {
        for (Tag tag : post.getTags()) {
            if (tag.getId() == tagId) {
                return true;
            }
        }
        return false;
    }

    /** The page to show: {@code pageIndex}, but never before the first or after the last page. */
    private static int clampPage(int pageIndex, long total, int pageSize) {
        int lastPage = (int) Math.max(0, (total - 1) / pageSize);
        return Math.clamp(pageIndex, 0, lastPage);
    }

    /**
     * Checks every field and throws one {@link ValidationException} listing all problems. Returns
     * the tag names as slugs, without duplicates.
     *
     * @param postId the post being edited, 0 for a new post
     */
    private List<String> validate(int postId, Integer authorId, String title, String content, PostStatus status,
                                  Collection<String> tagNames) {
        Validation v = new Validation();
        v.required("author", "Author", authorId);
        String cleanTitle = v.text("title", title, PostValidator::title);
        v.text("content", content, PostValidator::content);
        v.required("status", "Status", status);
        if (authorId != null) {
            User author = users.getUserById(authorId);
            if (author == null) {
                v.reject("author", "The selected author no longer exists.");
            } else if (!author.getRole().canWritePosts()) {
                v.reject("author", "Only authors and admins can write posts; "
                        + author.getUsername() + " is a " + author.getRole() + ".");
            } else if (!v.hasError("title") && posts.isTitleTaken(authorId, cleanTitle, postId)) {
                v.reject("title", "This author already has a post with that title.");
            }
        }

        List<String> slugs = new ArrayList<>();
        if (tagNames != null) {
            for (String name : tagNames) {
                String slug = TagValidator.slug(name);   // "Spring Boot" → "spring-boot"
                if (!slug.isEmpty() && !slugs.contains(slug)) {
                    slugs.add(slug);
                    v.rule("tags", TagValidator.postTag(slug));
                }
            }
        }
        v.check(slugs.size() <= TagValidator.MAX_TAGS_PER_POST, "tags", "A post can have at most " + TagValidator.MAX_TAGS_PER_POST + " tags.");
        v.throwIfInvalid();
        return slugs;
    }

    private static ValidationException notFound() {
        return ValidationException.of("This post no longer exists. Refresh the list.");
    }
}
