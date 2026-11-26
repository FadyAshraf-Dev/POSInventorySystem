package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.SaleItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SaleItemRepository implements GenericRepository<SaleItem, Integer> {

    private static final String BASE_SELECT =
            "SELECT sale_item_id, sale_id, product_id, quantity, unit_price, discount, line_total FROM sale_item";

    @Override
    public Optional<SaleItem> findById(Integer id) {
        return JdbcExecutor.withConnection("Failed to find sale item by id: " + id, conn -> findById(conn, id));
    }

    public List<SaleItem> findBySaleId(Integer saleId) {
        return JdbcExecutor.withConnection("Failed to find sale items for sale_id: " + saleId, conn -> findBySaleId(conn, saleId));
    }

    @Override
    public List<SaleItem> findAll() {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all sale items", e);
        }
    }

    @Override
    public Integer insert(SaleItem saleItem) {
        return JdbcExecutor.withConnection("Failed to insert sale item: " + saleItem, conn -> insert(conn, saleItem));
    }

    @Override
    public boolean update(SaleItem saleItem) {
        return JdbcExecutor.withConnection("Failed to update sale item: " + saleItem, conn -> update(conn, saleItem));
    }

    
    @Override
    public boolean softDelete(Integer id) {
        return JdbcExecutor.withConnection("Failed to soft-delete saleitem with id: " + id, conn -> softDelete(conn, id));
    }

    @Override
    public boolean restore(Integer id) {
        return JdbcExecutor.withConnection("Failed to restore saleitem with id: " + id, conn -> restore(conn, id));
    }

    public Optional<SaleItem> findById(Connection conn, Integer id) throws SQLException {
        String sql = BASE_SELECT + " WHERE sale_item_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<SaleItem> findBySaleId(Connection conn, Integer saleId) throws SQLException {
        String sql = BASE_SELECT + " WHERE sale_id = ? AND deleted_at IS NULL ORDER BY sale_item_id";
        List<SaleItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        }
        return items;
    }

    public List<SaleItem> findAll(Connection conn) throws SQLException {
        String sql = BASE_SELECT + " WHERE deleted_at IS NULL ORDER BY sale_item_id";
        List<SaleItem> items = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                items.add(mapRow(rs));
            }
        }
        return items;
    }

    public Integer insert(Connection conn, SaleItem saleItem) throws SQLException {
        String sql = "INSERT INTO sale_item (sale_id, product_id, quantity, unit_price, discount, line_total) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, saleItem.getSaleId());
            ps.setInt(2, saleItem.getProductId());
            ps.setInt(3, saleItem.getQuantity());
            ps.setBigDecimal(4, saleItem.getUnitPrice());
            ps.setBigDecimal(5, saleItem.getDiscount());
            ps.setBigDecimal(6, saleItem.getLineTotal());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    saleItem.setSaleItemId(generatedId);
                    return generatedId;
                }
                throw new SQLException("Insert succeeded but no generated key was returned for sale item");
            }
        }
    }

    public boolean update(Connection conn, SaleItem saleItem) throws SQLException {
        String sql = "UPDATE sale_item SET sale_id = ?, product_id = ?, quantity = ?, unit_price = ?, "
                + "discount = ?, line_total = ? WHERE sale_item_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleItem.getSaleId());
            ps.setInt(2, saleItem.getProductId());
            ps.setInt(3, saleItem.getQuantity());
            ps.setBigDecimal(4, saleItem.getUnitPrice());
            ps.setBigDecimal(5, saleItem.getDiscount());
            ps.setBigDecimal(6, saleItem.getLineTotal());
            ps.setInt(7, saleItem.getSaleItemId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE sale_item SET deleted_at = CURRENT_TIMESTAMP(6) WHERE sale_item_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); return ps.executeUpdate() > 0; }
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE sale_item SET deleted_at = NULL WHERE sale_item_id = ? AND deleted_at IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); return ps.executeUpdate() > 0; }
    }

    private SaleItem mapRow(ResultSet rs) throws SQLException {
        SaleItem item = new SaleItem();
        item.setSaleItemId(rs.getInt("sale_item_id"));
        item.setSaleId(rs.getInt("sale_id"));
        item.setProductId(rs.getInt("product_id"));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitPrice(rs.getBigDecimal("unit_price"));
        item.setDiscount(rs.getBigDecimal("discount"));
        item.setLineTotal(rs.getBigDecimal("line_total"));
        return item;
    }
}
