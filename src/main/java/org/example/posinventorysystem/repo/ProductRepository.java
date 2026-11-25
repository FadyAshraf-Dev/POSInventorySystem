package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductRepository implements GenericRepository<Product, Integer> {

    private static final String BASE_SELECT =
            "SELECT product_id, barcode, product_name, category_id, unit_id, cost_price, "
                    + "selling_price, quantity_in_stock, reorder_level FROM product";

    @Override
    public Optional<Product> findById(Integer id) {
        String sql = BASE_SELECT + " WHERE product_id = ?";
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
            throw new RepositoryException("Failed to find product by id: " + id, e);
        }
    }

    /** Convenience lookup used at the POS scan screen. */
    public Optional<Product> findByBarcode(String barcode) {
        String sql = BASE_SELECT + " WHERE barcode = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, barcode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find product by barcode: " + barcode, e);
        }
    }

    @Override
    public List<Product> findAll() {
        String sql = BASE_SELECT + " ORDER BY product_id";
        List<Product> products = new ArrayList<>();

        try (Connection conn = HikariDatabaseConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                products.add(mapRow(rs));
            }
            return products;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all products", e);
        }
    }

    @Override
    public Integer insert(Product product) {
        String sql = "INSERT INTO product (barcode, product_name, category_id, unit_id, cost_price, "
                + "selling_price, quantity_in_stock, reorder_level) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, product.getBarcode());
            ps.setString(2, product.getProductName());
            ps.setInt(3, product.getCategoryId());
            ps.setInt(4, product.getUnitId());
            ps.setBigDecimal(5, product.getCostPrice());
            ps.setBigDecimal(6, product.getSellingPrice());
            ps.setInt(7, product.getQuantityInStock());
            ps.setInt(8, product.getReorderLevel());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    product.setProductId(generatedId);
                    return generatedId;
                }
                throw new RepositoryException("Insert succeeded but no generated key was returned for product");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert product: " + product, e);
        }
    }

    @Override
    public boolean update(Product product) {
        String sql = "UPDATE product SET barcode = ?, product_name = ?, category_id = ?, unit_id = ?, "
                + "cost_price = ?, selling_price = ?, quantity_in_stock = ?, reorder_level = ? "
                + "WHERE product_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, product.getBarcode());
            ps.setString(2, product.getProductName());
            ps.setInt(3, product.getCategoryId());
            ps.setInt(4, product.getUnitId());
            ps.setBigDecimal(5, product.getCostPrice());
            ps.setBigDecimal(6, product.getSellingPrice());
            ps.setInt(7, product.getQuantityInStock());
            ps.setInt(8, product.getReorderLevel());
            ps.setInt(9, product.getProductId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update product: " + product, e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM product WHERE product_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to delete product with id: " + id, e);
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setProductId(rs.getInt("product_id"));
        product.setBarcode(rs.getString("barcode"));
        product.setProductName(rs.getString("product_name"));
        product.setCategoryId(rs.getInt("category_id"));
        product.setUnitId(rs.getInt("unit_id"));
        product.setCostPrice(rs.getBigDecimal("cost_price"));
        product.setSellingPrice(rs.getBigDecimal("selling_price"));
        product.setQuantityInStock(rs.getInt("quantity_in_stock"));
        product.setReorderLevel(rs.getInt("reorder_level"));
        return product;
    }
}
