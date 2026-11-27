package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.JdbcExecutor;
import org.example.posinventorysystem.model.Supplier;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class SupplierRepository implements GenericRepository<Supplier, Integer> {

    @Override
    public Optional<Supplier> findById(Integer id) {
        return JdbcExecutor.withConnection("Failed to find supplier by id: " + id, conn -> findById(conn, id));
    }

    @Override
    public List<Supplier> findAll() {
        return JdbcExecutor.withConnection("Failed to fetch all suppliers", this::findAll);
    }

    @Override
    public Integer insert(Supplier supplier) {
        return JdbcExecutor.withConnection("Failed to insert supplier: " + supplier, conn -> insert(conn, supplier));
    }

    @Override
    public boolean update(Supplier supplier) {
        return JdbcExecutor.withConnection("Failed to update supplier: " + supplier, conn -> update(conn, supplier));
    }

    @Override
    public boolean softDelete(Integer id) {
        return JdbcExecutor.withConnection("Failed to soft-delete supplier with id: " + id, conn -> softDelete(conn, id));
    }

    @Override
    public boolean restore(Integer id) {
        return JdbcExecutor.withConnection("Failed to restore supplier with id: " + id, conn -> restore(conn, id));
    }

    public Optional<Supplier> findById(Connection conn, Integer id) throws SQLException {
        String sql = "SELECT supplier_id, supplier_name, phone, address FROM supplier WHERE deleted_at IS NULL AND supplier_id = ?";
        return JdbcExecutor.queryOne(conn, sql, ps -> { ps.setInt(1, id); }, this::mapRow);
    }

    public List<Supplier> findAll(Connection conn) throws SQLException {
        String sql = "SELECT supplier_id, supplier_name, phone, address FROM supplier WHERE deleted_at IS NULL ORDER BY supplier_id";
        return JdbcExecutor.query(conn, sql, ps -> { }, this::mapRow);
    }

    public Integer insert(Connection conn, Supplier supplier) throws SQLException {
        String sql = "INSERT INTO supplier (supplier_name, phone, address) VALUES (?, ?, ?)";
        int generatedId = JdbcExecutor.executeInsert(conn, sql, ps -> { ps.setString(1, supplier.getSupplierName());
            ps.setString(2, supplier.getPhone());
            ps.setString(3, supplier.getAddress()); });
        supplier.setSupplierId(generatedId);
        return generatedId;
    }

    public boolean update(Connection conn, Supplier supplier) throws SQLException {
        String sql = "UPDATE supplier SET supplier_name = ?, phone = ?, address = ? WHERE supplier_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setString(1, supplier.getSupplierName());
            ps.setString(2, supplier.getPhone());
            ps.setString(3, supplier.getAddress());
            ps.setInt(4, supplier.getSupplierId()); }) > 0;
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE supplier SET deleted_at = CURRENT_TIMESTAMP(6) WHERE supplier_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setInt(1, id); }) > 0;
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE supplier SET deleted_at = NULL WHERE supplier_id = ? AND deleted_at IS NOT NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setInt(1, id); }) > 0;
    }

    private Supplier mapRow(ResultSet rs) throws SQLException {
        Supplier supplier = new Supplier();
        supplier.setSupplierId(rs.getInt("supplier_id"));
        supplier.setSupplierName(rs.getString("supplier_name"));
        supplier.setPhone(rs.getString("phone"));
        supplier.setAddress(rs.getString("address"));
        return supplier;
    }

    /** Case-insensitive search across supplier name and phone for non-deleted records. */
    public List<Supplier> search(String term) {
        return JdbcExecutor.withConnection("Failed to search suppliers", conn -> search(conn, term));
    }

    public List<Supplier> search(Connection conn, String term) throws SQLException {
        if (term == null || term.isBlank()) return List.of();
        String sql = "SELECT supplier_id, supplier_name, phone, address FROM supplier WHERE deleted_at IS NULL AND (LOWER(COALESCE(supplier_name, '')) LIKE ? OR LOWER(COALESCE(phone, '')) LIKE ? OR LOWER(COALESCE(address, '')) LIKE ?) ORDER BY supplier_name, supplier_id";
        String pattern = "%" + term.trim().toLowerCase() + "%";
        return JdbcExecutor.query(conn, sql, ps -> {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
        }, this::mapRow);
    }

    /** Returns true if any purchase record references this supplier, including historical/soft-deleted records. */
    public boolean hasPurchases(Integer id) {
        return JdbcExecutor.withConnection("Failed to check references for supplier id: " + id, conn -> hasPurchases(conn, id));
    }

    public boolean hasPurchases(Connection conn, Integer id) throws SQLException {
        String sql = "SELECT 1 FROM purchase WHERE supplier_id = ? LIMIT 1";
        return JdbcExecutor.exists(conn, sql, ps -> { ps.setInt(1, id); });
    }

}
