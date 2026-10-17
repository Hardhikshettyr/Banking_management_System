package com.bank.model;

import java.io.Serializable;

/** A login. The password is never stored, only a salted PBKDF2 hash of it. Immutable. */
public final class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String passwordHash;
    private final Role role;
    private final String customerId; // only for Role.CUSTOMER

    public User(String username, String passwordHash, Role role, String customerId) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.customerId = customerId;
    }

    public User withPasswordHash(String newHash) {
        return new User(username, newHash, role, customerId);
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public String getCustomerId() { return customerId; }

    @Override
    public String toString() {
        return username + " (" + role + (customerId == null ? "" : ", " + customerId) + ")";
    }
}
