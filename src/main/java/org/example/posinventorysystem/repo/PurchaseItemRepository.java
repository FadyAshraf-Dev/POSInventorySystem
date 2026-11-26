package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.PurchaseItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PurchaseItemRepository implements GenericRepository<PurchaseItem, Integer> {

    private static final String BASE_SELECT =
            "SELECT purchase_item_id, purchase_id, product_id, quantity, unit_cost, line_total "
                    + "FROM purchase_item";

    @Override
    public Optional<PurchaseItem> findById(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find purchase item by id: " + id, e);
        }
    }

    public List<PurchaseItem> findByPurchaseId(Integer purchaseId) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findByPurchaseId(conn, purchaseId);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find purchase items for purchase_id: " + purchaseId, e);
        }
    }

    @Override
    public List<PurchaseItem> findAll() {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all purchase items", e);
        }
    }

    @Override
    public Integer insert(PurchaseItem item) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return insert(conn, item);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert purchase item: " + item, e);
        }
    }

    @Override
    public boolean update(PurchaseItem item) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return update(conn, item);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update purchase item: " + item, e);
        }
    }

    
    @Override
    public boolean softDelete(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return softDelete(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to soft-delete purchaseitem with id: " + id, e);
        }
    }

    @Override
    public boolean restore(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return restore(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to restore purchaseitem with id: " + id, e);
        }
    }

    public Optional<PurchaseItem> findById(Connection conn, Integer id) throws SQLException {
        String sql = BASE_SELECT + " WHERE purchase_item_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<PurchaseItem> findByPurchaseId(Connection conn, Integer purchaseId) throws SQLException {
        String sql = BASE_SELECT + " WHERE purchase_id = ? AND deleted_at IS NULL ORDER BY purchase_item_id";
        List<PurchaseItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, purchaseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        }
        return items;
    }

    public List<PurchaseItem> findAll(Connection conn) throws SQLException {
        String sql = BASE_SELECT + " WHERE deleted_at IS NULL ORDER BY purchase_item_id";
        List<PurchaseItem> items = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                items.add(mapRow(rs));
            }
        }
        return items;
    }

    public Integer insert(Connection conn, PurchaseItem item) throws SQLException {
        String sql = "INSERT INTO purchase_item (purchase_id, product_id, quantity, unit_cost, line_total) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.getPurchaseId());
            ps.setInt(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, item.getUnitCost());
            ps.setBigDecimal(5, item.getLineTotal());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    item.setPurchaseItemId(generatedId);
                    return generatedId;
                }
                throw new SQLException("Insert succeeded but no generated key was returned for purchase item");
            }
        }
    }

    public boolean update(Connection conn, PurchaseItem item) throws SQLException {
        String sql = "UPDATE purchase_item SET purchase_id = ?, product_id = ?, quantity = ?, "
                + "unit_cost = ?, line_total = ? WHERE purchase_item_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getPurchaseId());
            ps.setInt(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, item.getUnitCost());
            ps.setBigDecimal(5, item.getLineTotal());
            ps.setInt(6, item.getPurchaseItemId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE purchase_item SET deleted_at = CURRENT_TIMESTAMP(6) WHERE purchase_item_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); return ps.executeUpdate() > 0; }
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE purchase_item SET deleted_at = NULL WHERE purchase_item_id = ? AND deleted_at IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); return ps.executeUpdate() > 0; }
    }

    private PurchaseItem mapRow(ResultSet rs) throws SQLException {
        PurchaseItem item = new PurchaseItem();
        item.setPurchaseItemId(rs.getInt("purchase_item_id"));
        item.setPurchaseId(rs.getInt("purchase_id"));
        item.setProductId(rs.getInt("product_id"));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitCost(rs.getBigDecimal("unit_cost"));
        item.setLineTotal(rs.getBigDecimal("line_total"));
        return item;
    }
}
