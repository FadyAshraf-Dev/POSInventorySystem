package org.example.posinventorysystem.model;

public class User {

    private Integer userId;
    private String fullName;
    private String username;
    private String passwordHash;
    private Integer roleId;
    private boolean active;

    public User() {
    }

    public User(Integer userId, String fullName, String username, String passwordHash,
                Integer roleId, boolean active) {
        this.userId = userId;
        this.fullName = fullName;
        this.username = username;
        this.passwordHash = passwordHash;
        this.roleId = roleId;
        this.active = active;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "User{userId=" + userId + ", fullName='" + fullName + "', username='" + username
                + "', roleId=" + roleId + ", active=" + active + "}";
    }
}
