package com.example.bloggingplatform.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DBConnection {

    public static final String URL = "jdbc:mysql://localhost:3306/bloggingPlatform";
    public static final String USER = "root";
    public static final String PASSWORD = "change-me";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
