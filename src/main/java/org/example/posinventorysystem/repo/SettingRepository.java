package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Setting;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class SettingRepository implements UpdatableRepository<Setting, Integer> {

    @Override
    public Optional<Setting> findById(Integer id) {
        return JdbcExecutor.withConnection("Failed to find setting by id: " + id, conn -> findById(conn, id));
    }

    @Override
    public List<Setting> findAll() {
        return JdbcExecutor.withConnection("Failed to fetch all settings", this::findAll);
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
        return JdbcExecutor.queryOne(conn, sql, ps -> { ps.setInt(1, id); }, this::mapRow);
    }

    public List<Setting> findAll(Connection conn) throws SQLException {
        String sql = "SELECT setting_id, store_name, currency, tax_rate, store_address, store_phone, store_email FROM setting ORDER BY setting_id";
        return JdbcExecutor.query(conn, sql, ps -> { }, this::mapRow);
    }

    public Integer insert(Connection conn, Setting setting) throws SQLException {
        String sql = "INSERT INTO setting (store_name, currency, tax_rate, store_address, store_phone, store_email) VALUES (?, ?, ?, ?, ?, ?)";
        int generatedId = JdbcExecutor.executeInsert(conn, sql, ps -> { ps.setString(1, setting.getStoreName());
            ps.setString(2, setting.getCurrency());
            ps.setBigDecimal(3, setting.getTaxRate());
            ps.setString(4, setting.getStoreAddress());
            ps.setString(5, setting.getStorePhone());
            ps.setString(6, setting.getStoreEmail()); });
        setting.setSettingId(generatedId);
        return generatedId;
    }

    public boolean update(Connection conn, Setting setting) throws SQLException {
        String sql = "UPDATE setting SET store_name = ?, currency = ?, tax_rate = ?, store_address = ?, store_phone = ?, store_email = ? WHERE setting_id = ?";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setString(1, setting.getStoreName());
            ps.setString(2, setting.getCurrency());
            ps.setBigDecimal(3, setting.getTaxRate());
            ps.setString(4, setting.getStoreAddress());
            ps.setString(5, setting.getStorePhone());
            ps.setString(6, setting.getStoreEmail());
            ps.setInt(7, setting.getSettingId()); }) > 0;
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
