package org.example.posinventorysystem.repo;
import java.util.List;
import java.util.Optional;
/** Read operations; ordinary reads exclude soft-deleted rows. */
public interface ReadOnlyRepository<T, ID> { Optional<T> findById(ID id); List<T> findAll(); }
