package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Sale;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SaleRepository implements GenericRepository<Sale, Integer> {

    private static final String BASE_SELECT =
            "SELECT sale_id, invoice_no, sale_date, user_id, customer_id, subtotal, discount, "
                    + "tax, total, status FROM sale";

    @Override
    public Optional<Sale> findById(Integer id) {
        String sql = BASE_SELECT + " WHERE sale_id = ?";
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
            throw new RepositoryException("Failed to find sale by id: " + id, e);
        }
    }

    @Override
    public List<Sale> findAll() {
        String sql = BASE_SELECT + " ORDER BY sale_id";
        List<Sale> sales = new ArrayList<>();

        try (Connection conn = HikariDatabaseConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                sales.add(mapRow(rs));
            }
            return sales;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all sales", e);
        }
    }

    @Override
    public Integer insert(Sale sale) {
        String sql = "INSERT INTO sale (invoice_no, sale_date, user_id, customer_id, subtotal, "
                + "discount, tax, total, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, sale.getInvoiceNo());
            ps.setTimestamp(2, Timestamp.valueOf(sale.getSaleDate()));
            ps.setInt(3, sale.getUserId());
            if (sale.getCustomerId() != null) {
                ps.setInt(4, sale.getCustomerId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setBigDecimal(5, sale.getSubtotal());
            ps.setBigDecimal(6, sale.getDiscount());
            ps.setBigDecimal(7, sale.getTax());
            ps.setBigDecimal(8, sale.getTotal());
            ps.setString(9, sale.getStatus());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    sale.setSaleId(generatedId);
                    return generatedId;
                }
                throw new RepositoryException("Insert succeeded but no generated key was returned for sale");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert sale: " + sale, e);
        }
    }

    @Override
    public boolean update(Sale sale) {
        String sql = "UPDATE sale SET invoice_no = ?, sale_date = ?, user_id = ?, customer_id = ?, "
                + "subtotal = ?, discount = ?, tax = ?, total = ?, status = ? WHERE sale_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sale.getInvoiceNo());
            ps.setTimestamp(2, Timestamp.valueOf(sale.getSaleDate()));
            ps.setInt(3, sale.getUserId());
            if (sale.getCustomerId() != null) {
                ps.setInt(4, sale.getCustomerId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setBigDecimal(5, sale.getSubtotal());
            ps.setBigDecimal(6, sale.getDiscount());
            ps.setBigDecimal(7, sale.getTax());
            ps.setBigDecimal(8, sale.getTotal());
            ps.setString(9, sale.getStatus());
            ps.setInt(10, sale.getSaleId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update sale: " + sale, e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM sale WHERE sale_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to delete sale with id: " + id, e);
        }
    }

    private Sale mapRow(ResultSet rs) throws SQLException {
        Sale sale = new Sale();
        sale.setSaleId(rs.getInt("sale_id"));
        sale.setInvoiceNo(rs.getString("invoice_no"));
        sale.setSaleDate(rs.getTimestamp("sale_date").toLocalDateTime());
        sale.setUserId(rs.getInt("user_id"));
        int customerId = rs.getInt("customer_id");
        sale.setCustomerId(rs.wasNull() ? null : customerId);
        sale.setSubtotal(rs.getBigDecimal("subtotal"));
        sale.setDiscount(rs.getBigDecimal("discount"));
        sale.setTax(rs.getBigDecimal("tax"));
        sale.setTotal(rs.getBigDecimal("total"));
        sale.setStatus(rs.getString("status"));
        return sale;
    }
}
