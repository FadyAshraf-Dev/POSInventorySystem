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
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find movement by id: " + id, e);
        }
    }

    public List<Movement> findByProductId(Integer productId) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findByProductId(conn, productId);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find movements for product_id: " + productId, e);
        }
    }

    @Override
    public List<Movement> findAll() {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to fetch all movements", e);
        }
    }

    @Override
    public Integer insert(Movement movement) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return insert(conn, movement);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to insert movement: " + movement, e);
        }
    }

    /**
     * NOTE (review item 4): movement should become insert-only — it's a
     * stock ledger, so historical entries shouldn't be editable. Left in
     * place here since splitting GenericRepository into read/write
     * interfaces is a separate, larger review item.
     */
    @Override
    public boolean update(Movement movement) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return update(conn, movement);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update movement: " + movement, e);
        }
    }

    /**
     * NOTE (review item 4): movement should be insert-only — no delete.
     * Left in place here since that's a separate review item.
     */
    @Override
    public boolean deleteById(Integer id) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            return deleteById(conn, id);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to delete movement with id: " + id, e);
        }
    }

    public Optional<Movement> findById(Connection conn, Integer id) throws SQLException {
        String sql = BASE_SELECT + " WHERE movement_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Movement> findByProductId(Connection conn, Integer productId) throws SQLException {
        String sql = BASE_SELECT + " WHERE product_id = ? ORDER BY movement_date, movement_id";
        List<Movement> movements = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movements.add(mapRow(rs));
                }
            }
        }
        return movements;
    }

    public List<Movement> findAll(Connection conn) throws SQLException {
        String sql = BASE_SELECT + " ORDER BY movement_id";
        List<Movement> movements = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                movements.add(mapRow(rs));
            }
        }
        return movements;
    }

    public Integer insert(Connection conn, Movement movement) throws SQLException {
        String sql = "INSERT INTO movement (product_id, movement_type, quantity_change, reference_id, "
                + "user_id, movement_date) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
                throw new SQLException("Insert succeeded but no generated key was returned for movement");
            }
        }
    }

    public boolean update(Connection conn, Movement movement) throws SQLException {
        String sql = "UPDATE movement SET product_id = ?, movement_type = ?, quantity_change = ?, "
                + "reference_id = ?, user_id = ?, movement_date = ? WHERE movement_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
        }
    }

    public boolean deleteById(Connection conn, Integer id) throws SQLException {
        String sql = "DELETE FROM movement WHERE movement_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
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
