package org.example.posinventorysystem.service;

final class Validator {
    private Validator() { }

    /** Tiny validation helper: Check.that(condition, "message shown to the user"). */
    static public void Check(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(message);
        }
    }
}
