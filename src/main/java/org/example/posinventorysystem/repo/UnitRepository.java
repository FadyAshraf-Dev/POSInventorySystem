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
        String sql = "SELECT unit_id, unit_name FROM unit WHERE unit_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find unit by id: " + id, e);
        }
    }

    @Override
    public List<Unit> findAll() {
        String sql = "SELECT unit_id, unit_name FROM unit ORDER BY unit_id";
        List<Unit> units = new ArrayList<>();

        try (Connection conn = HikariDatabaseConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                units.add(mapRow(rs));
            }
            return units;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all units", e);
        }
    }

    @Override
    public Integer insert(Unit unit) {
        String sql = "INSERT INTO unit (unit_name) VALUES (?)";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, unit.getUnitName());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    unit.setUnitId(generatedId);
                    return generatedId;
                }
                throw new RepositoryException("Insert succeeded but no generated key was returned for unit");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert unit: " + unit, e);
        }
    }

    @Override
    public boolean update(Unit unit) {
        String sql = "UPDATE unit SET unit_name = ? WHERE unit_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, unit.getUnitName());
            ps.setInt(2, unit.getUnitId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update unit: " + unit, e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM unit WHERE unit_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to delete unit with id: " + id, e);
        }
    }

    private Unit mapRow(ResultSet rs) throws SQLException {
        Unit unit = new Unit();
        unit.setUnitId(rs.getInt("unit_id"));
        unit.setUnitName(rs.getString("unit_name"));
        return unit;
    }
}
