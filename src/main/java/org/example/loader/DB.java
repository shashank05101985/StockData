package org.example.loader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {

    private static final String URL =
            "jdbc:postgresql://localhost:5432/stockdb";
    private static final String USER = "mayank";
    private static final String PASS = "password";

    private static Connection conn;

    public static Connection get() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(URL, USER, PASS);
            conn.setAutoCommit(false); // 🔥 IMPORTANT
        }
        return conn;
    }
}
