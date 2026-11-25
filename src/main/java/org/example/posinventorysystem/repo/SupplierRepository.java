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
        String sql = "SELECT supplier_id, supplier_name, phone FROM supplier WHERE supplier_id = ?";
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
            throw new RepositoryException("Failed to find supplier by id: " + id, e);
        }
    }

    @Override
    public List<Supplier> findAll() {
        String sql = "SELECT supplier_id, supplier_name, phone FROM supplier ORDER BY supplier_id";
        List<Supplier> suppliers = new ArrayList<>();

        try (Connection conn = HikariDatabaseConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                suppliers.add(mapRow(rs));
            }
            return suppliers;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all suppliers", e);
        }
    }

    @Override
    public Integer insert(Supplier supplier) {
        String sql = "INSERT INTO supplier (supplier_name, phone) VALUES (?, ?)";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, supplier.getSupplierName());
            ps.setString(2, supplier.getPhone());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    supplier.setSupplierId(generatedId);
                    return generatedId;
                }
                throw new RepositoryException("Insert succeeded but no generated key was returned for supplier");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert supplier: " + supplier, e);
        }
    }

    @Override
    public boolean update(Supplier supplier) {
        String sql = "UPDATE supplier SET supplier_name = ?, phone = ? WHERE supplier_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, supplier.getSupplierName());
            ps.setString(2, supplier.getPhone());
            ps.setInt(3, supplier.getSupplierId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update supplier: " + supplier, e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM supplier WHERE supplier_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to delete supplier with id: " + id, e);
        }
    }

    private Supplier mapRow(ResultSet rs) throws SQLException {
        Supplier supplier = new Supplier();
        supplier.setSupplierId(rs.getInt("supplier_id"));
        supplier.setSupplierName(rs.getString("supplier_name"));
        supplier.setPhone(rs.getString("phone"));
        return supplier;
    }
}
