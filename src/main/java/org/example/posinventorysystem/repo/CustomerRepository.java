package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Customer;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class CustomerRepository implements GenericRepository<Customer, Integer> {

    @Override
    public Optional<Customer> findById(Integer id) {
        return JdbcExecutor.withConnection("Failed to find customer by id: " + id, conn -> findById(conn, id));
    }

    @Override
    public List<Customer> findAll() {
        return JdbcExecutor.withConnection("Failed to fetch all customers", this::findAll);
    }

    @Override
    public Integer insert(Customer customer) {
        return JdbcExecutor.withConnection("Failed to insert customer: " + customer, conn -> insert(conn, customer));
    }

    @Override
    public boolean update(Customer customer) {
        return JdbcExecutor.withConnection("Failed to update customer: " + customer, conn -> update(conn, customer));
    }

    @Override
    public boolean softDelete(Integer id) {
        return JdbcExecutor.withConnection("Failed to soft-delete customer with id: " + id, conn -> softDelete(conn, id));
    }

    @Override
    public boolean restore(Integer id) {
        return JdbcExecutor.withConnection("Failed to restore customer with id: " + id, conn -> restore(conn, id));
    }

    public Optional<Customer> findById(Connection conn, Integer id) throws SQLException {
        String sql = "SELECT customer_id, customer_name, phone, address FROM customer WHERE deleted_at IS NULL AND customer_id = ?";
        return JdbcExecutor.queryOne(conn, sql, ps -> { ps.setInt(1, id); }, this::mapRow);
    }

    public List<Customer> findAll(Connection conn) throws SQLException {
        String sql = "SELECT customer_id, customer_name, phone, address FROM customer WHERE deleted_at IS NULL ORDER BY customer_id";
        return JdbcExecutor.query(conn, sql, ps -> { }, this::mapRow);
    }

    public Integer insert(Connection conn, Customer customer) throws SQLException {
        String sql = "INSERT INTO customer (customer_name, phone, address) VALUES (?, ?, ?)";
        int generatedId = JdbcExecutor.executeInsert(conn, sql, ps -> { ps.setString(1, customer.getCustomerName());
            ps.setString(2, customer.getPhone());
            ps.setString(3, customer.getAddress()); });
        customer.setCustomerId(generatedId);
        return generatedId;
    }

    public boolean update(Connection conn, Customer customer) throws SQLException {
        String sql = "UPDATE customer SET customer_name = ?, phone = ?, address = ? WHERE customer_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setString(1, customer.getCustomerName());
            ps.setString(2, customer.getPhone());
            ps.setString(3, customer.getAddress());
            ps.setInt(4, customer.getCustomerId()); }) > 0;
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE customer SET deleted_at = CURRENT_TIMESTAMP(6) WHERE customer_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setInt(1, id); }) > 0;
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE customer SET deleted_at = NULL WHERE customer_id = ? AND deleted_at IS NOT NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setInt(1, id); }) > 0;
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer customer = new Customer();
        customer.setCustomerId(rs.getInt("customer_id"));
        customer.setCustomerName(rs.getString("customer_name"));
        customer.setPhone(rs.getString("phone"));
        customer.setAddress(rs.getString("address"));
        return customer;
    }

    /** Case-insensitive search across customer name and phone for non-deleted records. */
    public List<Customer> search(String term) {
        return JdbcExecutor.withConnection("Failed to search customers", conn -> search(conn, term));
    }

    public List<Customer> search(Connection conn, String term) throws SQLException {
        if (term == null || term.isBlank()) return List.of();
        String sql = "SELECT customer_id, customer_name, phone, address FROM customer WHERE deleted_at IS NULL AND (LOWER(COALESCE(customer_name, '')) LIKE ? OR LOWER(COALESCE(phone, '')) LIKE ? OR LOWER(COALESCE(address, '')) LIKE ?) ORDER BY customer_name, customer_id";
        String pattern = "%" + term.trim().toLowerCase() + "%";
        return JdbcExecutor.query(conn, sql, ps -> {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
        }, this::mapRow);
    }

    /** Returns true if any sale record references this customer, including historical/soft-deleted records. */
    public boolean hasSales(Integer id) {
        return JdbcExecutor.withConnection("Failed to check references for customer id: " + id, conn -> hasSales(conn, id));
    }

    public boolean hasSales(Connection conn, Integer id) throws SQLException {
        String sql = "SELECT 1 FROM sale WHERE customer_id = ? LIMIT 1";
        return JdbcExecutor.exists(conn, sql, ps -> { ps.setInt(1, id); });
    }

}
