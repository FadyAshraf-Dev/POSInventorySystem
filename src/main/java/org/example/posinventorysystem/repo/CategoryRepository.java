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
        String sql = "SELECT category_id, category_name FROM category WHERE category_id = ?";
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
            throw new RepositoryException("Failed to find category by id: " + id, e);
        }
    }

    @Override
    public List<Category> findAll() {
        String sql = "SELECT category_id, category_name FROM category ORDER BY category_id";
        List<Category> categories = new ArrayList<>();

        try (Connection conn = HikariDatabaseConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                categories.add(mapRow(rs));
            }
            return categories;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all categories", e);
        }
    }

    @Override
    public Integer insert(Category category) {
        String sql = "INSERT INTO category (category_name) VALUES (?)";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, category.getCategoryName());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    category.setCategoryId(generatedId);
                    return generatedId;
                }
                throw new RepositoryException("Insert succeeded but no generated key was returned for category");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert category: " + category, e);
        }
    }

    @Override
    public boolean update(Category category) {
        String sql = "UPDATE category SET category_name = ? WHERE category_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, category.getCategoryName());
            ps.setInt(2, category.getCategoryId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update category: " + category, e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM category WHERE category_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to delete category with id: " + id, e);
        }
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setCategoryId(rs.getInt("category_id"));
        category.setCategoryName(rs.getString("category_name"));
        return category;
    }
}
