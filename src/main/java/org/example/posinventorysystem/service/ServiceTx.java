package org.example.posinventorysystem.service;

import org.example.posinventorysystem.repo.RepositoryException;
import org.example.posinventorysystem.repo.TransactionManager;
import org.example.posinventorysystem.repo.TransactionalWork;

//Thin wrapper over TransactionManager.run.
final class ServiceTx {
    private ServiceTx() { }

    static <T> T run(TransactionalWork<T> work) {
        try {
            return TransactionManager.run(work);
        } catch (RepositoryException e) {
            if (e.getCause() instanceof BusinessException business) {
                throw business;
            }
            throw e;
        }
    }
}
