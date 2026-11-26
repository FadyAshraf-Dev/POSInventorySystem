package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Purchase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PurchaseRepository implements GenericRepository<Purchase, Integer> {

    private static final String BASE_SELECT =
            "SELECT purchase_id, reference_no, purchase_date, supplier_id, user_id, total, status FROM purchase";

    @Override
    public Optional<Purchase> findById(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find purchase by id: " + id, e);
        }
    }

    @Override
    public List<Purchase> findAll() {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all purchases", e);
        }
    }

    @Override
    public Integer insert(Purchase purchase) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return insert(conn, purchase);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert purchase: " + purchase, e);
        }
    }

    @Override
    public boolean update(Purchase purchase) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return update(conn, purchase);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update purchase: " + purchase, e);
        }
    }

    
    @Override
    public boolean softDelete(Integer id) {
        return org.example.posinventorysystem.data.TransactionManager.run(conn -> softDelete(conn, id));
    }

    @Override
    public boolean restore(Integer id) {
        return org.example.posinventorysystem.data.TransactionManager.run(conn -> restore(conn, id));
    }

    public Optional<Purchase> findById(Connection conn, Integer id) throws SQLException {
        String sql = BASE_SELECT + " WHERE purchase_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Purchase> findAll(Connection conn) throws SQLException {
        String sql = BASE_SELECT + " WHERE deleted_at IS NULL ORDER BY purchase_id";
        List<Purchase> purchases = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                purchases.add(mapRow(rs));
            }
        }
        return purchases;
    }

    public Integer insert(Connection conn, Purchase purchase) throws SQLException {
        String sql = "INSERT INTO purchase (reference_no, purchase_date, supplier_id, user_id, total, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, purchase.getReferenceNo());
            ps.setTimestamp(2, Timestamp.valueOf(purchase.getPurchaseDate()));
            ps.setInt(3, purchase.getSupplierId());
            ps.setInt(4, purchase.getUserId());
            ps.setBigDecimal(5, purchase.getTotal());
            ps.setString(6, purchase.getStatus());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    purchase.setPurchaseId(generatedId);
                    return generatedId;
                }
                throw new SQLException("Insert succeeded but no generated key was returned for purchase");
            }
        }
    }

    public boolean update(Connection conn, Purchase purchase) throws SQLException {
        String sql = "UPDATE purchase SET reference_no = ?, purchase_date = ?, supplier_id = ?, "
                + "user_id = ?, total = ?, status = ? WHERE purchase_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, purchase.getReferenceNo());
            ps.setTimestamp(2, Timestamp.valueOf(purchase.getPurchaseDate()));
            ps.setInt(3, purchase.getSupplierId());
            ps.setInt(4, purchase.getUserId());
            ps.setBigDecimal(5, purchase.getTotal());
            ps.setString(6, purchase.getStatus());
            ps.setInt(7, purchase.getPurchaseId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE purchase SET deleted_at = CURRENT_TIMESTAMP(6) WHERE purchase_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) return false;
        }
        try (PreparedStatement child = conn.prepareStatement("UPDATE purchase_item SET deleted_at = (SELECT deleted_at FROM purchase WHERE purchase_id = ?) WHERE purchase_id = ? AND deleted_at IS NULL")) {
            child.setInt(1, id); child.setInt(2, id); child.executeUpdate();
        }
        return true;
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        try (PreparedStatement child = conn.prepareStatement(
                "UPDATE purchase_item SET deleted_at = NULL WHERE purchase_id = ? "
                        + "AND deleted_at = (SELECT deleted_at FROM purchase WHERE purchase_id = ? AND deleted_at IS NOT NULL)")) {
            child.setInt(1, id); child.setInt(2, id); child.executeUpdate();
        }
        String sql = "UPDATE purchase SET deleted_at = NULL WHERE purchase_id = ? AND deleted_at IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); return ps.executeUpdate() > 0; }
    }

    private Purchase mapRow(ResultSet rs) throws SQLException {
        Purchase purchase = new Purchase();
        purchase.setPurchaseId(rs.getInt("purchase_id"));
        purchase.setReferenceNo(rs.getString("reference_no"));
        purchase.setPurchaseDate(rs.getTimestamp("purchase_date").toLocalDateTime());
        purchase.setSupplierId(rs.getInt("supplier_id"));
        purchase.setUserId(rs.getInt("user_id"));
        purchase.setTotal(rs.getBigDecimal("total"));
        purchase.setStatus(rs.getString("status"));
        return purchase;
    }
}
