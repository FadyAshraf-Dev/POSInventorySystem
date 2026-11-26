package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.JdbcExecutor;
import org.example.posinventorysystem.model.Sale;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class SaleRepository implements GenericRepository<Sale, Integer> {

    private static final String BASE_SELECT =
            "SELECT sale_id, invoice_no, sale_date, user_id, customer_id, subtotal, discount, "
                    + "tax, total, status FROM sale";

    @Override
    public Optional<Sale> findById(Integer id) {
        return JdbcExecutor.withConnection("Failed to find sale by id: " + id, conn -> findById(conn, id));
    }

    @Override
    public List<Sale> findAll() {
        return JdbcExecutor.withConnection("Failed to fetch all sales", this::findAll);
    }

    @Override
    public Integer insert(Sale sale) {
        return JdbcExecutor.withConnection("Failed to insert sale: " + sale, conn -> insert(conn, sale));
    }

    @Override
    public boolean update(Sale sale) {
        return JdbcExecutor.withConnection("Failed to update sale: " + sale, conn -> update(conn, sale));
    }

    
    @Override
    public boolean softDelete(Integer id) {
        return TransactionManager.run(conn -> softDelete(conn, id));
    }

    @Override
    public boolean restore(Integer id) {
        return TransactionManager.run(conn -> restore(conn, id));
    }

    public Optional<Sale> findById(Connection conn, Integer id) throws SQLException {
        String sql = BASE_SELECT + " WHERE sale_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.queryOne(conn, sql, ps -> { ps.setInt(1, id); }, this::mapRow);
    }

    public List<Sale> findAll(Connection conn) throws SQLException {
        String sql = BASE_SELECT + " WHERE deleted_at IS NULL ORDER BY sale_id";
        return JdbcExecutor.query(conn, sql, ps -> { }, this::mapRow);
    }

    public Integer insert(Connection conn, Sale sale) throws SQLException {
        String sql = "INSERT INTO sale (invoice_no, sale_date, user_id, customer_id, subtotal, "
                + "discount, tax, total, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int generatedId = JdbcExecutor.executeInsert(conn, sql, ps -> { ps.setString(1, sale.getInvoiceNo());
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
            ps.setString(9, sale.getStatus()); });
        sale.setSaleId(generatedId);
        return generatedId;
    }

    public boolean update(Connection conn, Sale sale) throws SQLException {
        String sql = "UPDATE sale SET invoice_no = ?, sale_date = ?, user_id = ?, customer_id = ?, "
                + "subtotal = ?, discount = ?, tax = ?, total = ?, status = ? WHERE sale_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> {
            ps.setString(1, sale.getInvoiceNo());
            ps.setTimestamp(2, Timestamp.valueOf(sale.getSaleDate()));
            ps.setInt(3, sale.getUserId());
            if (sale.getCustomerId() != null) ps.setInt(4, sale.getCustomerId());
            else ps.setNull(4, Types.INTEGER);
            ps.setBigDecimal(5, sale.getSubtotal());
            ps.setBigDecimal(6, sale.getDiscount());
            ps.setBigDecimal(7, sale.getTax());
            ps.setBigDecimal(8, sale.getTotal());
            ps.setString(9, sale.getStatus());
            ps.setInt(10, sale.getSaleId());
        }) > 0;
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE sale SET deleted_at = CURRENT_TIMESTAMP(6) WHERE sale_id = ? AND deleted_at IS NULL";
        if (JdbcExecutor.executeUpdate(conn, sql, ps -> ps.setInt(1, id)) == 0) return false;
        String childSql = "UPDATE sale_item SET deleted_at = (SELECT deleted_at FROM sale WHERE sale_id = ?) "
                + "WHERE sale_id = ? AND deleted_at IS NULL";
        JdbcExecutor.executeUpdate(conn, childSql, ps -> {
            ps.setInt(1, id);
            ps.setInt(2, id);
        });
        return true;
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        String childSql = "UPDATE sale_item SET deleted_at = NULL WHERE sale_id = ? "
                + "AND deleted_at = (SELECT deleted_at FROM sale WHERE sale_id = ? AND deleted_at IS NOT NULL)";
        JdbcExecutor.executeUpdate(conn, childSql, ps -> {
            ps.setInt(1, id);
            ps.setInt(2, id);
        });
        String sql = "UPDATE sale SET deleted_at = NULL WHERE sale_id = ? AND deleted_at IS NOT NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setInt(1, id); }) > 0;
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

    /** Finds non-deleted sales in the inclusive date/time range. */
    public List<Sale> findByDateRange(java.time.LocalDateTime from, java.time.LocalDateTime to) {
        return JdbcExecutor.withConnection("Failed to fetch sales for date range", conn -> findByDateRange(conn, from, to));
    }

    public List<Sale> findByDateRange(Connection conn, java.time.LocalDateTime from,
                                       java.time.LocalDateTime to) throws SQLException {
        if (from == null || to == null || from.isAfter(to))
            throw new IllegalArgumentException("Date range must have non-null endpoints and from <= to");
        String sql = BASE_SELECT + " WHERE deleted_at IS NULL AND sale_date >= ? AND sale_date <= ? "
                + "ORDER BY sale_date, sale_id";
        return JdbcExecutor.query(conn, sql, ps -> {
            ps.setObject(1, from);
            ps.setObject(2, to);
        }, this::mapRow);
    }

    public Optional<Sale> findByInvoiceNo(String invoiceNo) {
        return JdbcExecutor.withConnection("Failed to find sale by invoice number: " + invoiceNo, conn -> findByInvoiceNo(conn, invoiceNo));
    }

    public Optional<Sale> findByInvoiceNo(Connection conn, String invoiceNo) throws SQLException {
        String sql = BASE_SELECT + " WHERE invoice_no = ? AND deleted_at IS NULL";
        return JdbcExecutor.queryOne(conn, sql, ps -> { ps.setString(1, invoiceNo); }, this::mapRow);
    }

    /** Returns the invoice number of the newest non-deleted sale, or empty when none exists. */
    public Optional<String> findLastInvoiceNo() {
        return JdbcExecutor.withConnection("Failed to find last invoice number", this::findLastInvoiceNo);
    }

    public Optional<String> findLastInvoiceNo(Connection conn) throws SQLException {
        String sql = "SELECT invoice_no FROM sale WHERE deleted_at IS NULL ORDER BY sale_id DESC LIMIT 1";
        return JdbcExecutor.queryOne(conn, sql, ps -> { }, rs -> rs.getString("invoice_no"));
    }

}
