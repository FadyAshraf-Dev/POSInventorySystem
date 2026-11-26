package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.JdbcExecutor;
import org.example.posinventorysystem.model.Purchase;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class PurchaseRepository implements GenericRepository<Purchase, Integer> {

    private static final String BASE_SELECT =
            "SELECT purchase_id, reference_no, purchase_date, supplier_id, user_id, total, status FROM purchase";

    @Override
    public Optional<Purchase> findById(Integer id) {
        return JdbcExecutor.withConnection("Failed to find purchase by id: " + id, conn -> findById(conn, id));
    }

    @Override
    public List<Purchase> findAll() {
        return JdbcExecutor.withConnection("Failed to fetch all purchases", this::findAll);
    }

    @Override
    public Integer insert(Purchase purchase) {
        return JdbcExecutor.withConnection("Failed to insert purchase: " + purchase, conn -> insert(conn, purchase));
    }

    @Override
    public boolean update(Purchase purchase) {
        return JdbcExecutor.withConnection("Failed to update purchase: " + purchase, conn -> update(conn, purchase));
    }

    
    @Override
    public boolean softDelete(Integer id) {
        return TransactionManager.run(conn -> softDelete(conn, id));
    }

    @Override
    public boolean restore(Integer id) {
        return TransactionManager.run(conn -> restore(conn, id));
    }

    public Optional<Purchase> findById(Connection conn, Integer id) throws SQLException {
        String sql = BASE_SELECT + " WHERE purchase_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.queryOne(conn, sql, ps -> { ps.setInt(1, id); }, this::mapRow);
    }

    public List<Purchase> findAll(Connection conn) throws SQLException {
        String sql = BASE_SELECT + " WHERE deleted_at IS NULL ORDER BY purchase_id";
        return JdbcExecutor.query(conn, sql, ps -> { }, this::mapRow);
    }

    public Integer insert(Connection conn, Purchase purchase) throws SQLException {
        String sql = "INSERT INTO purchase (reference_no, purchase_date, supplier_id, user_id, total, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        int generatedId = JdbcExecutor.executeInsert(conn, sql, ps -> { ps.setString(1, purchase.getReferenceNo());
            ps.setTimestamp(2, Timestamp.valueOf(purchase.getPurchaseDate()));
            ps.setInt(3, purchase.getSupplierId());
            ps.setInt(4, purchase.getUserId());
            ps.setBigDecimal(5, purchase.getTotal());
            ps.setString(6, purchase.getStatus()); });
        purchase.setPurchaseId(generatedId);
        return generatedId;
    }

    public boolean update(Connection conn, Purchase purchase) throws SQLException {
        String sql = "UPDATE purchase SET reference_no = ?, purchase_date = ?, supplier_id = ?, "
                + "user_id = ?, total = ?, status = ? WHERE purchase_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setString(1, purchase.getReferenceNo());
            ps.setTimestamp(2, Timestamp.valueOf(purchase.getPurchaseDate()));
            ps.setInt(3, purchase.getSupplierId());
            ps.setInt(4, purchase.getUserId());
            ps.setBigDecimal(5, purchase.getTotal());
            ps.setString(6, purchase.getStatus());
            ps.setInt(7, purchase.getPurchaseId()); }) > 0;
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE purchase SET deleted_at = CURRENT_TIMESTAMP(6) WHERE purchase_id = ? AND deleted_at IS NULL";
        if (JdbcExecutor.executeUpdate(conn, sql, ps -> ps.setInt(1, id)) == 0) return false;
        String childSql = "UPDATE purchase_item SET deleted_at = (SELECT deleted_at FROM purchase WHERE purchase_id = ?) "
                + "WHERE purchase_id = ? AND deleted_at IS NULL";
        JdbcExecutor.executeUpdate(conn, childSql, ps -> {
            ps.setInt(1, id);
            ps.setInt(2, id);
        });
        return true;
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        String childSql = "UPDATE purchase_item SET deleted_at = NULL WHERE purchase_id = ? "
                + "AND deleted_at = (SELECT deleted_at FROM purchase WHERE purchase_id = ? AND deleted_at IS NOT NULL)";
        JdbcExecutor.executeUpdate(conn, childSql, ps -> {
            ps.setInt(1, id);
            ps.setInt(2, id);
        });
        String sql = "UPDATE purchase SET deleted_at = NULL WHERE purchase_id = ? AND deleted_at IS NOT NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setInt(1, id); }) > 0;
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
