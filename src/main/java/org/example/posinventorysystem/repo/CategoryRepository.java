package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryRepository implements GenericRepository<Category, Integer> {

    @Override
    public Optional<Category> findById(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find category by id: " + id, e);
        }
    }

    @Override
    public List<Category> findAll() {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all categories", e);
        }
    }

    @Override
    public Integer insert(Category category) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return insert(conn, category);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert category: " + category, e);
        }
    }

    @Override
    public boolean update(Category category) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return update(conn, category);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update category: " + category, e);
        }
    }

    @Override
    public boolean softDelete(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return softDelete(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to soft-delete category with id: " + id, e);
        }
    }

    @Override
    public boolean restore(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return restore(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to restore category with id: " + id, e);
        }
    }

    public Optional<Category> findById(Connection conn, Integer id) throws SQLException {
        String sql = "SELECT category_id, category_name FROM category WHERE deleted_at IS NULL AND category_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Category> findAll(Connection conn) throws SQLException {
        String sql = "SELECT category_id, category_name FROM category WHERE deleted_at IS NULL ORDER BY category_id";
        List<Category> categories = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                categories.add(mapRow(rs));
            }
        }
        return categories;
    }

    public Integer insert(Connection conn, Category category) throws SQLException {
        String sql = "INSERT INTO category (category_name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, category.getCategoryName());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    category.setCategoryId(generatedId);
                    return generatedId;
                }
                throw new SQLException("Insert succeeded but no generated key was returned for category");
            }
        }
    }

    public boolean update(Connection conn, Category category) throws SQLException {
        String sql = "UPDATE category SET category_name = ? WHERE category_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.getCategoryName());
            ps.setInt(2, category.getCategoryId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean softDelete(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE category SET deleted_at = CURRENT_TIMESTAMP(6) WHERE category_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); return ps.executeUpdate() > 0; }
    }

    public boolean restore(Connection conn, Integer id) throws SQLException {
        String sql = "UPDATE category SET deleted_at = NULL WHERE category_id = ? AND deleted_at IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, id); return ps.executeUpdate() > 0; }
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setCategoryId(rs.getInt("category_id"));
        category.setCategoryName(rs.getString("category_name"));
        return category;
    }
}
