package com.example.bloggingplatform.dao;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DaoExceptionTest {

    private static String message(String sqlState, int errorCode, String text) {
        return new DaoException("Could not save the post.", new SQLException(text, sqlState, errorCode)).getMessage();
    }

    @Test
    void commonMySqlErrorsGetPlainWords() {
        assertEquals("Could not save the post. A record with the same value already exists.",
                message("23000", 1062, "Duplicate entry 'x' for key 'posts.uq_posts_author_title'"));
        assertEquals("Could not save the post. A related record is missing or still in use.",
                message("23000", 1452, "Cannot add or update a child row"));
        assertEquals("Could not save the post. MySQL refused the login: check USER and PASSWORD in config/DBConnection.java.",
                message("28000", 1045, "Access denied"));
        assertEquals("Could not save the post. The database does not exist: run docs/sql/01_schema.sql first.",
                message("42000", 1049, "Unknown database"));
    }

    @Test
    void connectionProblemsAreRecognisedBySqlState() {
        assertEquals("Could not save the post. Cannot reach the MySQL server: check that it is running.",
                message("08S01", 0, "Communications link failure"));
    }

    @Test
    void otherErrorsKeepTheDriverMessageAndCause() {
        SQLException cause = new SQLException("Something odd", "HY000", 9999);
        DaoException e = new DaoException("Could not save the post.", cause);

        assertEquals("Could not save the post. (Something odd)", e.getMessage());
        assertSame(cause, e.getCause());
    }
}
