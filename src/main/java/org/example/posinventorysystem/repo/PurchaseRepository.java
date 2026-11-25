package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Purchase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PurchaseRepository implements GenericRepository<Purchase, Integer> {

    private static final String BASE_SELECT =
            "SELECT purchase_id, reference_no, purchase_date, supplier_id, user_id, total FROM purchase";

    @Override
    public Optional<Purchase> findById(Integer id) {
        String sql = BASE_SELECT + " WHERE purchase_id = ?";
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
            throw new RepositoryException("Failed to find purchase by id: " + id, e);
        }
    }

    @Override
    public List<Purchase> findAll() {
        String sql = BASE_SELECT + " ORDER BY purchase_id";
        List<Purchase> purchases = new ArrayList<>();

        try (Connection conn = HikariDatabaseConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                purchases.add(mapRow(rs));
            }
            return purchases;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all purchases", e);
        }
    }

    @Override
    public Integer insert(Purchase purchase) {
        String sql = "INSERT INTO purchase (reference_no, purchase_date, supplier_id, user_id, total) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, purchase.getReferenceNo());
            ps.setTimestamp(2, Timestamp.valueOf(purchase.getPurchaseDate()));
            ps.setInt(3, purchase.getSupplierId());
            ps.setInt(4, purchase.getUserId());
            ps.setBigDecimal(5, purchase.getTotal());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    purchase.setPurchaseId(generatedId);
                    return generatedId;
                }
                throw new RepositoryException("Insert succeeded but no generated key was returned for purchase");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert purchase: " + purchase, e);
        }
    }

    @Override
    public boolean update(Purchase purchase) {
        String sql = "UPDATE purchase SET reference_no = ?, purchase_date = ?, supplier_id = ?, "
                + "user_id = ?, total = ? WHERE purchase_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, purchase.getReferenceNo());
            ps.setTimestamp(2, Timestamp.valueOf(purchase.getPurchaseDate()));
            ps.setInt(3, purchase.getSupplierId());
            ps.setInt(4, purchase.getUserId());
            ps.setBigDecimal(5, purchase.getTotal());
            ps.setInt(6, purchase.getPurchaseId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update purchase: " + purchase, e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM purchase WHERE purchase_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to delete purchase with id: " + id, e);
        }
    }

    private Purchase mapRow(ResultSet rs) throws SQLException {
        Purchase purchase = new Purchase();
        purchase.setPurchaseId(rs.getInt("purchase_id"));
        purchase.setReferenceNo(rs.getString("reference_no"));
        purchase.setPurchaseDate(rs.getTimestamp("purchase_date").toLocalDateTime());
        purchase.setSupplierId(rs.getInt("supplier_id"));
        purchase.setUserId(rs.getInt("user_id"));
        purchase.setTotal(rs.getBigDecimal("total"));
        return purchase;
    }
}
