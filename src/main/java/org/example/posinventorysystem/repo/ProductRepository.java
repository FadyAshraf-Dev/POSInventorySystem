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
                    + "selling_price, quantity_in_stock, reorder_level, is_active FROM product";

    @Override
    public Optional<Product> findById(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find product by id: " + id, e);
        }
    }

    public List<Product> findActive() {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findActive(conn);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch active products", e);
        }
    }

    public List<Product> findActive(Connection conn) throws SQLException {
        String sql = BASE_SELECT + " WHERE deleted_at IS NULL AND is_active = 1 ORDER BY product_id";
        List<Product> products = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) products.add(mapRow(rs));
        }
        return products;
    }

    public boolean setActive(int productId, boolean active) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return setActive(conn, productId, active);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to change active state for product " + productId, e);
        }
    }

    public boolean setActive(Connection conn, int productId, boolean active) throws SQLException {
        String sql = "UPDATE product SET is_active = ? WHERE product_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        }
    }

    public Optional<Product> findByBarcode(String barcode) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findByBarcode(conn, barcode);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find product by barcode: " + barcode, e);
        }
    }

    @Override
    public List<Product> findAll() {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all products", e);
        }
    }

    @Override
    public Integer insert(Product product) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return insert(conn, product);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert product: " + product, e);
        }
    }

    @Override
    public boolean update(Product product) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return update(conn, product);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update product: " + product, e);
        }
    }

    @Override
    public boolean softDelete(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return softDelete(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to soft-delete product with id: " + id, e);
        }
    }

    @Override
    public boolean restore(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return restore(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to restore product with id: " + id, e);
        }
    }

    /**
     * Atomically adjusts stock by delta (positive = stock in, negative = stock out)
     * without a read-then-write race: the database itself checks the resulting
     * quantity never goes negative, so two concurrent sales of the same product
     * can't silently overwrite each other's decrement (lost update).
     *
     * Returns false if the adjustment would take stock below zero (insufficient
     * stock) — in that case, zero rows are updated and nothing changes.
     *
     * No-connection form: opens its own connection, auto-commits. Only safe to
     * call on its own when NOT part of a larger multi-table operation (e.g. a
     * standalone manual stock correction). For a sale/purchase — which also
     * writes sale_item/movement rows — use the (Connection, ...) overload
     * inside TransactionManager.run(...) instead, so the stock change and the
     * related rows commit or roll back together.
     */
    public boolean adjustStock(int productId, int delta) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return adjustStock(conn, productId, delta);
        } catch (SQLException e) {
            throw new RepositoryException(
                    "Failed to adjust stock for product id: " + productId + " by delta: " + delta, e);
        }
    }

    public boolean adjustStock(Connection conn, int productId, int delta) throws SQLException {
        String sql = "UPDATE product SET quantity_in_stock = quantity_in_stock + ? "
                + "WHERE product_id = ? AND deleted_at IS NULL AND quantity_in_stock + ? >= 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, productId);
            ps.setInt(3, delta);
            // Zero rows updated means either the product doesn't exist, or the
            // adjustment would have taken stock below zero (insufficient stock).
            return ps.executeUpdate() > 0;
        }
    }

    public Optional<Product> findById(Connection conn, Integer id) throws SQLException {
        String sql = BASE_SELECT + " WHERE product_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Product> findByBarcode(Connection conn, String barcode) throws SQLException {
        String sql = BASE_SELECT + " WHERE barcode = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barcode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Product> findAll(Connection conn) throws SQLException {
        String sql = BASE_SELECT + " WHERE deleted_at IS NULL ORDER BY product_id";
        List<Product> products = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                products.add(mapRow(rs));
            }
        }
        return products;
    }

    public Integer insert(Connection conn, Product product) throws SQLException {
        String sql = "INSERT INTO product (barcode, product_name, category_id, unit_id, cost_price, "
                + "selling_price, quantity_in_stock, reorder_level, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getBarcode());
            ps.setString(2, product.getProductName());
            ps.setInt(3, product.getCategoryId());
            ps.setInt(4, product.getUnitId());
            ps.setBigDecimal(5, product.getCostPrice());
            ps.setBigDecimal(6, product.getSellingPrice());
            ps.setInt(7, product.getQuantityInStock());
            ps.setInt(8, product.getReorderLevel());
            ps.setBoolean(9, product.isActive());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    product.setProductId(generatedId);
                    return generatedId;
                }
                throw new SQLException("Insert succeeded but no generated key was returned for product");
            }
        }
    }

    /**
     * Updates product fields including quantity_in_stock as a full overwrite.
     * Do NOT use this for ordinary stock changes from sales/purchases/returns —
     * it has no concurrency protection against two simultaneous stock changes
     * (lost update). Use adjustStock(conn, productId, delta) for that; reserve
     * this method for edits to non-stock fields (name, price, category, etc.)
     * or deliberate manual stock corrections where overwriting is intended.
     */
    public boolean update(Connection conn, Product product) throws SQLException {
        String sql = "UPDATE product SET barcode = ?, product_name = ?, category_id = ?, unit_id = ?, "
                + "cost_price = ?, selling_price = ?, quantity_in_stock = ?, reorder_level = ?, is_active = ? "
                + "WHERE product_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getBarcode());
            ps.setString(2, product.getProductName());
            ps.setInt(3, product.getCategoryId());
            ps.setInt(4, product.getUnitId());
            ps.setBigDecimal(5, product.getCostPrice());
            ps.setBigDecimal(6, product.getSellingPrice());
            ps.setInt(7, product.getQuantityInStock());
            ps.setInt(8, product.getReorderLevel());
            ps.setBoolean(9, product.isActive());
            ps.setInt(10, product.getProductId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE product SET deleted_at = CURRENT_TIMESTAMP(6) WHERE product_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); return ps.executeUpdate() > 0; }
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE product SET deleted_at = NULL WHERE product_id = ? AND deleted_at IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); return ps.executeUpdate() > 0; }
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
        product.setActive(rs.getBoolean("is_active"));
        return product;
    }
}
