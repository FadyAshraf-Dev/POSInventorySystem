package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Payment;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class PaymentRepository implements InsertOnlyRepository<Payment, Integer> {

    private static final String BASE_SELECT =
            "SELECT payment_id, sale_id, method, amount, payment_date FROM payment";

    @Override
    public Optional<Payment> findById(Integer id) {
        return JdbcExecutor.withConnection("Failed to find payment by id: " + id, conn -> findById(conn, id));
    }

    public List<Payment> findBySaleId(Integer saleId) {
        return JdbcExecutor.withConnection("Failed to find payments for sale_id: " + saleId, conn -> findBySaleId(conn, saleId));
    }

    @Override
    public List<Payment> findAll() {
        return JdbcExecutor.withConnection("Failed to fetch all payments", this::findAll);
    }

    @Override
    public Integer insert(Payment payment) {
        return JdbcExecutor.withConnection("Failed to insert payment: " + payment, conn -> insert(conn, payment));
    }

    public Optional<Payment> findById(Connection conn, Integer id) throws SQLException {
        String sql = BASE_SELECT + " WHERE payment_id = ?";
        return JdbcExecutor.queryOne(conn, sql, ps -> { ps.setInt(1, id); }, this::mapRow);
    }

    public List<Payment> findBySaleId(Connection conn, Integer saleId) throws SQLException {
        String sql = BASE_SELECT + " WHERE sale_id = ? ORDER BY payment_id";
        return JdbcExecutor.query(conn, sql, ps -> { ps.setInt(1, saleId); }, this::mapRow);
    }

    public List<Payment> findAll(Connection conn) throws SQLException {
        String sql = BASE_SELECT + " ORDER BY payment_id";
        return JdbcExecutor.query(conn, sql, ps -> { }, this::mapRow);
    }

    public Integer insert(Connection conn, Payment payment) throws SQLException {
        String sql = "INSERT INTO payment (sale_id, method, amount, payment_date) VALUES (?, ?, ?, ?)";
        int generatedId = JdbcExecutor.executeInsert(conn, sql, ps -> { ps.setInt(1, payment.getSaleId());
            ps.setString(2, payment.getMethod());
            ps.setBigDecimal(3, payment.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(payment.getPaymentDate())); });
        payment.setPaymentId(generatedId);
        return generatedId;
    }



    private Payment mapRow(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        payment.setPaymentId(rs.getInt("payment_id"));
        payment.setSaleId(rs.getInt("sale_id"));
        payment.setMethod(rs.getString("method"));
        payment.setAmount(rs.getBigDecimal("amount"));
        payment.setPaymentDate(rs.getTimestamp("payment_date").toLocalDateTime());
        return payment;
    }
}
