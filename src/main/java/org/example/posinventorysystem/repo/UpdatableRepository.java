package org.example.posinventorysystem.repo;
/** Repository contract for rows that may be inserted and edited, but not deleted. */
public interface UpdatableRepository<T, ID> extends InsertOnlyRepository<T, ID> { boolean update(T entity); }
