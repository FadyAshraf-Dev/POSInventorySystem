package org.example.posinventorysystem.service;

import org.example.posinventorysystem.model.Role;
import org.example.posinventorysystem.model.User;
import org.example.posinventorysystem.repo.RoleRepository;
import org.example.posinventorysystem.repo.UserRepository;

import java.util.List;

public class UserService {
    private final UserRepository userRepo = new UserRepository();
    private final RoleRepository roleRepo = new RoleRepository();

    public List<User> getAll() {
        CurrentUser.requireAdmin();
        return userRepo.findAll();
    }

    public List<Role> getRoles() {
        CurrentUser.requireAdmin();
        return roleRepo.findAll();
    }

    public User findUserById(int id){
        CurrentUser.requireAdmin();
        return userRepo.findById(id)
                .orElse(null);
    }

    public User findUserByUsername(String username){
        CurrentUser.requireAdmin();
        return userRepo.findByUsername(username)
                .orElse(null);
    }

    public int add(User user, String password) {
       // CurrentUser.requireAdmin();
        validateUser(user);
        Validator.validatePassword(password);
        Check.that(roleRepo.findById(user.getRoleId()).isPresent(), "Role not found.");

        return ServiceTx.run(conn -> {
            Check.that(!userRepo.usernameExists(conn, user.getUsername(), null), "Username already exists.");
            user.setPasswordHash(PasswordHasher.hash(password));
            user.setActive(true);
            return userRepo.insert(conn, user);
        });
    }

    public void update(User user) {
        CurrentUser.requireAdmin();
        validateUser(user);
        ServiceTx.run(conn -> {
            User current = userRepo.findById(conn, user.getUserId())
                    .orElseThrow(() -> new BusinessException("User not found."));
            // may be a problem
            Check.that(!userRepo.usernameExists(conn, user.getUsername(), user.getUserId()), "Username already exists.");
            user.setPasswordHash(current.getPasswordHash());
            return userRepo.update(conn, user);
        });
    }

    public void deleteUser(int userId)
    {
        CurrentUser.requireAdmin();
        ServiceTx.run(conn -> {
            Check.that(userRepo.softDelete(conn, userId), "User not found.");
            return null;
        });
    }

    public void restoreUser(int userId)
    {
        CurrentUser.requireAdmin();
        ServiceTx.run(conn -> {
            Check.that(userRepo.restore(conn, userId), "User not found.");
            return null;
        });
    }

    public void resetPassword(int userId, String newPassword) {
        CurrentUser.requireAdmin();
        Validator.validatePassword(newPassword);
        String hash = PasswordHasher.hash(newPassword);
        ServiceTx.run(conn -> {
            Check.that(userRepo.updatePassword(conn, userId, hash), "User not found.");
            return null;
        });
    }

    /** An admin cannot disable his own account (he would lock himself out). */
    public void setActive(int userId, boolean active) {
        CurrentUser.requireAdmin();
        Check.that(active || CurrentUser.getUser().getUserId() != userId, "You cannot disable your own account.");
        ServiceTx.run(conn -> userRepo.setActive(conn, userId, active));
    }

    private void validateUser(User user) {
        Check.that(user != null, "User is required.");
        Check.that(user.getFullName() != null && !user.getFullName().isBlank(), "Full name is required.");
        Check.that(user.getUsername() != null && !user.getUsername().isBlank(), "Username is required.");
        Check.that(user.getRoleId() != null && user.getRoleId() > 0, "Choose a role.");
        user.setFullName(user.getFullName().trim());
        user.setUsername(user.getUsername().trim());
    }
}
