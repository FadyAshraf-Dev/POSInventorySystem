package org.example.posinventorysystem.service;

import org.example.posinventorysystem.model.User;

/** «static» Who is logged in right now (one desktop app = one session). Set by AuthService.login. */
public final class CurrentUser {
    private static User user;
    private static String roleName;

    private CurrentUser() { }

    static void set(User loggedIn, String role) {
        user = loggedIn;
        roleName = role;
    }

    public static void clear() {
        user = null;
        roleName = null;
    }

    public static User getUser() {
        return user;
    }

    public static String getRoleName() {
        return roleName;
    }

    public static boolean isLoggedIn() {
        return user != null;
    }

    public static boolean isAdmin() {
        return user != null && "admin".equalsIgnoreCase(roleName);
    }

    /** Throws AccessDeniedException when nobody is logged in; returns the user otherwise. */
    public static User requireLogin() {
        if (user == null) throw new AccessDeniedException("Please log in first.");
        return user;
    }

    public static void requireAdmin() {
        requireLogin();
        if (!isAdmin()) throw new AccessDeniedException("Administrator permission required.");
    }
}
