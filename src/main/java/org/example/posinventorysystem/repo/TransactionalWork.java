package org.example.posinventorysystem.repo;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A unit of work that runs inside a single transaction.
 * Declared to throw SQLException so repository calls (which are checked)
 * can be used directly inside the lambda without a wrapper try/catch.
 */
@FunctionalInterface
public interface TransactionalWork<T> {
    T execute(Connection conn) throws SQLException;
}
