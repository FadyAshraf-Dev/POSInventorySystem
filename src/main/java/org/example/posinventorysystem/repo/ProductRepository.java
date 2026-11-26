package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Product;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class ProductRepository implements GenericRepository<Product, Integer> {

    private static final String BASE_SELECT =
            "SELECT product_id, barcode, product_name, category_id, unit_id, cost_price, "
                    + "selling_price, quantity_in_stock, reorder_level, is_active FROM product";

    @Override
    public Optional<Product> findById(Integer id) {
        return JdbcExecutor.withConnection("Failed to find product by id: " + id, conn -> findById(conn, id));
    }

    public List<Product> findActive() {
        return JdbcExecutor.withConnection("Failed to fetch active products", this::findActive);
    }

    public List<Product> findActive(Connection conn) throws SQLException {
        String sql = BASE_SELECT + " WHERE deleted_at IS NULL AND is_active = 1 ORDER BY product_id";
        return JdbcExecutor.query(conn, sql, ps -> { }, this::mapRow);
    }

    public boolean setActive(int productId, boolean active) {
        return JdbcExecutor.withConnection("Failed to change active state for product " + productId, conn -> setActive(conn, productId, active));
    }

    public boolean setActive(Connection conn, int productId, boolean active) throws SQLException {
        String sql = "UPDATE product SET is_active = ? WHERE product_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setBoolean(1, active);
            ps.setInt(2, productId); }) > 0;
    }

    public Optional<Product> findByBarcode(String barcode) {
        return JdbcExecutor.withConnection("Failed to find product by barcode: " + barcode, conn -> findByBarcode(conn, barcode));
    }

    @Override
    public List<Product> findAll() {
        return JdbcExecutor.withConnection("Failed to fetch all products", this::findAll);
    }

    @Override
    public Integer insert(Product product) {
        return JdbcExecutor.withConnection("Failed to insert product: " + product, conn -> insert(conn, product));
    }

    @Override
    public boolean update(Product product) {
        return JdbcExecutor.withConnection("Failed to update product: " + product, conn -> update(conn, product));
    }

    @Override
    public boolean softDelete(Integer id) {
        return JdbcExecutor.withConnection("Failed to soft-delete product with id: " + id, conn -> softDelete(conn, id));
    }

    @Override
    public boolean restore(Integer id) {
        return JdbcExecutor.withConnection("Failed to restore product with id: " + id, conn -> restore(conn, id));
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
        return JdbcExecutor.withConnection("Failed to adjust stock for product id: " + productId + " by delta: " + delta, conn -> adjustStock(conn, productId, delta));
    }

    public boolean adjustStock(Connection conn, int productId, int delta) throws SQLException {
        String sql = "UPDATE product SET quantity_in_stock = quantity_in_stock + ? "
                + "WHERE product_id = ? AND deleted_at IS NULL AND quantity_in_stock + ? >= 0";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setInt(1, delta);
            ps.setInt(2, productId);
            ps.setInt(3, delta);
        }) > 0;
    }

    public Optional<Product> findById(Connection conn, Integer id) throws SQLException {
        String sql = BASE_SELECT + " WHERE product_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.queryOne(conn, sql, ps -> { ps.setInt(1, id); }, this::mapRow);
    }

    public Optional<Product> findByBarcode(Connection conn, String barcode) throws SQLException {
        String sql = BASE_SELECT + " WHERE barcode = ? AND deleted_at IS NULL";
        return JdbcExecutor.queryOne(conn, sql, ps -> { ps.setString(1, barcode); }, this::mapRow);
    }

    public List<Product> findAll(Connection conn) throws SQLException {
        String sql = BASE_SELECT + " WHERE deleted_at IS NULL ORDER BY product_id";
        return JdbcExecutor.query(conn, sql, ps -> { }, this::mapRow);
    }

    public Integer insert(Connection conn, Product product) throws SQLException {
        String sql = "INSERT INTO product (barcode, product_name, category_id, unit_id, cost_price, "
                + "selling_price, quantity_in_stock, reorder_level, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int generatedId = JdbcExecutor.executeInsert(conn, sql, ps -> { ps.setString(1, product.getBarcode());
            ps.setString(2, product.getProductName());
            ps.setInt(3, product.getCategoryId());
            ps.setInt(4, product.getUnitId());
            ps.setBigDecimal(5, product.getCostPrice());
            ps.setBigDecimal(6, product.getSellingPrice());
            ps.setInt(7, product.getQuantityInStock());
            ps.setInt(8, product.getReorderLevel());
            ps.setBoolean(9, product.isActive()); });
        product.setProductId(generatedId);
        return generatedId;
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
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setString(1, product.getBarcode());
            ps.setString(2, product.getProductName());
            ps.setInt(3, product.getCategoryId());
            ps.setInt(4, product.getUnitId());
            ps.setBigDecimal(5, product.getCostPrice());
            ps.setBigDecimal(6, product.getSellingPrice());
            ps.setInt(7, product.getQuantityInStock());
            ps.setInt(8, product.getReorderLevel());
            ps.setBoolean(9, product.isActive());
            ps.setInt(10, product.getProductId()); }) > 0;
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE product SET deleted_at = CURRENT_TIMESTAMP(6) WHERE product_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setInt(1, id); }) > 0;
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE product SET deleted_at = NULL WHERE product_id = ? AND deleted_at IS NOT NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setInt(1, id); }) > 0;
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

    /** Searches non-deleted products by name or barcode, including inactive products for inventory-management use. */
    public List<Product> search(String term) {
        return JdbcExecutor.withConnection("Failed to search products", conn -> search(conn, term));
    }

    public List<Product> search(Connection conn, String term) throws SQLException {
        if (term == null || term.isBlank()) return List.of();
        String sql = BASE_SELECT + " WHERE deleted_at IS NULL "
                + "AND (LOWER(product_name) LIKE ? OR LOWER(COALESCE(barcode, '')) LIKE ?) "
                + "ORDER BY product_name, product_id";
        String pattern = "%" + term.trim().toLowerCase() + "%";
        return JdbcExecutor.query(conn, sql, ps -> {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
        }, this::mapRow);
    }

    /** Active, non-deleted products at or below their configured reorder level. */
    public List<Product> findLowStock() {
        return JdbcExecutor.withConnection("Failed to fetch low-stock products", this::findLowStock);
    }

    public List<Product> findLowStock(Connection conn) throws SQLException {
        String sql = BASE_SELECT + " WHERE deleted_at IS NULL AND is_active = 1 "
                + "AND quantity_in_stock <= reorder_level ORDER BY quantity_in_stock, product_name";
        return JdbcExecutor.query(conn, sql, ps -> { }, this::mapRow);
    }

    /** Checks barcode uniqueness across all rows (including soft-deleted rows), matching the global UNIQUE constraint. */
    public boolean barcodeExists(String barcode, Integer excludeId) {
        return JdbcExecutor.withConnection("Failed to check product barcode", conn -> barcodeExists(conn, barcode, excludeId));
    }

    public boolean barcodeExists(Connection conn, String barcode, Integer excludeId) throws SQLException {
        if (barcode == null || barcode.isBlank()) return false;
        String sql = excludeId == null
                ? "SELECT 1 FROM product WHERE barcode = ? LIMIT 1"
                : "SELECT 1 FROM product WHERE barcode = ? AND product_id <> ? LIMIT 1";
        return JdbcExecutor.exists(conn, sql, ps -> { ps.setString(1, barcode.trim());
            if (excludeId != null) ps.setInt(2, excludeId); });
    }

}
