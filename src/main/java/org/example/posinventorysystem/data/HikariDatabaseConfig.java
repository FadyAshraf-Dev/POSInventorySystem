package org.example.posinventorysystem.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Pooled connection factory using HikariCP.
 * One pool (one HikariDataSource) is created ONCE per app run — this is the
 * "singleton manager" part. DAOs then borrow/return connections from it
 * instead of opening a fresh TCP connection every time.
 *
 * Maven dependency needed in pom.xml:
 * <dependency>
 *     <groupId>com.zaxxer</groupId>
 *     <artifactId>HikariCP</artifactId>
 *     <version>6.2.1</version>
 * </dependency>
 */
public class HikariDatabaseConfig {

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/posinventorysystem?useSSL=false&serverTimezone=UTC");
        config.setUsername("root");
        config.setPassword("asdasd"); // TODO: move to a config file / env var

        // Pool sizing — fine defaults for a desktop app with modest concurrency
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000); // ms to wait for a free connection before throwing
        config.setIdleTimeout(600_000);      // ms an idle connection can sit before being closed
        config.setMaxLifetime(1_800_000);    // ms before a connection is recycled, even if in use less often

        dataSource = new HikariDataSource(config);
    }

    private HikariDatabaseConfig() {
        // prevent instantiation — the pool itself lives in the static field above
    }

    /**
     * Borrows a connection from the pool.
     * IMPORTANT: callers must close() it when done — Hikari intercepts close()
     * to mean "return to pool," not "actually disconnect." Use try-with-resources.
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** Call once on app shutdown to cleanly close the pool. */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
