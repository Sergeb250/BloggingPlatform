package com.example.bloggingplatform.dao.impl;

import com.example.bloggingplatform.config.DBConnection;
import com.example.bloggingplatform.dao.DaoException;
import com.example.bloggingplatform.dao.PostDAO;
import com.example.bloggingplatform.model.Post;
import com.example.bloggingplatform.model.PostFilter;
import com.example.bloggingplatform.model.PostStatus;
import com.example.bloggingplatform.model.Tag;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** The MySQL version of {@link PostDAO}. Every value goes into a ? placeholder, so typed text can never change the SQL. */
public class PostDAOImpl implements PostDAO {

    /** Every column of a post, plus the author's name, the number of comments and the average rating. */
    private static final String SELECT_POSTS = """
            SELECT p.post_id, p.author_id, u.username AS author_name, p.title, p.content, p.status,
                   p.created_at, p.updated_at,
                   (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.post_id) AS comment_count,
                   (SELECT AVG(r.rating) FROM reviews r WHERE r.post_id = p.post_id) AS average_rating
            FROM posts p
            JOIN users u ON u.user_id = p.author_id
            """;

    @Override
    public List<Post> getAllPosts() {
        List<Post> posts = new ArrayList<>();
        String sql = SELECT_POSTS + " ORDER BY p.title";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                posts.add(readPost(rs));
            }
            for (Post post : posts) {
                post.setTags(getTagsOfPost(conn, post.getId()));
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load the posts.", e);
        }
        return posts;
    }

    /** The post, or null if there is none with this id. */
    @Override
    public Post getPostById(int id) {
        String sql = SELECT_POSTS + " WHERE p.post_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Post post = readPost(rs);
                    post.setTags(getTagsOfPost(conn, id));
                    return post;
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load the post.", e);
        }
        return null;
    }

    /** One page of the posts that match the filter, newest first. */
    @Override
    public List<Post> getPostsPage(PostFilter filter, int offset, int limit) {
        List<Object> values = new ArrayList<>();
        String sql = SELECT_POSTS + where(filter, values) + " ORDER BY p.created_at DESC, p.post_id DESC LIMIT ? OFFSET ?";
        values.add(limit);
        values.add(offset);

        List<Post> posts = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(readPost(rs));
                }
            }
            for (Post post : posts) {
                post.setTags(getTagsOfPost(conn, post.getId()));
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load the posts.", e);
        }
        return posts;
    }

    /** How many posts match the filter (for the page count). */
    @Override
    public long countPosts(PostFilter filter) {
        List<Object> values = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM posts p" + where(filter, values);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DaoException("Could not count the posts.", e);
        }
    }

    /** True if the author has another post with this title ({@code exceptPostId} is 0 for a new post). */
    @Override
    public boolean isTitleTaken(int authorId, String title, int exceptPostId) {
        String sql = "SELECT COUNT(*) FROM posts WHERE author_id = ? AND title = ? AND post_id <> ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, authorId);
            ps.setString(2, title);
            ps.setInt(3, exceptPostId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new DaoException("Could not check the title.", e);
        }
    }

    /** Saves a new post and its tag links together (one transaction) and returns the id MySQL gave it. */
    @Override
    public int addPost(Post post) {
        String sql = "INSERT INTO posts (author_id, title, content, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, post.getAuthorId());
                ps.setString(2, post.getTitle());
                ps.setString(3, post.getContent());
                ps.setString(4, post.getStatus().name());
                ps.executeUpdate();
                int id;
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getInt(1);
                }
                addTagLinks(conn, id, post.getTags());
                conn.commit();
                return id;
            } catch (SQLException e) {
                conn.rollback();   // nothing of the post is saved
                throw e;
            }
        } catch (SQLException e) {
            throw new DaoException("Could not save the post.", e);
        }
    }

    /** Updates the post and replaces its tag links (one transaction). Returns false if the post no longer exists. */
    @Override
    public boolean updatePost(Post post) {
        String sql = "UPDATE posts SET author_id = ?, title = ?, content = ?, status = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE post_id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 PreparedStatement deleteLinks = conn.prepareStatement("DELETE FROM post_tags WHERE post_id = ?")) {
                ps.setInt(1, post.getAuthorId());
                ps.setString(2, post.getTitle());
                ps.setString(3, post.getContent());
                ps.setString(4, post.getStatus().name());
                ps.setInt(5, post.getId());
                if (ps.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
                deleteLinks.setInt(1, post.getId());
                deleteLinks.executeUpdate();
                addTagLinks(conn, post.getId(), post.getTags());
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DaoException("Could not update the post.", e);
        }
    }

    /** Deletes the post; its comments, reviews and tag links go too (ON DELETE CASCADE). */
    @Override
    public boolean deletePost(int id) {
        String sql = "DELETE FROM posts WHERE post_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Could not delete the post.", e);
        }
    }

    /**
     * The WHERE part for a filter: " WHERE 1=1" plus one condition for every filter that is set.
     * The values are added to {@code values} in the same order as their ? marks.
     */
    private static String where(PostFilter filter, List<Object> values) {
        StringBuilder sql = new StringBuilder(" WHERE 1=1");
        if (filter.getStatus() != null) {
            sql.append(" AND p.status = ?");
            values.add(filter.getStatus().name());
        }
        if (filter.getAuthorId() != null) {
            sql.append(" AND p.author_id = ?");
            values.add(filter.getAuthorId());
        }
        if (filter.getTagId() != null) {
            sql.append(" AND p.post_id IN (SELECT post_id FROM post_tags WHERE tag_id = ?)");
            values.add(filter.getTagId());
        }
        if (filter.getKeyword() != null) {
            // MySQL's default collation ignores upper/lower case, so "java" also finds "Java"
            sql.append(" AND (p.title LIKE ? OR p.content LIKE ?)");
            String pattern = "%" + escapeLike(filter.getKeyword()) + "%";
            values.add(pattern);
            values.add(pattern);
        }
        return sql.toString();
    }

    /** Makes % and _ typed by the user match literally instead of acting as LIKE wildcards. */
    static String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** Turns the current row into a Post object (without its tags). */
    private Post readPost(ResultSet rs) throws SQLException {
        Post post = new Post();
        post.setId(rs.getInt("post_id"));
        post.setAuthorId(rs.getInt("author_id"));
        post.setAuthorName(rs.getString("author_name"));
        post.setTitle(rs.getString("title"));
        post.setContent(rs.getString("content"));
        post.setStatus(PostStatus.valueOf(rs.getString("status")));
        post.setCommentCount(rs.getInt("comment_count"));
        BigDecimal average = rs.getBigDecimal("average_rating");   // null when there are no reviews
        post.setAverageRating(average == null ? null : average.doubleValue());
        post.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        post.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return post;
    }

    /** The tags of one post, read on the connection that is already open. */
    private List<Tag> getTagsOfPost(Connection conn, int postId) throws SQLException {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT t.tag_id, t.name FROM post_tags pt JOIN tags t ON t.tag_id = pt.tag_id "
                + "WHERE pt.post_id = ? ORDER BY t.name";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Tag tag = new Tag();
                    tag.setId(rs.getInt("tag_id"));
                    tag.setName(rs.getString("name"));
                    tags.add(tag);
                }
            }
        }
        return tags;
    }

    private void addTagLinks(Connection conn, int postId, List<Tag> tags) throws SQLException {
        String sql = "INSERT INTO post_tags (post_id, tag_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Tag tag : tags) {
                ps.setInt(1, postId);
                ps.setInt(2, tag.getId());
                ps.executeUpdate();
            }
        }
    }
}
