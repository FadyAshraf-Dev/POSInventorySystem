package org.example.posinventorysystem.repo;

/**
 * Unchecked wrapper around SQLException (and any other DAO-layer failure).
 * DAOs catch SQLException internally and rethrow this, so service/UI code
 * doesn't need to handle checked SQLException at every call site —
 * only catch RepositoryException where it actually matters.
 */
public class RepositoryException extends RuntimeException {

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RepositoryException(String message) {
        super(message);
    }
}
