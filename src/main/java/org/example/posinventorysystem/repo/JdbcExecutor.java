package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Centralizes JDBC resource management and SQLException translation.
 *
 * Connections passed to methods in this class are caller-owned and are NEVER
 * closed here. Statements and result sets are always closed by this class.
 */
public final class JdbcExecutor {

    private JdbcExecutor() {}

    @FunctionalInterface
    public interface SqlWork<T> {
        T execute(Connection connection) throws SQLException;
    }

    public static <T> T withConnection(String operation, SqlWork<T> work) {
        try (Connection connection = HikariDatabaseConfig.getConnection()) {
            return work.execute(connection);
        } catch (SQLException e) {
            throw new RepositoryException(operation, e);
        }
    }

    public static <T> List<T> query(
            Connection connection,
            String sql,
            PreparedStatementSetter setter,
            RowMapper<T> mapper
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setter.setValues(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(mapper.mapRow(resultSet));
                }
                return results;
            }
        }
    }

    public static <T> Optional<T> queryOne(
            Connection connection,
            String sql,
            PreparedStatementSetter setter,
            RowMapper<T> mapper
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setter.setValues(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.ofNullable(mapper.mapRow(resultSet)) : Optional.empty();
            }
        }
    }

    public static int executeUpdate(
            Connection connection,
            String sql,
            PreparedStatementSetter setter
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setter.setValues(statement);
            return statement.executeUpdate();
        }
    }

    public static int executeInsert(
            Connection connection,
            String sql,
            PreparedStatementSetter setter
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setter.setValues(statement);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert succeeded but no generated key was returned.");
                }
                return keys.getInt(1);
            }
        }
    }

    public static boolean exists(
            Connection connection,
            String sql,
            PreparedStatementSetter setter
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setter.setValues(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
