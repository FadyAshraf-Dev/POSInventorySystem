package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;
import org.example.posinventorysystem.model.Movement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MovementRepository implements InsertOnlyRepository<Movement, Integer> {

    private static final String BASE_SELECT =
            "SELECT movement_id, product_id, movement_type, quantity_change, reference_id, "
                    + "reason, user_id, movement_date FROM movement";

    @Override
    public Optional<Movement> findById(Integer id) {
        return JdbcExecutor.withConnection("Failed to find movement by id: " + id, conn -> findById(conn, id));
    }

    public List<Movement> findByProductId(Integer productId) {
        return JdbcExecutor.withConnection("Failed to find movements for product_id: " + productId, conn -> findByProductId(conn, productId));
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
        return JdbcExecutor.withConnection("Failed to insert movement: " + movement, conn -> insert(conn, movement));
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
        String sql = "INSERT INTO movement (product_id, movement_type, quantity_change, reference_id, reason, user_id, movement_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, movement.getProductId());
            ps.setString(2, movement.getMovementType());
            ps.setInt(3, movement.getQuantityChange());
            if (movement.getReferenceId() != null) {
                ps.setInt(4, movement.getReferenceId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setString(5, movement.getReason());
            ps.setInt(6, movement.getUserId());
            ps.setTimestamp(7, Timestamp.valueOf(movement.getMovementDate()));
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



    private Movement mapRow(ResultSet rs) throws SQLException {
        Movement movement = new Movement();
        movement.setMovementId(rs.getInt("movement_id"));
        movement.setProductId(rs.getInt("product_id"));
        movement.setMovementType(rs.getString("movement_type"));
        movement.setQuantityChange(rs.getInt("quantity_change"));
        int referenceId = rs.getInt("reference_id");
        movement.setReferenceId(rs.wasNull() ? null : referenceId);
        movement.setReason(rs.getString("reason"));
        movement.setUserId(rs.getInt("user_id"));
        movement.setMovementDate(rs.getTimestamp("movement_date").toLocalDateTime());
        return movement;
    }

    /** Returns ledger movements within the inclusive date/time range. */
    public List<Movement> findByDateRange(java.time.LocalDateTime from, java.time.LocalDateTime to) {
        return JdbcExecutor.withConnection("Failed to fetch movements for date range", conn -> findByDateRange(conn, from, to));
    }

    public List<Movement> findByDateRange(Connection conn, java.time.LocalDateTime from,
                                           java.time.LocalDateTime to) throws SQLException {
        if (from == null || to == null || from.isAfter(to))
            throw new IllegalArgumentException("Date range must have non-null endpoints and from <= to");
        String sql = BASE_SELECT + " WHERE movement_date >= ? AND movement_date <= ? ORDER BY movement_date, movement_id";
        List<Movement> movements = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) movements.add(mapRow(rs));
            }
        }
        return movements;
    }

}
