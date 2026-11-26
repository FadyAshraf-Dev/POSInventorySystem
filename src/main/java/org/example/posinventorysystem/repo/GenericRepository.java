package org.example.posinventorysystem.repo;

/** Full CRUD-shaped contract for ordinary entities, with deletion implemented as soft deletion. */
public interface GenericRepository<T, ID> extends SoftDeletableRepository<T, ID>, InsertOnlyRepository<T,ID>,ReadOnlyRepository<T,ID>, UpdatableRepository<T,ID> { }
