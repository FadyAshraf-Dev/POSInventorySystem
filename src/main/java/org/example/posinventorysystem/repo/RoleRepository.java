package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Role;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoleRepository implements GenericRepository<Role, Integer> {

    // ------------------------------------------------------------
    // Public API (GenericRepository) — opens its own connection,
    // auto-commits, and delegates to the (Connection, ...) overload.
    // Safe to call directly for single-statement operations that
    // don't need to be part of a larger transaction.
    // ------------------------------------------------------------

    @Override
    public Optional<Role> findById(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find role by id: " + id, e);
        }
    }

    @Override
    public List<Role> findAll() {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all roles", e);
        }
    }

    @Override
    public Integer insert(Role role) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return insert(conn, role);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert role: " + role, e);
        }
    }

    @Override
    public boolean update(Role role) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return update(conn, role);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update role: " + role, e);
        }
    }

    @Override
    public boolean softDelete(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return softDelete(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to soft-delete role with id: " + id, e);
        }
    }

    @Override
    public boolean restore(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return restore(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to restore role with id: " + id, e);
        }
    }

    // ------------------------------------------------------------
    // Connection-accepting overloads — the real implementations.
    // Call these directly (passing a shared Connection) from inside
    // TransactionManager.run(...) so multiple repo calls share one
    // transaction. These throw checked SQLException on purpose —
    // TransactionManager catches it and rolls back.
    // ------------------------------------------------------------

    public Optional<Role> findById(Connection conn, Integer id) throws SQLException {
        String sql = "SELECT role_id, role_name FROM role WHERE deleted_at IS NULL AND role_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Role> findAll(Connection conn) throws SQLException {
        String sql = "SELECT role_id, role_name FROM role WHERE deleted_at IS NULL ORDER BY role_id";
        List<Role> roles = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                roles.add(mapRow(rs));
            }
        }
        return roles;
    }

    public Integer insert(Connection conn, Role role) throws SQLException {
        String sql = "INSERT INTO role (role_name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, role.getRoleName());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    role.setRoleId(generatedId);
                    return generatedId;
                }
                throw new SQLException("Insert succeeded but no generated key was returned for role");
            }
        }
    }

    public boolean update(Connection conn, Role role) throws SQLException {
        String sql = "UPDATE role SET role_name = ? WHERE role_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.getRoleName());
            ps.setInt(2, role.getRoleId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE role SET deleted_at = CURRENT_TIMESTAMP(6) WHERE role_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); return ps.executeUpdate() > 0; }
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE role SET deleted_at = NULL WHERE role_id = ? AND deleted_at IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); return ps.executeUpdate() > 0; }
    }

    private Role mapRow(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setRoleId(rs.getInt("role_id"));
        role.setRoleName(rs.getString("role_name"));
        return role;
    }
}
