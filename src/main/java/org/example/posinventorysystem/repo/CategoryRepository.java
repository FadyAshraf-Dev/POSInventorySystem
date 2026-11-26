package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Category;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class CategoryRepository implements GenericRepository<Category, Integer> {

    @Override
    public Optional<Category> findById(Integer id) {
        return JdbcExecutor.withConnection("Failed to find category by id: " + id, conn -> findById(conn, id));
    }

    @Override
    public List<Category> findAll() {
        return JdbcExecutor.withConnection("Failed to fetch all categories", this::findAll);
    }

    @Override
    public Integer insert(Category category) {
        return JdbcExecutor.withConnection("Failed to insert category: " + category, conn -> insert(conn, category));
    }

    @Override
    public boolean update(Category category) {
        return JdbcExecutor.withConnection("Failed to update category: " + category, conn -> update(conn, category));
    }

    @Override
    public boolean softDelete(Integer id) {
        return JdbcExecutor.withConnection("Failed to soft-delete category with id: " + id, conn -> softDelete(conn, id));
    }

    @Override
    public boolean restore(Integer id) {
        return JdbcExecutor.withConnection("Failed to restore category with id: " + id, conn -> restore(conn, id));
    }

    public Optional<Category> findById(Connection conn, Integer id) throws SQLException {
        String sql = "SELECT category_id, category_name FROM category WHERE deleted_at IS NULL AND category_id = ?";
        return JdbcExecutor.queryOne(conn, sql, ps -> { ps.setInt(1, id); }, this::mapRow);
    }

    public List<Category> findAll(Connection conn) throws SQLException {
        String sql = "SELECT category_id, category_name FROM category WHERE deleted_at IS NULL ORDER BY category_id";
        return JdbcExecutor.query(conn, sql, ps -> { }, this::mapRow);
    }

    public Integer insert(Connection conn, Category category) throws SQLException {
        String sql = "INSERT INTO category (category_name) VALUES (?)";
        int generatedId = JdbcExecutor.executeInsert(conn, sql, ps -> { ps.setString(1, category.getCategoryName()); });
        category.setCategoryId(generatedId);
        return generatedId;
    }

    public boolean update(Connection conn, Category category) throws SQLException {
        String sql = "UPDATE category SET category_name = ? WHERE category_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setString(1, category.getCategoryName());
            ps.setInt(2, category.getCategoryId()); }) > 0;
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE category SET deleted_at = CURRENT_TIMESTAMP(6) WHERE category_id = ? AND deleted_at IS NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setInt(1, id); }) > 0;
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE category SET deleted_at = NULL WHERE category_id = ? AND deleted_at IS NOT NULL";
        return JdbcExecutor.executeUpdate(conn, sql, ps -> { ps.setInt(1, id); }) > 0;
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setCategoryId(rs.getInt("category_id"));
        category.setCategoryName(rs.getString("category_name"));
        return category;
    }

    /** Case-insensitive search across category_name for non-deleted records. */
    public List<Category> search(String term) {
        return JdbcExecutor.withConnection("Failed to search categories", conn -> search(conn, term));
    }

    public List<Category> search(Connection conn, String term) throws SQLException {
        if (term == null || term.isBlank()) return List.of();
        String sql = "SELECT category_id, category_name FROM category WHERE deleted_at IS NULL AND LOWER(category_name) LIKE ? ORDER BY category_name, category_id";
        String pattern = "%" + term.trim().toLowerCase() + "%";
        return JdbcExecutor.query(conn, sql, ps -> ps.setString(1, pattern), this::mapRow);
    }

    /** Returns true if any product record references this category, including historical/soft-deleted records. */
    public boolean hasProducts(Integer id) {
        return JdbcExecutor.withConnection("Failed to check references for category id: " + id, conn -> hasProducts(conn, id));
    }

    public boolean hasProducts(Connection conn, Integer id) throws SQLException {
        String sql = "SELECT 1 FROM product WHERE category_id = ? LIMIT 1";
        return JdbcExecutor.exists(conn, sql, ps -> { ps.setInt(1, id); });
    }

}
