package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository implements GenericRepository<User, Integer> {

    private static final String BASE_SELECT =
            "SELECT user_id, full_name, username, password_hash, role_id, is_active FROM user";

    @Override
    public Optional<User> findById(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find user by id: " + id, e);
        }
    }

    public Optional<User> findByUsername(String username) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findByUsername(conn, username);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find user by username: " + username, e);
        }
    }

    @Override
    public List<User> findAll() {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all users", e);
        }
    }

    @Override
    public Integer insert(User user) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return insert(conn, user);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert user: " + user, e);
        }
    }

    @Override
    public boolean update(User user) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return update(conn, user);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update user: " + user, e);
        }
    }

    @Override
    public boolean softDelete(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return softDelete(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to soft-delete user with id: " + id, e);
        }
    }

    @Override
    public boolean restore(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return restore(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to restore user with id: " + id, e);
        }
    }

    public Optional<User> findById(Connection conn, Integer id) throws SQLException {
        String sql = BASE_SELECT + " WHERE user_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public Optional<User> findByUsername(Connection conn, String username) throws SQLException {
        String sql = BASE_SELECT + " WHERE username = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<User> findAll(Connection conn) throws SQLException {
        String sql = BASE_SELECT + " WHERE deleted_at IS NULL ORDER BY user_id";
        List<User> users = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        return users;
    }

    public Integer insert(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO user (full_name, username, password_hash, role_id, is_active) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPasswordHash());
            ps.setInt(4, user.getRoleId());
            ps.setBoolean(5, user.isActive());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    user.setUserId(generatedId);
                    return generatedId;
                }
                throw new SQLException("Insert succeeded but no generated key was returned for user");
            }
        }
    }

    /**
     * NOTE (review item 9): this full update() still rewrites password_hash every
     * call. A User object loaded without the hash populated would wipe it.
     * A dedicated updatePassword(conn, userId, hash) should be added separately
     * and this update() should stop touching password_hash — left as-is here
     * since that's a distinct review item; flagging so it isn't forgotten.
     */
    public boolean update(Connection conn, User user) throws SQLException {
        String sql = "UPDATE user SET full_name = ?, username = ?, password_hash = ?, "
                + "role_id = ?, is_active = ? WHERE user_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPasswordHash());
            ps.setInt(4, user.getRoleId());
            ps.setBoolean(5, user.isActive());
            ps.setInt(6, user.getUserId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE user SET deleted_at = CURRENT_TIMESTAMP(6) WHERE user_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); return ps.executeUpdate() > 0; }
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE user SET deleted_at = NULL WHERE user_id = ? AND deleted_at IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); return ps.executeUpdate() > 0; }
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
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return usernameExists(conn, username, excludeId);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to check username uniqueness", e);
        }
    }

    public boolean usernameExists(Connection conn, String username, Integer excludeId) throws SQLException {
        if (username == null || username.isBlank()) return false;
        String sql = excludeId == null
                ? "SELECT 1 FROM user WHERE username = ? LIMIT 1"
                : "SELECT 1 FROM user WHERE username = ? AND user_id <> ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            if (excludeId != null) ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean setActive(Integer userId, boolean active) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return setActive(conn, userId, active);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to change active state for user id: " + userId, e);
        }
    }

    public boolean setActive(Connection conn, Integer userId, boolean active) throws SQLException {
        String sql = "UPDATE user SET is_active = ? WHERE user_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

}
