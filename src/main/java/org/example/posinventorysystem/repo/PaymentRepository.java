package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Payment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaymentRepository implements GenericRepository<Payment, Integer> {

    private static final String BASE_SELECT =
            "SELECT payment_id, sale_id, method, amount, payment_date FROM payment";

    @Override
    public Optional<Payment> findById(Integer id) {
        String sql = BASE_SELECT + " WHERE payment_id = ?";
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
            throw new RepositoryException("Failed to find payment by id: " + id, e);
        }
    }

    /** Convenience lookup — payments made against one sale (a sale can be split across methods). */
    public List<Payment> findBySaleId(Integer saleId) {
        String sql = BASE_SELECT + " WHERE sale_id = ? ORDER BY payment_id";
        List<Payment> payments = new ArrayList<>();

        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapRow(rs));
                }
            }
            return payments;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find payments for sale_id: " + saleId, e);
        }
    }

    @Override
    public List<Payment> findAll() {
        String sql = BASE_SELECT + " ORDER BY payment_id";
        List<Payment> payments = new ArrayList<>();

        try (Connection conn = HikariDatabaseConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                payments.add(mapRow(rs));
            }
            return payments;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all payments", e);
        }
    }

    @Override
    public Integer insert(Payment payment) {
        String sql = "INSERT INTO payment (sale_id, method, amount, payment_date) VALUES (?, ?, ?, ?)";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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
                throw new RepositoryException("Insert succeeded but no generated key was returned for payment");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert payment: " + payment, e);
        }
    }

    @Override
    public boolean update(Payment payment) {
        String sql = "UPDATE payment SET sale_id = ?, method = ?, amount = ?, payment_date = ? "
                + "WHERE payment_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, payment.getSaleId());
            ps.setString(2, payment.getMethod());
            ps.setBigDecimal(3, payment.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(payment.getPaymentDate()));
            ps.setInt(5, payment.getPaymentId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update payment: " + payment, e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM payment WHERE payment_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to delete payment with id: " + id, e);
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
