package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerRepository implements GenericRepository<Customer, Integer> {

    @Override
    public Optional<Customer> findById(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find customer by id: " + id, e);
        }
    }

    @Override
    public List<Customer> findAll() {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all customers", e);
        }
    }

    @Override
    public Integer insert(Customer customer) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return insert(conn, customer);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert customer: " + customer, e);
        }
    }

    @Override
    public boolean update(Customer customer) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return update(conn, customer);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update customer: " + customer, e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return deleteById(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to delete customer with id: " + id, e);
        }
    }

    public Optional<Customer> findById(Connection conn, Integer id) throws SQLException {
        String sql = "SELECT customer_id, customer_name, phone FROM customer WHERE customer_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Customer> findAll(Connection conn) throws SQLException {
        String sql = "SELECT customer_id, customer_name, phone FROM customer ORDER BY customer_id";
        List<Customer> customers = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                customers.add(mapRow(rs));
            }
        }
        return customers;
    }

    public Integer insert(Connection conn, Customer customer) throws SQLException {
        String sql = "INSERT INTO customer (customer_name, phone) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customer.getCustomerName());
            ps.setString(2, customer.getPhone());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    customer.setCustomerId(generatedId);
                    return generatedId;
                }
                throw new SQLException("Insert succeeded but no generated key was returned for customer");
            }
        }
    }

    public boolean update(Connection conn, Customer customer) throws SQLException {
        String sql = "UPDATE customer SET customer_name = ?, phone = ? WHERE customer_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getCustomerName());
            ps.setString(2, customer.getPhone());
            ps.setInt(3, customer.getCustomerId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteById(Connection conn, Integer id) throws SQLException {
        String sql = "DELETE FROM customer WHERE customer_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer customer = new Customer();
        customer.setCustomerId(rs.getInt("customer_id"));
        customer.setCustomerName(rs.getString("customer_name"));
        customer.setPhone(rs.getString("phone"));
        return customer;
    }
}
