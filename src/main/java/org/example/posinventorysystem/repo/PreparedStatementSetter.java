package org.example.posinventorysystem.repo;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface PreparedStatementSetter {
    void setValues(PreparedStatement statement) throws SQLException;
}
