package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.JdbcExecutor;
import org.example.posinventorysystem.model.Unit;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class UnitRepository implements GenericRepository<Unit, Integer> {

    @Override
    public Optional<Unit> findById(Integer id) {
        return JdbcExecutor.withConnection("Failed to find unit by id: " + id, conn -> findById(conn, id));
    }

    @Override
    public List<Unit> findAll() {
        return JdbcExecutor.withConnection("Failed to fetch all units", this::findAll);
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
        return JdbcExecutor.queryOne(conn, sql, ps -> { ps.setInt(1, id); }, this::mapRow);
    }

    public List<Unit> findAll(Connection conn) throws SQLException {
        String sql = "SELECT unit_id, unit_name FROM unit WHERE deleted_at IS NULL ORDER BY unit_id";
        return JdbcExecutor.query(conn, sql, ps -> { }, this::mapRow);
    }

    public Integer insert(Connection conn, Unit unit) throws SQLException {
        String sql = "INSERT INTO unit (unit_name) VALUES (?)";
        int generatedId = JdbcExecutor.executeInsert(conn, sql, ps -> { ps.setString(1, unit.getUnitName()); });
        unit.setUnitId(generatedId);
        return generatedId;
    }

    public boolean update(Connection conn, Unit unit) throws SQLException {
        String sql = "UPDATE unit SET unit_name = ? WHERE unit_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setString(1, unit.getUnitName());
            ps.setInt(2, unit.getUnitId()); }) > 0;
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE unit SET deleted_at = CURRENT_TIMESTAMP(6) WHERE unit_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setInt(1, id); }) > 0;
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE unit SET deleted_at = NULL WHERE unit_id = ? AND deleted_at IS NOT NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setInt(1, id); }) > 0;
    }

    private Unit mapRow(ResultSet rs) throws SQLException {
        Unit unit = new Unit();
        unit.setUnitId(rs.getInt("unit_id"));
        unit.setUnitName(rs.getString("unit_name"));
        return unit;
    }
}
