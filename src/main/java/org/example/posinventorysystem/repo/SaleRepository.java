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
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find sale by id: " + id, e);
        }
    }

    @Override
    public List<Sale> findAll() {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all sales", e);
        }
    }

    @Override
    public Integer insert(Sale sale) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return insert(conn, sale);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert sale: " + sale, e);
        }
    }

    @Override
    public boolean update(Sale sale) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return update(conn, sale);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update sale: " + sale, e);
        }
    }

    
    @Override
    public boolean softDelete(Integer id) {
        return org.example.posinventorysystem.data.TransactionManager.run(conn -> softDelete(conn, id));
    }

    @Override
    public boolean restore(Integer id) {
        return org.example.posinventorysystem.data.TransactionManager.run(conn -> restore(conn, id));
    }

    public Optional<Sale> findById(Connection conn, Integer id) throws SQLException {
        String sql = BASE_SELECT + " WHERE sale_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Sale> findAll(Connection conn) throws SQLException {
        String sql = BASE_SELECT + " WHERE deleted_at IS NULL ORDER BY sale_id";
        List<Sale> sales = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                sales.add(mapRow(rs));
            }
        }
        return sales;
    }

    public Integer insert(Connection conn, Sale sale) throws SQLException {
        String sql = "INSERT INTO sale (invoice_no, sale_date, user_id, customer_id, subtotal, "
                + "discount, tax, total, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
                throw new SQLException("Insert succeeded but no generated key was returned for sale");
            }
        }
    }

    public boolean update(Connection conn, Sale sale) throws SQLException {
        String sql = "UPDATE sale SET invoice_no = ?, sale_date = ?, user_id = ?, customer_id = ?, "
                + "subtotal = ?, discount = ?, tax = ?, total = ?, status = ? WHERE sale_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
        }
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE sale SET deleted_at = CURRENT_TIMESTAMP(6) WHERE sale_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) return false;
        }
        try (PreparedStatement child = conn.prepareStatement("UPDATE sale_item SET deleted_at = (SELECT deleted_at FROM sale WHERE sale_id = ?) WHERE sale_id = ? AND deleted_at IS NULL")) {
            child.setInt(1, id); child.setInt(2, id); child.executeUpdate();
        }
        return true;
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        try (PreparedStatement child = conn.prepareStatement(
                "UPDATE sale_item SET deleted_at = NULL WHERE sale_id = ? "
                        + "AND deleted_at = (SELECT deleted_at FROM sale WHERE sale_id = ? AND deleted_at IS NOT NULL)")) {
            child.setInt(1, id); child.setInt(2, id); child.executeUpdate();
        }
        String sql = "UPDATE sale SET deleted_at = NULL WHERE sale_id = ? AND deleted_at IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); return ps.executeUpdate() > 0; }
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
