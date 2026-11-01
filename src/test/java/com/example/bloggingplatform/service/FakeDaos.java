package com.example.bloggingplatform.service;

import com.example.bloggingplatform.dao.CommentDAO;
import com.example.bloggingplatform.dao.DaoException;
import com.example.bloggingplatform.dao.PostDAO;
import com.example.bloggingplatform.dao.ReviewDAO;
import com.example.bloggingplatform.dao.TagDAO;
import com.example.bloggingplatform.dao.UserDAO;
import com.example.bloggingplatform.model.Comment;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostFilter;
import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Review;
import com.example.bloggingplatform.model.Role;
import com.example.bloggingplatform.model.Tag;
import com.example.bloggingplatform.model.User;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory implementations of the DAO interfaces, so the service rules can be unit-tested without a database. */
final class FakeDaos {

    private FakeDaos() {
    }

    /** A session signed in as an admin that is not stored in any fake DAO, so it never shows up in lists. */
    static Session adminSession() {
        return sessionOf(new User(999, "root", "root@example.com", "hash", Role.ADMIN, null));
    }

    static Session sessionOf(User user) {
        Session session = new Session();
        session.signIn(user);
        return session;
    }

    static final class Users implements UserDAO {
        final Map<Integer, User> rows = new LinkedHashMap<>();
        private int nextId = 1;

        User add(String username, Role role) {
            int id = addUser(new User(0, username, username + "@example.com", "hash", role, null));
            return rows.get(id);
        }

        @Override
        public List<User> getAllUsers() {
            return new ArrayList<>(rows.values());
        }

        @Override
        public User getUserByLogin(String login) {
            return rows.values().stream()
                    .filter(user -> user.getUsername().equalsIgnoreCase(login) || user.getEmail().equalsIgnoreCase(login))
                    .findFirst().orElse(null);
        }

        @Override
        public User getUserByUsername(String username) {
            return rows.values().stream().filter(user -> user.getUsername().equalsIgnoreCase(username)).findFirst().orElse(null);
        }

        @Override
        public User getUserByEmail(String email) {
            return rows.values().stream().filter(user -> user.getEmail().equalsIgnoreCase(email)).findFirst().orElse(null);
        }

        @Override
        public long countUsersByRole(Role role) {
            return rows.values().stream().filter(user -> user.getRole() == role).count();
        }

        @Override
        public User getUserById(int id) {
            return rows.get(id);
        }

        @Override
        public int addUser(User user) {
            int id = nextId++;
            rows.put(id, new User(id, user.getUsername(), user.getEmail(), user.getPasswordHash(), user.getRole(), LocalDateTime.now()));
            return id;
        }

        @Override
        public boolean updateUser(User user) {
            return rows.replace(user.getId(), user) != null;
        }

        @Override
        public boolean deleteUser(int id) {
            return rows.remove(id) != null;
        }
    }

    static final class Posts implements PostDAO {
        final Map<Integer, Post> rows = new LinkedHashMap<>();
        int lastOffset = -1;
        int getAllPostsCalls;   // how often the cache loaded from the "database"
        boolean databaseDown;   // true: getAllPosts fails like MySQL being unreachable
        private int nextId = 1;

        Post add(User author, String title, PostStatus status) {
            int id = addPost(new Post(0, author.getId(), title, "Content of the " + title + " fixture.", status, List.of()));
            return rows.get(id);
        }

        /** Ignores the filter: the service tests only check paging arithmetic and rules. */
        @Override
        public List<Post> getPostsPage(PostFilter filter, int offset, int limit) {
            lastOffset = offset;
            return rows.values().stream().skip(offset).limit(limit).toList();
        }

        @Override
        public long countPosts(PostFilter filter) {
            return rows.size();
        }

        @Override
        public List<Post> getAllPosts() {
            getAllPostsCalls++;
            if (databaseDown) {
                throw new DaoException("Could not load the posts.", new SQLException("Connection refused", "08S01"));
            }
            return new ArrayList<>(rows.values());
        }

        @Override
        public boolean isTitleTaken(int authorId, String title, int exceptPostId) {
            return rows.values().stream().anyMatch(post -> post.getAuthorId() == authorId
                    && post.getTitle().equalsIgnoreCase(title) && post.getId() != exceptPostId);
        }

        @Override
        public Post getPostById(int id) {
            return rows.get(id);
        }

        @Override
        public int addPost(Post post) {
            int id = nextId++;
            LocalDateTime now = LocalDateTime.now();
            rows.put(id, new Post(id, post.getAuthorId(), null, post.getTitle(), post.getContent(), post.getStatus(),
                    post.getTags(), 0, null, now, now));
            return id;
        }

        /** Like the UPDATE statement: the edited values change, created_at and the counts stay. */
        @Override
        public boolean updatePost(Post post) {
            Post existing = rows.get(post.getId());
            if (existing == null) {
                return false;
            }
            rows.put(post.getId(), new Post(post.getId(), post.getAuthorId(), existing.getAuthorName(), post.getTitle(),
                    post.getContent(), post.getStatus(), post.getTags(), existing.getCommentCount(),
                    existing.getAverageRating(), existing.getCreatedAt(), LocalDateTime.now()));
            return true;
        }

        @Override
        public boolean deletePost(int id) {
            return rows.remove(id) != null;
        }
    }

    static final class Comments implements CommentDAO {
        final Map<Integer, Comment> rows = new LinkedHashMap<>();
        private int nextId = 1;

        @Override
        public List<Comment> getAllComments() {
            return new ArrayList<>(rows.values());
        }

        @Override
        public List<Comment> getCommentsByPost(int postId) {
            return rows.values().stream().filter(comment -> comment.getPostId() == postId).toList();
        }

        @Override
        public List<Comment> getCommentsByUser(int userId) {
            return rows.values().stream().filter(comment -> comment.getUserId() == userId).toList();
        }

        /** Not used by the service tests. */
        @Override
        public List<Comment> getCommentsOnPostsOf(int authorId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Comment getCommentById(int id) {
            return rows.get(id);
        }

        @Override
        public int addComment(Comment comment) {
            int id = nextId++;
            rows.put(id, new Comment(id, comment.getPostId(), comment.getUserId(), comment.getContent()));
            return id;
        }

        @Override
        public boolean updateComment(Comment comment) {
            return rows.replace(comment.getId(), comment) != null;
        }

        @Override
        public boolean deleteComment(int id) {
            return rows.remove(id) != null;
        }
    }

    static final class Reviews implements ReviewDAO {
        final Map<Integer, Review> rows = new LinkedHashMap<>();
        private int nextId = 1;

        @Override
        public List<Review> getAllReviews() {
            return new ArrayList<>(rows.values());
        }

        @Override
        public List<Review> getReviewsByPost(int postId) {
            return rows.values().stream().filter(review -> review.getPostId() == postId).toList();
        }

        @Override
        public List<Review> getReviewsByUser(int userId) {
            return rows.values().stream().filter(review -> review.getUserId() == userId).toList();
        }

        /** Not used by the service tests. */
        @Override
        public List<Review> getReviewsOnPostsOf(int authorId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Review getReviewById(int id) {
            return rows.get(id);
        }

        @Override
        public int addReview(Review review) {
            int id = nextId++;
            rows.put(id, new Review(id, review.getPostId(), review.getUserId(), review.getRating(), review.getReviewText()));
            return id;
        }

        @Override
        public boolean updateReview(Review review) {
            return rows.replace(review.getId(), review) != null;
        }

        @Override
        public boolean deleteReview(int id) {
            return rows.remove(id) != null;
        }
    }

    static final class Tags implements TagDAO {
        final Map<Integer, Tag> rows = new LinkedHashMap<>();
        private final Posts posts;
        private int nextId = 1;

        Tags() {
            this(null);
        }

        /** {@code posts} is needed only by {@link #findUsedBy}. */
        Tags(Posts posts) {
            this.posts = posts;
        }

        @Override
        public List<Tag> getAllTags() {
            return new ArrayList<>(rows.values());
        }

        @Override
        public Tag getTagByName(String name) {
            return rows.values().stream().filter(tag -> tag.getName().equals(name)).findFirst().orElse(null);
        }

        @Override
        public List<Tag> getTagsUsedBy(int authorId) {
            return posts.rows.values().stream()
                    .filter(post -> post.getAuthorId() == authorId)
                    .flatMap(post -> post.getTags().stream())
                    .distinct()
                    .toList();
        }

        @Override
        public Tag getTagById(int id) {
            return rows.get(id);
        }

        @Override
        public int addTag(Tag tag) {
            int id = nextId++;
            rows.put(id, new Tag(id, tag.getName()));
            return id;
        }

        @Override
        public boolean updateTag(Tag tag) {
            return rows.replace(tag.getId(), tag) != null;
        }

        @Override
        public boolean deleteTag(int id) {
            return rows.remove(id) != null;
        }
    }
}
