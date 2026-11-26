package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Payment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaymentRepository implements InsertOnlyRepository<Payment, Integer> {

    private static final String BASE_SELECT =
            "SELECT payment_id, sale_id, method, amount, payment_date FROM payment";

    @Override
    public Optional<Payment> findById(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find payment by id: " + id, e);
        }
    }

    public List<Payment> findBySaleId(Integer saleId) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findBySaleId(conn, saleId);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find payments for sale_id: " + saleId, e);
        }
    }

    @Override
    public List<Payment> findAll() {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all payments", e);
        }
    }

    @Override
    public Integer insert(Payment payment) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return insert(conn, payment);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert payment: " + payment, e);
        }
    }

    public Optional<Payment> findById(Connection conn, Integer id) throws SQLException {
        String sql = BASE_SELECT + " WHERE payment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Payment> findBySaleId(Connection conn, Integer saleId) throws SQLException {
        String sql = BASE_SELECT + " WHERE sale_id = ? ORDER BY payment_id";
        List<Payment> payments = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRow(rs));
                }
            }
        }
        return payments;
    }

    public List<Payment> findAll(Connection conn) throws SQLException {
        String sql = BASE_SELECT + " ORDER BY payment_id";
        List<Payment> payments = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                payments.add(mapRow(rs));
            }
        }
        return payments;
    }

    public Integer insert(Connection conn, Payment payment) throws SQLException {
        String sql = "INSERT INTO payment (sale_id, method, amount, payment_date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, payment.getSaleId());
            ps.setString(2, payment.getMethod());
            ps.setBigDecimal(3, payment.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(payment.getPaymentDate()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    payment.setPaymentId(generatedId);
                    return generatedId;
                }
                throw new SQLException("Insert succeeded but no generated key was returned for payment");
            }
        }
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
