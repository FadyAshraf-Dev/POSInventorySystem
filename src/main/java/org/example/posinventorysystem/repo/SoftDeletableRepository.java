package org.example.posinventorysystem.repo;
/** Soft-delete contract: deleted_at is set/cleared; hard deletes are intentionally absent. */
public interface SoftDeletableRepository<T, ID> extends UpdatableRepository<T, ID> { boolean softDelete(ID id); boolean restore(ID id); }
