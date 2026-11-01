package com.example.bloggingplatform.dao.impl;

import com.example.bloggingplatform.config.DBConnection;
import com.example.bloggingplatform.dao.DaoException;
import com.example.bloggingplatform.dao.TagDAO;
import com.example.bloggingplatform.model.Tag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** The MySQL version of {@link TagDAO}. Every value goes into a ? placeholder, so typed text can never change the SQL. */
public class TagDAOImpl implements TagDAO {

    @Override
    public List<Tag> getAllTags() {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT * FROM tags ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tags.add(readTag(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load the tags.", e);
        }
        return tags;
    }

    /** The tag, or null if there is none with this id. */
    @Override
    public Tag getTagById(int id) {
        String sql = "SELECT * FROM tags WHERE tag_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return readTag(rs);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load the tag.", e);
        }
        return null;
    }

    /** The tag with this name, or null. */
    @Override
    public Tag getTagByName(String name) {
        String sql = "SELECT * FROM tags WHERE name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return readTag(rs);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load the tag.", e);
        }
        return null;
    }

    /** The tags on the author's own posts (a tag has no owner column, post_tags → posts.author_id tells). */
    @Override
    public List<Tag> getTagsUsedBy(int authorId) {
        List<Tag> tags = new ArrayList<>();
        String sql = """
                SELECT DISTINCT t.tag_id, t.name
                FROM tags t
                JOIN post_tags pt ON pt.tag_id = t.tag_id
                JOIN posts p ON p.post_id = pt.post_id
                WHERE p.author_id = ?
                ORDER BY t.name
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, authorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tags.add(readTag(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load your tags.", e);
        }
        return tags;
    }

    /** Saves a new tag and returns the id MySQL gave it. */
    @Override
    public int addTag(Tag tag) {
        String sql = "INSERT INTO tags (name) VALUES (?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tag.getName());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DaoException("Could not save the tag.", e);
        }
    }

    /** Renames the tag; returns false if it no longer exists. */
    @Override
    public boolean updateTag(Tag tag) {
        String sql = "UPDATE tags SET name = ? WHERE tag_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tag.getName());
            ps.setInt(2, tag.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Could not rename the tag.", e);
        }
    }

    /** Deletes the tag; its links to posts go too (ON DELETE CASCADE), the posts stay. */
    @Override
    public boolean deleteTag(int id) {
        String sql = "DELETE FROM tags WHERE tag_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Could not delete the tag.", e);
        }
    }

    /** Turns the current row into a Tag object. */
    private Tag readTag(ResultSet rs) throws SQLException {
        Tag tag = new Tag();
        tag.setId(rs.getInt("tag_id"));
        tag.setName(rs.getString("name"));
        return tag;
    }
}
