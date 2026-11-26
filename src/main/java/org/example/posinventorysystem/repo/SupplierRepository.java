package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Supplier;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SupplierRepository implements GenericRepository<Supplier, Integer> {

    @Override
    public Optional<Supplier> findById(Integer id) {
        return JdbcExecutor.withConnection("Failed to find supplier by id: " + id, conn -> findById(conn, id));
    }

    @Override
    public List<Supplier> findAll() {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all suppliers", e);
        }
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
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Supplier> findAll(Connection conn) throws SQLException {
        String sql = "SELECT supplier_id, supplier_name, phone, address FROM supplier WHERE deleted_at IS NULL ORDER BY supplier_id";
        List<Supplier> suppliers = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                suppliers.add(mapRow(rs));
            }
        }
        return suppliers;
    }

    public Integer insert(Connection conn, Supplier supplier) throws SQLException {
        String sql = "INSERT INTO supplier (supplier_name, phone, address) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, supplier.getSupplierName());
            ps.setString(2, supplier.getPhone());
            ps.setString(3, supplier.getAddress());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    supplier.setSupplierId(generatedId);
                    return generatedId;
                }
                throw new SQLException("Insert succeeded but no generated key was returned for supplier");
            }
        }
    }

    public boolean update(Connection conn, Supplier supplier) throws SQLException {
        String sql = "UPDATE supplier SET supplier_name = ?, phone = ?, address = ? WHERE supplier_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, supplier.getSupplierName());
            ps.setString(2, supplier.getPhone());
            ps.setString(3, supplier.getAddress());
            ps.setInt(4, supplier.getSupplierId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE supplier SET deleted_at = CURRENT_TIMESTAMP(6) WHERE supplier_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); return ps.executeUpdate() > 0; }
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE supplier SET deleted_at = NULL WHERE supplier_id = ? AND deleted_at IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); return ps.executeUpdate() > 0; }
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
        List<Supplier> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 3; i++) ps.setString(i, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(mapRow(rs));
            }
        }
        return rows;
    }

    /** Returns true if any purchase record references this supplier, including historical/soft-deleted records. */
    public boolean hasPurchases(Integer id) {
        return JdbcExecutor.withConnection("Failed to check references for supplier id: " + id, conn -> hasPurchases(conn, id));
    }

    public boolean hasPurchases(Connection conn, Integer id) throws SQLException {
        String sql = "SELECT 1 FROM purchase WHERE supplier_id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

}
