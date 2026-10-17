package com.bank.service;

import com.bank.exception.AuthenticationException;
import com.bank.exception.InvalidInputException;
import com.bank.model.AccountType;
import com.bank.model.Customer;
import com.bank.model.Role;
import com.bank.model.User;
import com.bank.persistence.BankData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private BankData data;
    private AuthService auth;
    private Customer asha;

    @BeforeEach
    void setUp() {
        data = new BankData();
        auth = new AuthService(data);
        asha = new AccountService(data).createCustomer("Asha Rao", "asha@example.com", "9876543210");
    }

    @Test
    void createdUserCanLoginAndPasswordIsNotStoredInPlainText() {
        auth.createUser("Admin", "Admin@1234", Role.ADMIN, null);
        User u = auth.authenticate("admin", "Admin@1234");
        assertEquals("admin", u.getUsername());
        assertEquals(Role.ADMIN, u.getRole());
        assertFalse(u.getPasswordHash().contains("Admin@1234"));
    }

    @Test
    void usernameIsCaseInsensitive() {
        auth.createUser("asha", "Asha@1234", Role.CUSTOMER, asha.getCustomerId());
        assertNotNull(auth.authenticate("  ASHA ", "Asha@1234"));
    }

    @Test
    void wrongPasswordAndUnknownUserGiveTheSameMessage() {
        auth.createUser("admin", "Admin@1234", Role.ADMIN, null);
        AuthenticationException wrongPassword = assertThrows(AuthenticationException.class,
                () -> auth.authenticate("admin", "nope12345"));
        AuthenticationException unknownUser = assertThrows(AuthenticationException.class,
                () -> auth.authenticate("ghost", "Admin@1234"));
        assertEquals(wrongPassword.getMessage(), unknownUser.getMessage());
        assertThrows(AuthenticationException.class, () -> auth.authenticate(null, null));
    }

    @Test
    void weakPasswordsAndBadUsernamesAreRejected() {
        assertThrows(InvalidInputException.class, () -> auth.createUser("admin", "short1", Role.ADMIN, null));
        assertThrows(InvalidInputException.class, () -> auth.createUser("admin", "onlyletters", Role.ADMIN, null));
        assertThrows(InvalidInputException.class, () -> auth.createUser("admin", "12345678", Role.ADMIN, null));
        assertThrows(InvalidInputException.class, () -> auth.createUser("a b", "Admin@1234", Role.ADMIN, null));
        assertThrows(InvalidInputException.class, () -> auth.createUser("ab", "Admin@1234", Role.ADMIN, null));
        assertFalse(auth.hasUsers());
    }

    @Test
    void duplicateUsernameIsRejected() {
        auth.createUser("admin", "Admin@1234", Role.ADMIN, null);
        assertThrows(InvalidInputException.class, () -> auth.createUser("ADMIN", "Other@1234", Role.ADMIN, null));
    }

    @Test
    void customerLoginNeedsAnExistingCustomerAndOnlyOneLoginPerCustomer() {
        assertThrows(InvalidInputException.class,
                () -> auth.createUser("ghost", "Ghost@1234", Role.CUSTOMER, "CUST9999"));
        assertThrows(InvalidInputException.class,
                () -> auth.createUser("ghost", "Ghost@1234", Role.CUSTOMER, null));
        auth.createUser("asha", "Asha@1234", Role.CUSTOMER, asha.getCustomerId());
        assertThrows(InvalidInputException.class,
                () -> auth.createUser("asha2", "Asha@1234", Role.CUSTOMER, asha.getCustomerId()));
    }

    @Test
    void adminCannotBeLinkedToACustomer() {
        assertThrows(InvalidInputException.class,
                () -> auth.createUser("boss", "Boss@1234", Role.ADMIN, asha.getCustomerId()));
    }

    @Test
    void changePasswordWorksAndOldPasswordStopsWorking() {
        auth.createUser("admin", "Admin@1234", Role.ADMIN, null);
        auth.changePassword("admin", "Admin@1234", "NewPass@5678");
        assertThrows(AuthenticationException.class, () -> auth.authenticate("admin", "Admin@1234"));
        assertNotNull(auth.authenticate("admin", "NewPass@5678"));
    }

    @Test
    void changePasswordRequiresCorrectOldPasswordAndAStrongDifferentNewOne() {
        auth.createUser("admin", "Admin@1234", Role.ADMIN, null);
        assertThrows(AuthenticationException.class, () -> auth.changePassword("admin", "wrong12345", "NewPass@5678"));
        assertThrows(InvalidInputException.class, () -> auth.changePassword("admin", "Admin@1234", "weak"));
        assertThrows(InvalidInputException.class, () -> auth.changePassword("admin", "Admin@1234", "Admin@1234"));
        assertNotNull(auth.authenticate("admin", "Admin@1234"));
    }
}
