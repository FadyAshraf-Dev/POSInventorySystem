package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.JdbcExecutor;
import org.example.posinventorysystem.model.PurchaseItem;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class PurchaseItemRepository implements GenericRepository<PurchaseItem, Integer> {

    private static final String BASE_SELECT =
            "SELECT purchase_item_id, purchase_id, product_id, quantity, unit_cost, line_total "
                    + "FROM purchase_item";

    @Override
    public Optional<PurchaseItem> findById(Integer id) {
        return JdbcExecutor.withConnection("Failed to find purchase item by id: " + id, conn -> findById(conn, id));
    }

    public List<PurchaseItem> findByPurchaseId(Integer purchaseId) {
        return JdbcExecutor.withConnection("Failed to find purchase items for purchase_id: " + purchaseId, conn -> findByPurchaseId(conn, purchaseId));
    }

    @Override
    public List<PurchaseItem> findAll() {
        return JdbcExecutor.withConnection("Failed to fetch all purchase items", this::findAll);
    }

    @Override
    public Integer insert(PurchaseItem item) {
        return JdbcExecutor.withConnection("Failed to insert purchase item: " + item, conn -> insert(conn, item));
    }

    @Override
    public boolean update(PurchaseItem item) {
        return JdbcExecutor.withConnection("Failed to update purchase item: " + item, conn -> update(conn, item));
    }

    
    @Override
    public boolean softDelete(Integer id) {
        return JdbcExecutor.withConnection("Failed to soft-delete purchaseitem with id: " + id, conn -> softDelete(conn, id));
    }

    @Override
    public boolean restore(Integer id) {
        return JdbcExecutor.withConnection("Failed to restore purchaseitem with id: " + id, conn -> restore(conn, id));
    }

    public Optional<PurchaseItem> findById(Connection conn, Integer id) throws SQLException {
        String sql = BASE_SELECT + " WHERE purchase_item_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.queryOne(conn, sql, ps -> { ps.setInt(1, id); }, this::mapRow);
    }

    public List<PurchaseItem> findByPurchaseId(Connection conn, Integer purchaseId) throws SQLException {
        String sql = BASE_SELECT + " WHERE purchase_id = ? AND deleted_at IS NULL ORDER BY purchase_item_id";
        return JdbcExecutor.query(conn, sql, ps -> { ps.setInt(1, purchaseId); }, this::mapRow);
    }

    public List<PurchaseItem> findAll(Connection conn) throws SQLException {
        String sql = BASE_SELECT + " WHERE deleted_at IS NULL ORDER BY purchase_item_id";
        return JdbcExecutor.query(conn, sql, ps -> { }, this::mapRow);
    }

    public Integer insert(Connection conn, PurchaseItem item) throws SQLException {
        String sql = "INSERT INTO purchase_item (purchase_id, product_id, quantity, unit_cost, line_total) "
                + "VALUES (?, ?, ?, ?, ?)";
        int generatedId = JdbcExecutor.executeInsert(conn, sql, ps -> { ps.setInt(1, item.getPurchaseId());
            ps.setInt(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, item.getUnitCost());
            ps.setBigDecimal(5, item.getLineTotal()); });
        item.setPurchaseItemId(generatedId);
        return generatedId;
    }

    public boolean update(Connection conn, PurchaseItem item) throws SQLException {
        String sql = "UPDATE purchase_item SET purchase_id = ?, product_id = ?, quantity = ?, "
                + "unit_cost = ?, line_total = ? WHERE purchase_item_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setInt(1, item.getPurchaseId());
            ps.setInt(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, item.getUnitCost());
            ps.setBigDecimal(5, item.getLineTotal());
            ps.setInt(6, item.getPurchaseItemId()); }) > 0;
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE purchase_item SET deleted_at = CURRENT_TIMESTAMP(6) WHERE purchase_item_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setInt(1, id); }) > 0;
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE purchase_item SET deleted_at = NULL WHERE purchase_item_id = ? AND deleted_at IS NOT NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setInt(1, id); }) > 0;
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
