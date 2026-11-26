package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Setting;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SettingRepository implements GenericRepository<Setting, Integer> {

    @Override
    public Optional<Setting> findById(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find setting by id: " + id, e);
        }
    }

    @Override
    public List<Setting> findAll() {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all settings", e);
        }
    }

    @Override
    public Integer insert(Setting setting) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return insert(conn, setting);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert setting: " + setting, e);
        }
    }

    @Override
    public boolean update(Setting setting) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return update(conn, setting);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update setting: " + setting, e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return deleteById(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to delete setting with id: " + id, e);
        }
    }

    public Optional<Setting> findById(Connection conn, Integer id) throws SQLException {
        String sql = "SELECT setting_id, store_name, currency, tax_rate FROM setting WHERE setting_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Setting> findAll(Connection conn) throws SQLException {
        String sql = "SELECT setting_id, store_name, currency, tax_rate FROM setting ORDER BY setting_id";
        List<Setting> settings = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                settings.add(mapRow(rs));
            }
        }
        return settings;
    }

    public Integer insert(Connection conn, Setting setting) throws SQLException {
        String sql = "INSERT INTO setting (store_name, currency, tax_rate) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, setting.getStoreName());
            ps.setString(2, setting.getCurrency());
            ps.setBigDecimal(3, setting.getTaxRate());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    setting.setSettingId(generatedId);
                    return generatedId;
                }
                throw new SQLException("Insert succeeded but no generated key was returned for setting");
            }
        }
    }

    public boolean update(Connection conn, Setting setting) throws SQLException {
        String sql = "UPDATE setting SET store_name = ?, currency = ?, tax_rate = ? WHERE setting_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, setting.getStoreName());
            ps.setString(2, setting.getCurrency());
            ps.setBigDecimal(3, setting.getTaxRate());
            ps.setInt(4, setting.getSettingId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteById(Connection conn, Integer id) throws SQLException {
        String sql = "DELETE FROM setting WHERE setting_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Setting mapRow(ResultSet rs) throws SQLException {
        Setting setting = new Setting();
        setting.setSettingId(rs.getInt("setting_id"));
        setting.setStoreName(rs.getString("store_name"));
        setting.setCurrency(rs.getString("currency"));
        setting.setTaxRate(rs.getBigDecimal("tax_rate"));
        return setting;
    }
}
