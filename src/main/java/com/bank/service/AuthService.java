package com.bank.service;

import com.bank.exception.AuthenticationException;
import com.bank.exception.InvalidInputException;
import com.bank.model.Role;
import com.bank.model.User;
import com.bank.persistence.BankData;
import com.bank.util.PasswordHasher;
import com.bank.util.Validator;

/** Creates logins and checks passwords. Only password hashes are ever stored. */
public class AuthService {

    private static final String LOGIN_FAILED = "Invalid username or password.";
    /** Checked when the username does not exist, so a wrong username takes as long as a wrong password. */
    private static final String DUMMY_HASH = PasswordHasher.hash("not-a-real-password-1");

    private final BankData data;

    public AuthService(BankData data) {
        this.data = data;
    }

    public boolean hasUsers() {
        return !data.users().isEmpty();
    }

    public User createUser(String username, String password, Role role, String customerId) {
        String name = Validator.requireValidUsername(username);
        Validator.requireStrongPassword(password);
        if (data.users().containsKey(name)) {
            throw new InvalidInputException("That username is already taken.");
        }
        if (role == Role.CUSTOMER) {
            if (customerId == null || !data.customers().containsKey(customerId)) {
                throw new InvalidInputException("A customer login must belong to an existing customer.");
            }
            boolean alreadyHasLogin = data.users().values().stream()
                    .anyMatch(u -> customerId.equals(u.getCustomerId()));
            if (alreadyHasLogin) {
                throw new InvalidInputException("This customer already has a login.");
            }
        } else if (customerId != null) {
            throw new InvalidInputException("An admin login cannot be linked to a customer.");
        }
        User user = new User(name, PasswordHasher.hash(password), role, customerId);
        data.users().put(name, user);
        return user;
    }

    public User authenticate(String username, String password) {
        String name = username == null ? "" : username.trim().toLowerCase();
        User user = data.users().get(name);
        String hash = user != null ? user.getPasswordHash() : DUMMY_HASH;
        boolean passwordOk = PasswordHasher.verify(password, hash);
        if (user == null || !passwordOk) {
            throw new AuthenticationException(LOGIN_FAILED);
        }
        return user;
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = authenticate(username, oldPassword);
        Validator.requireStrongPassword(newPassword);
        if (newPassword.equals(oldPassword)) {
            throw new InvalidInputException("The new password must be different from the old one.");
        }
        data.users().put(user.getUsername(), user.withPasswordHash(PasswordHasher.hash(newPassword)));
    }
}
