package org.example.posinventorysystem.service;

import org.example.posinventorysystem.model.Role;
import org.example.posinventorysystem.model.User;
import org.example.posinventorysystem.repo.RoleRepository;
import org.example.posinventorysystem.repo.UserRepository;

//Login , logout, password change and user creation.
public class AuthService {
    private final UserRepository userRepo = new UserRepository();
    private final RoleRepository roleRepo = new RoleRepository();

    /** Returns the logged-in user and fills CurrentUser. One generic error on purpose: don't reveal which part was wrong. */
    public User login(String username, String password) {
        Check.that(username != null && !username.isBlank(), "Enter your username.");
        Check.that(password != null && !password.isEmpty(), "Enter your password.");

        User user = userRepo.findByUsername(username.trim()).orElse(null);
        boolean ok = user != null && PasswordHasher.verify(password, user.getPasswordHash());
        Check.that(ok, "Wrong username or password.");
        Check.that(user.isActive(), "This account is disabled. Contact the administrator.");

        String roleName = roleRepo.findById(user.getRoleId()).map(Role::getRoleName).orElse("");
        CurrentUser.set(user, roleName);
        return user;
    }

    public void logout() {
        CurrentUser.clear();
    }

    /** A user changes their own password (needs the old one). */
    public void changePassword(int userId, String oldPassword, String newPassword) {
        CurrentUser.requireLogin();
        Validator.validatePassword(newPassword);
        User user = userRepo.findById(userId).orElseThrow(() -> new BusinessException("User not found."));
        Check.that(PasswordHasher.verify(oldPassword, user.getPasswordHash()), "Old password is incorrect.");
        String newHash = PasswordHasher.hash(newPassword);
        ServiceTx.run(conn -> userRepo.updatePassword(conn, userId, newHash));
    }

}
