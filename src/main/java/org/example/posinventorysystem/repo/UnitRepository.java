package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Unit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UnitRepository implements GenericRepository<Unit, Integer> {

    @Override
    public Optional<Unit> findById(Integer id) {
        return JdbcExecutor.withConnection("Failed to find unit by id: " + id, conn -> findById(conn, id));
    }

    @Override
    public List<Unit> findAll() {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all units", e);
        }
    }

    @Override
    public Integer insert(Unit unit) {
        return JdbcExecutor.withConnection("Failed to insert unit: " + unit, conn -> insert(conn, unit));
    }

    @Override
    public boolean update(Unit unit) {
        return JdbcExecutor.withConnection("Failed to update unit: " + unit, conn -> update(conn, unit));
    }

    @Override
    public boolean softDelete(Integer id) {
        return JdbcExecutor.withConnection("Failed to soft-delete unit with id: " + id, conn -> softDelete(conn, id));
    }

    @Override
    public boolean restore(Integer id) {
        return JdbcExecutor.withConnection("Failed to restore unit with id: " + id, conn -> restore(conn, id));
    }

    public Optional<Unit> findById(Connection conn, Integer id) throws SQLException {
        String sql = "SELECT unit_id, unit_name FROM unit WHERE deleted_at IS NULL AND unit_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Unit> findAll(Connection conn) throws SQLException {
        String sql = "SELECT unit_id, unit_name FROM unit WHERE deleted_at IS NULL ORDER BY unit_id";
        List<Unit> units = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                units.add(mapRow(rs));
            }
        }
        return units;
    }

    public Integer insert(Connection conn, Unit unit) throws SQLException {
        String sql = "INSERT INTO unit (unit_name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, unit.getUnitName());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    unit.setUnitId(generatedId);
                    return generatedId;
                }
                throw new SQLException("Insert succeeded but no generated key was returned for unit");
            }
        }
    }

    public boolean update(Connection conn, Unit unit) throws SQLException {
        String sql = "UPDATE unit SET unit_name = ? WHERE unit_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, unit.getUnitName());
            ps.setInt(2, unit.getUnitId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE unit SET deleted_at = CURRENT_TIMESTAMP(6) WHERE unit_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); return ps.executeUpdate() > 0; }
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE unit SET deleted_at = NULL WHERE unit_id = ? AND deleted_at IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); return ps.executeUpdate() > 0; }
    }

    private Unit mapRow(ResultSet rs) throws SQLException {
        Unit unit = new Unit();
        unit.setUnitId(rs.getInt("unit_id"));
        unit.setUnitName(rs.getString("unit_name"));
        return unit;
    }
}
