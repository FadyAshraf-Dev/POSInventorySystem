package org.example.posinventorysystem.service;

public class AccessDeniedException extends BusinessException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
