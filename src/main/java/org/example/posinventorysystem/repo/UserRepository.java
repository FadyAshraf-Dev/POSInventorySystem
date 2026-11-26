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
    public boolean deleteById(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return deleteById(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to delete user with id: " + id, e);
        }
    }

    public Optional<User> findById(Connection conn, Integer id) throws SQLException {
        String sql = BASE_SELECT + " WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public Optional<User> findByUsername(Connection conn, String username) throws SQLException {
        String sql = BASE_SELECT + " WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<User> findAll(Connection conn) throws SQLException {
        String sql = BASE_SELECT + " ORDER BY user_id";
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
                + "role_id = ?, is_active = ? WHERE user_id = ?";
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

    public boolean deleteById(Connection conn, Integer id) throws SQLException {
        String sql = "DELETE FROM user WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
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
}
