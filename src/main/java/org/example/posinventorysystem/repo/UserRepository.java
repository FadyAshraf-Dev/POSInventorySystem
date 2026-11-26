package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.User;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class UserRepository implements GenericRepository<User, Integer> {

    private static final String BASE_SELECT =
            "SELECT user_id, full_name, username, password_hash, role_id, is_active FROM user";

    @Override
    public Optional<User> findById(Integer id) {
        return JdbcExecutor.withConnection("Failed to find user by id: " + id, conn -> findById(conn, id));
    }

    public Optional<User> findByUsername(String username) {
        return JdbcExecutor.withConnection("Failed to find user by username: " + username, conn -> findByUsername(conn, username));
    }

    @Override
    public List<User> findAll() {
        return JdbcExecutor.withConnection("Failed to fetch all users", this::findAll);
    }

    @Override
    public Integer insert(User user) {
        return JdbcExecutor.withConnection("Failed to insert user: " + user, conn -> insert(conn, user));
    }

    @Override
    public boolean update(User user) {
        return JdbcExecutor.withConnection("Failed to update user: " + user, conn -> update(conn, user));
    }

    @Override
    public boolean softDelete(Integer id) {
        return JdbcExecutor.withConnection("Failed to soft-delete user with id: " + id, conn -> softDelete(conn, id));
    }

    @Override
    public boolean restore(Integer id) {
        return JdbcExecutor.withConnection("Failed to restore user with id: " + id, conn -> restore(conn, id));
    }

    public Optional<User> findById(Connection conn, Integer id) throws SQLException {
        String sql = BASE_SELECT + " WHERE user_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.queryOne(conn, sql, ps -> { ps.setInt(1, id); }, this::mapRow);
    }

    public Optional<User> findByUsername(Connection conn, String username) throws SQLException {
        String sql = BASE_SELECT + " WHERE username = ? AND deleted_at IS NULL";
        return JdbcExecutor.queryOne(conn, sql, ps -> { ps.setString(1, username); }, this::mapRow);
    }

    public List<User> findAll(Connection conn) throws SQLException {
        String sql = BASE_SELECT + " WHERE deleted_at IS NULL ORDER BY user_id";
        return JdbcExecutor.query(conn, sql, ps -> { }, this::mapRow);
    }

    public Integer insert(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO user (full_name, username, password_hash, role_id, is_active) "
                + "VALUES (?, ?, ?, ?, ?)";
        int generatedId = JdbcExecutor.executeInsert(conn, sql, ps -> { ps.setString(1, user.getFullName());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPasswordHash());
            ps.setInt(4, user.getRoleId());
            ps.setBoolean(5, user.isActive()); });
        user.setUserId(generatedId);
        return generatedId;
    }

    /**
     * Updates a user's profile and role without touching their password hash.
     * Password changes must go through updatePassword() explicitly.
     */
    public boolean update(Connection conn, User user) throws SQLException {
        String sql = "UPDATE user SET full_name = ?, username = ?, role_id = ?, is_active = ? "
                + "WHERE user_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setString(1, user.getFullName());
            ps.setString(2, user.getUsername());
            ps.setInt(3, user.getRoleId());
            ps.setBoolean(4, user.isActive());
            ps.setInt(5, user.getUserId()); }) > 0;
    }

    /** Updates only the password hash for a non-deleted user. */
    public boolean updatePassword(Integer userId, String passwordHash) {
        return JdbcExecutor.withConnection("Failed to update password for user id: " + userId, conn -> updatePassword(conn, userId, passwordHash));
    }

    /** Connection-aware overload for use inside a business-layer transaction. */
    public boolean updatePassword(Connection conn, Integer userId, String passwordHash) throws SQLException {
        String sql = "UPDATE user SET password_hash = ? WHERE user_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setString(1, passwordHash);
            ps.setInt(2, userId); }) > 0;
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE user SET deleted_at = CURRENT_TIMESTAMP(6) WHERE user_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setInt(1, id); }) > 0;
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE user SET deleted_at = NULL WHERE user_id = ? AND deleted_at IS NOT NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setInt(1, id); }) > 0;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setFullName(rs.getString("full_name"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRoleId(rs.getInt("role_id"));
        user.setActive(rs.getBoolean("is_active"));
        return user;
    }

    /** Checks username uniqueness across all records, including soft-deleted rows, to match the UNIQUE constraint. */
    public boolean usernameExists(String username, Integer excludeId) {
        return JdbcExecutor.withConnection("Failed to check username uniqueness", conn -> usernameExists(conn, username, excludeId));
    }

    public boolean usernameExists(Connection conn, String username, Integer excludeId) throws SQLException {
        if (username == null || username.isBlank()) return false;
        String sql = excludeId == null
                ? "SELECT 1 FROM user WHERE username = ? LIMIT 1"
                : "SELECT 1 FROM user WHERE username = ? AND user_id <> ? LIMIT 1";
        return JdbcExecutor.exists(conn, sql, ps -> { ps.setString(1, username.trim());
            if (excludeId != null) ps.setInt(2, excludeId); });
    }

    public boolean setActive(Integer userId, boolean active) {
        return JdbcExecutor.withConnection("Failed to change active state for user id: " + userId, conn -> setActive(conn, userId, active));
    }

    public boolean setActive(Connection conn, Integer userId, boolean active) throws SQLException {
        String sql = "UPDATE user SET is_active = ? WHERE user_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setBoolean(1, active);
            ps.setInt(2, userId); }) > 0;
    }

}
