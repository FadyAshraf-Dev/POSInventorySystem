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
        String sql = BASE_SELECT + " WHERE purchase_item_id = ?";
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
            throw new RepositoryException("Failed to find purchase item by id: " + id, e);
        }
    }

    /** Convenience lookup — line items belonging to one purchase. */
    public List<PurchaseItem> findByPurchaseId(Integer purchaseId) {
        String sql = BASE_SELECT + " WHERE purchase_id = ? ORDER BY purchase_item_id";
        List<PurchaseItem> items = new ArrayList<>();

        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, purchaseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
            return items;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find purchase items for purchase_id: " + purchaseId, e);
        }
    }

    @Override
    public List<PurchaseItem> findAll() {
        String sql = BASE_SELECT + " ORDER BY purchase_item_id";
        List<PurchaseItem> items = new ArrayList<>();

        try (Connection conn = HikariDatabaseConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                items.add(mapRow(rs));
            }
            return items;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all purchase items", e);
        }
    }

    @Override
    public Integer insert(PurchaseItem item) {
        String sql = "INSERT INTO purchase_item (purchase_id, product_id, quantity, unit_cost, line_total) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, item.getPurchaseId());
            ps.setInt(2, item.getProductId());
            ps.setBigDecimal(3, item.getQuantity());
            ps.setBigDecimal(4, item.getUnitCost());
            ps.setBigDecimal(5, item.getLineTotal());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    item.setPurchaseItemId(generatedId);
                    return generatedId;
                }
                throw new RepositoryException("Insert succeeded but no generated key was returned for purchase item");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert purchase item: " + item, e);
        }
    }

    @Override
    public boolean update(PurchaseItem item) {
        String sql = "UPDATE purchase_item SET purchase_id = ?, product_id = ?, quantity = ?, "
                + "unit_cost = ?, line_total = ? WHERE purchase_item_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, item.getPurchaseId());
            ps.setInt(2, item.getProductId());
            ps.setBigDecimal(3, item.getQuantity());
            ps.setBigDecimal(4, item.getUnitCost());
            ps.setBigDecimal(5, item.getLineTotal());
            ps.setInt(6, item.getPurchaseItemId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update purchase item: " + item, e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM purchase_item WHERE purchase_item_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to delete purchase item with id: " + id, e);
        }
    }

    private PurchaseItem mapRow(ResultSet rs) throws SQLException {
        PurchaseItem item = new PurchaseItem();
        item.setPurchaseItemId(rs.getInt("purchase_item_id"));
        item.setPurchaseId(rs.getInt("purchase_id"));
        item.setProductId(rs.getInt("product_id"));
        item.setQuantity(rs.getBigDecimal("quantity"));
        item.setUnitCost(rs.getBigDecimal("unit_cost"));
        item.setLineTotal(rs.getBigDecimal("line_total"));
        return item;
    }
}
