package com.example.bloggingplatform.dao;

import java.sql.SQLException;

/**
 * Thrown by the DAOs when a database operation fails. It wraps the {@link SQLException} and adds
 * a message a user can act on, so the screens never show raw SQL errors.
 */
public class DaoException extends RuntimeException {

    /** @param action what was being done, e.g. "Could not save the post." */
    public DaoException(String action, SQLException cause) {
        super(action + " " + reason(cause), cause);
    }

    /** The most common MySQL problems in plain words (vendor error code, or SQL state "08" for connection errors). */
    private static String reason(SQLException e) {
        return switch (e.getErrorCode()) {
            case 1045 -> "MySQL refused the login: check USER and PASSWORD in config/DBConnection.java.";
            case 1049 -> "The database does not exist: run docs/sql/01_schema.sql first.";
            case 1062 -> "A record with the same value already exists.";
            case 1451, 1452 -> "A related record is missing or still in use.";
            default -> e.getSQLState() != null && e.getSQLState().startsWith("08")
                    ? "Cannot reach the MySQL server: check that it is running."
                    : "(" + e.getMessage() + ")";
        };
    }
}
