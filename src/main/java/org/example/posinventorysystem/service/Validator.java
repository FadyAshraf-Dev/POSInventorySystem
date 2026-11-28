package org.example.posinventorysystem.service;

final class Validator {

    public static final int MIN_PASSWORD_LENGTH = 6;

    private Validator() { }

    public static void validatePassword(String password) {
        Check.that(password != null && password.length() >= MIN_PASSWORD_LENGTH,
                "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
    }


}
