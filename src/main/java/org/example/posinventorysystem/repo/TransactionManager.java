package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.HikariDatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Runs a block of repository calls as a single atomic transaction on one
 * borrowed connection: commits if the block completes normally, rolls back
 * if it throws anything.
 *
 * Usage (from the Business/service layer):
 *
 *   TransactionManager.run(conn -> {
 *       saleRepository.insert(conn, sale);
 *       for (SaleItem item : items) {
 *           saleItemRepository.insert(conn, item);
 *           productRepository.adjustStock(conn, item.getProductId(), -item.getQuantity());
 *           movementRepository.insert(conn, buildMovement(item));
 *       }
 *       paymentRepository.insert(conn, payment);
 *       return sale.getSaleId();
 *   });
 *
 * All repository methods used inside the lambda MUST be the
 * (Connection conn, ...) overloads, so every call in the block
 * shares this same connection and transaction.
 */
public final class TransactionManager {

    private TransactionManager() {
        // static utility class — no instances
    }

    public static <T> T run(TransactionalWork<T> work) {
        try (Connection conn = HikariDatabaseConfig.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                T result = work.execute(conn);
                conn.commit();
                return result;
            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    // Rollback itself failed — surface both failures, don't swallow either.
                    RepositoryException wrapped = new RepositoryException(
                            "Transaction failed AND rollback failed", e);
                    wrapped.addSuppressed(rollbackEx);
                    throw wrapped;
                }
                throw new RepositoryException("Transaction rolled back due to: " + e.getMessage(), e);
            } finally {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException ignored) {
                    // Connection is being returned to the pool either way; not fatal.
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to obtain or close connection for transaction", e);
        }
    }

    /** Overload for work that doesn't need to return a value. */
    public static void runVoid(VoidTransactionalWork work) {
        run(conn -> {
            work.execute(conn);
            return null;
        });
    }

    @FunctionalInterface
    public interface VoidTransactionalWork {
        void execute(Connection conn) throws SQLException;
    }
}
