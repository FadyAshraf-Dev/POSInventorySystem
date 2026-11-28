package org.example.posinventorysystem.service;

/** Tiny validation helper: Check.that(condition, "message shown to the user"). */
final class Check {
    private Check() { }

    static void that(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(message);
        }
    }
}