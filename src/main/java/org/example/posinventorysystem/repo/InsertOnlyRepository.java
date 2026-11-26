package org.example.posinventorysystem.repo;
/** Append-only ledger contract. No update or deletion operations are exposed. */
public interface InsertOnlyRepository<T, ID> extends ReadOnlyRepository<T, ID> { ID insert(T entity); }
