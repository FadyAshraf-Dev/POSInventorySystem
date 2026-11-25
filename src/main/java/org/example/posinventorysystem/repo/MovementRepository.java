package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Movement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MovementRepository implements GenericRepository<Movement, Integer> {

    private static final String BASE_SELECT =
            "SELECT movement_id, product_id, movement_type, quantity_change, reference_id, "
                    + "user_id, movement_date FROM movement";

    @Override
    public Optional<Movement> findById(Integer id) {
        String sql = BASE_SELECT + " WHERE movement_id = ?";
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
            throw new RepositoryException("Failed to find movement by id: " + id, e);
        }
    }

    /** Convenience lookup — the full stock ledger for one product. */
    public List<Movement> findByProductId(Integer productId) {
        String sql = BASE_SELECT + " WHERE product_id = ? ORDER BY movement_date, movement_id";
        List<Movement> movements = new ArrayList<>();

        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movements.add(mapRow(rs));
                }
            }
            return movements;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find movements for product_id: " + productId, e);
        }
    }

    @Override
    public List<Movement> findAll() {
        String sql = BASE_SELECT + " ORDER BY movement_id";
        List<Movement> movements = new ArrayList<>();

        try (Connection conn = HikariDatabaseConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                movements.add(mapRow(rs));
            }
            return movements;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all movements", e);
        }
    }

    @Override
    public Integer insert(Movement movement) {
        String sql = "INSERT INTO movement (product_id, movement_type, quantity_change, reference_id, "
                + "user_id, movement_date) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, movement.getProductId());
            ps.setString(2, movement.getMovementType());
            ps.setInt(3, movement.getQuantityChange());
            if (movement.getReferenceId() != null) {
                ps.setInt(4, movement.getReferenceId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setInt(5, movement.getUserId());
            ps.setTimestamp(6, Timestamp.valueOf(movement.getMovementDate()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    movement.setMovementId(generatedId);
                    return generatedId;
                }
                throw new RepositoryException("Insert succeeded but no generated key was returned for movement");
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert movement: " + movement, e);
        }
    }

    @Override
    public boolean update(Movement movement) {
        String sql = "UPDATE movement SET product_id = ?, movement_type = ?, quantity_change = ?, "
                + "reference_id = ?, user_id = ?, movement_date = ? WHERE movement_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, movement.getProductId());
            ps.setString(2, movement.getMovementType());
            ps.setInt(3, movement.getQuantityChange());
            if (movement.getReferenceId() != null) {
                ps.setInt(4, movement.getReferenceId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setInt(5, movement.getUserId());
            ps.setTimestamp(6, Timestamp.valueOf(movement.getMovementDate()));
            ps.setInt(7, movement.getMovementId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update movement: " + movement, e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM movement WHERE movement_id = ?";
        try (Connection conn = HikariDatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to delete movement with id: " + id, e);
        }
    }

    private Movement mapRow(ResultSet rs) throws SQLException {
        Movement movement = new Movement();
        movement.setMovementId(rs.getInt("movement_id"));
        movement.setProductId(rs.getInt("product_id"));
        movement.setMovementType(rs.getString("movement_type"));
        movement.setQuantityChange(rs.getInt("quantity_change"));
        int referenceId = rs.getInt("reference_id");
        movement.setReferenceId(rs.wasNull() ? null : referenceId);
        movement.setUserId(rs.getInt("user_id"));
        movement.setMovementDate(rs.getTimestamp("movement_date").toLocalDateTime());
        return movement;
    }
}
