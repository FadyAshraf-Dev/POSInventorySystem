package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Setting;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SettingRepository implements UpdatableRepository<Setting, Integer> {

    @Override
    public Optional<Setting> findById(Integer id) {
        return JdbcExecutor.withConnection("Failed to find setting by id: " + id, conn -> findById(conn, id));
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
        return JdbcExecutor.withConnection("Failed to insert setting: " + setting, conn -> insert(conn, setting));
    }

    @Override
    public boolean update(Setting setting) {
        return JdbcExecutor.withConnection("Failed to update setting: " + setting, conn -> update(conn, setting));
    }

    public Optional<Setting> findById(Connection conn, Integer id) throws SQLException {
        String sql = "SELECT setting_id, store_name, currency, tax_rate, store_address, store_phone, store_email FROM setting WHERE setting_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Setting> findAll(Connection conn) throws SQLException {
        String sql = "SELECT setting_id, store_name, currency, tax_rate, store_address, store_phone, store_email FROM setting ORDER BY setting_id";
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
        String sql = "INSERT INTO setting (store_name, currency, tax_rate, store_address, store_phone, store_email) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, setting.getStoreName());
            ps.setString(2, setting.getCurrency());
            ps.setBigDecimal(3, setting.getTaxRate());
            ps.setString(4, setting.getStoreAddress());
            ps.setString(5, setting.getStorePhone());
            ps.setString(6, setting.getStoreEmail());
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
        String sql = "UPDATE setting SET store_name = ?, currency = ?, tax_rate = ?, store_address = ?, store_phone = ?, store_email = ? WHERE setting_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, setting.getStoreName());
            ps.setString(2, setting.getCurrency());
            ps.setBigDecimal(3, setting.getTaxRate());
            ps.setString(4, setting.getStoreAddress());
            ps.setString(5, setting.getStorePhone());
            ps.setString(6, setting.getStoreEmail());
            ps.setInt(7, setting.getSettingId());
            return ps.executeUpdate() > 0;
        }
    }


    private Setting mapRow(ResultSet rs) throws SQLException {
        Setting setting = new Setting();
        setting.setSettingId(rs.getInt("setting_id"));
        setting.setStoreName(rs.getString("store_name"));
        setting.setCurrency(rs.getString("currency"));
        setting.setTaxRate(rs.getBigDecimal("tax_rate"));
        setting.setStoreAddress(rs.getString("store_address"));
        setting.setStorePhone(rs.getString("store_phone"));
        setting.setStoreEmail(rs.getString("store_email"));
        return setting;
    }
}
