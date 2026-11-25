package org.example.posinventorysystem.repo;

import java.util.List;
import java.util.Optional;

/**
 * Shared contract for all table-specific DAOs.
 * T  = the entity/model type (e.g. Role, Product, Sale)
 * ID = the primary key type (Integer for every table in this schema)
 */
public interface GenericRepository<T, ID> {

    Optional<T> findById(ID id);

    List<T> findAll();

    /**
     * Inserts a new row. Returns the generated primary key
     * (useful since every table here uses AUTO_INCREMENT).
     */
    ID insert(T entity);

    /**
     * Updates an existing row. Returns true if a row was actually changed.
     */
    boolean update(T entity);

    /**
     * Deletes a row by id. Returns true if a row was actually deleted.
     */
    boolean deleteById(ID id);
}
