package org.example.posinventorysystem.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Lazy connection-pool manager for the application.
 * The pool is created only when the first connection is requested, rather than
 * during class initialization. This means a database outage produces a useful
 * SQLException instead of poisoning class initialization for the entire JVM.
 */
public final class HikariDatabaseConfig {

    private static volatile HikariDataSource dataSource;

    private HikariDatabaseConfig() {
        // Utility class; the pool is managed statically.
    }

    private static HikariDataSource getDataSource() throws SQLException {
        HikariDataSource current = dataSource;
        if (current != null && !current.isClosed()) {
            return current;
        }

        synchronized (HikariDatabaseConfig.class) {
            current = dataSource;
            if (current != null && !current.isClosed()) {
                return current;
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://localhost:3306/posinventorysystem?useSSL=false&serverTimezone=UTC+2");
            config.setUsername("root");
            config.setPassword("asdasd"); // Credentials intentionally left as-is for this student project.

            // Pool sizing — sensible defaults for a desktop app with modest concurrency.
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30_000);
            config.setIdleTimeout(600_000);
            config.setMaxLifetime(1_800_000);

            try {
                dataSource = new HikariDataSource(config);
                return dataSource;
            } catch (RuntimeException e) {
                throw new SQLException(
                        "Could not initialize the MySQL connection pool. Make sure MySQL is running, "
                                + "the 'posinventorysystem' database exists, and the configured credentials are correct.",
                        e);
            }
        }
    }

    /**
     * Borrows a connection from the pool. Callers must close it (preferably via
     * try-with-resources); Hikari returns it to the pool rather than disconnecting.
     */
    public static Connection getConnection() throws SQLException {
        try {
            return getDataSource().getConnection();
        } catch (SQLException e) {
            throw new SQLException(
                    "Unable to obtain a MySQL connection for POSInventorySystem. "
                            + "Check that MySQL is running and that the database and credentials are valid.",
                    e);
        }
    }

    /** Closes the pool if it was initialized. Safe to call more than once. */
    public static void shutdown() {
        synchronized (HikariDatabaseConfig.class) {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
            dataSource = null;
        }
    }
}
