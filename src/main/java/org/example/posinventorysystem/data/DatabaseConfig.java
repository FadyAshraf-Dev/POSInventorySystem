package org.example.posinventorysystem.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Simple, no-pooling connection factory.
 * Every call to getConnection() opens a brand new TCP connection to MySQL.
 * Fine for early development / single-threaded testing.
 * Replace with HikariDatabaseConfig once you move to the pooled version below.
 */
public class DatabaseConfig {

    private static final String HOST = "localhost";
    private static final int PORT = 3306;
    private static final String DB_NAME = "posinventorysystem";
    private static final String USER = "root";
    private static final String PASSWORD = "asdasd"; // TODO: move to a config file / env var, don't hardcode in real builds

    private static final String URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME
            + "?useSSL=false&serverTimezone=UTC";

    private DatabaseConfig() {
        // prevent instantiation — this is a static utility class
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
