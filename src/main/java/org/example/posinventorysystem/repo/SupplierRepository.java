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
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find supplier by id: " + id, e);
        }
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
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return insert(conn, supplier);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert supplier: " + supplier, e);
        }
    }

    @Override
    public boolean update(Supplier supplier) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return update(conn, supplier);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update supplier: " + supplier, e);
        }
    }

    @Override
    public boolean softDelete(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return softDelete(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to soft-delete supplier with id: " + id, e);
        }
    }

    @Override
    public boolean restore(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return restore(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to restore supplier with id: " + id, e);
        }
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
}
