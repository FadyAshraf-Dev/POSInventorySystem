package org.example.posinventorysystem.service;

import org.example.posinventorysystem.model.Role;
import org.example.posinventorysystem.model.User;
import org.example.posinventorysystem.repo.RoleRepository;
import org.example.posinventorysystem.repo.UserRepository;

//Login , logout, password change and user creation.
public class AuthService {
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository userRepo = new UserRepository();
    private final RoleRepository roleRepo = new RoleRepository();

    public User login(String username, String password) {
        Validator.Check(username != null && !username.isBlank(), "Enter your username.");
        Validator.Check(password != null && !password.isEmpty(), "Enter your password.");

        User user = userRepo.findByUsername(username.trim()).orElse(null);

        boolean ok = user != null && PasswordHasher.verify(password, user.getPasswordHash());
        Check.that(ok, "Wrong username or password.");
        Check.that(user.isActive(), "This account is disabled. Contact the administrator.");

        String roleName = roleRepo.findById(user.getRoleId()).map(Role::getRoleName).orElse("");
        Session.start(user, roleName);
        return user;
    }

}
