package com.bank.service;

import com.bank.exception.AccountNotFoundException;
import com.bank.exception.InvalidAmountException;
import com.bank.exception.InvalidInputException;
import com.bank.model.AccountType;
import com.bank.model.BankAccount;
import com.bank.model.Customer;
import com.bank.persistence.BankData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AccountServiceTest {

    private AccountService service;
    private Customer asha;

    @BeforeEach
    void setUp() {
        service = new AccountService(new BankData());
        asha = service.createCustomer("Asha Rao", "asha@example.com", "9876543210");
    }

    @Test
    void customersAndAccountsGetUniqueIds() {
        Customer ravi = service.createCustomer("Ravi Kumar", "ravi@example.com", "9123456780");
        assertNotEquals(asha.getCustomerId(), ravi.getCustomerId());

        Set<String> numbers = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            numbers.add(service.openAccount(asha.getCustomerId(), AccountType.CURRENT, BigDecimal.ZERO)
                    .getAccountNumber());
        }
        assertEquals(5, numbers.size());
    }

    @Test
    void savingsNeedsMinimumOpeningDeposit() {
        assertThrows(InvalidAmountException.class,
                () -> service.openAccount(asha.getCustomerId(), AccountType.SAVINGS, new BigDecimal("499.99")));
        assertTrue(service.listAccounts().isEmpty(), "failed open must not create an account");

        BankAccount a = service.openAccount(asha.getCustomerId(), AccountType.SAVINGS, new BigDecimal("500"));
        assertEquals(new BigDecimal("500.00"), a.getBalance());
        assertEquals(1, a.getTransactions().size());
    }

    @Test
    void currentAccountCanOpenWithNoDeposit() {
        BankAccount a = service.openAccount(asha.getCustomerId(), AccountType.CURRENT, null);
        assertEquals(0, a.getBalance().signum());
        assertTrue(a.getTransactions().isEmpty());
    }

    @Test
    void openingForUnknownCustomerFails() {
        assertThrows(AccountNotFoundException.class,
                () -> service.openAccount("CUST9999", AccountType.CURRENT, BigDecimal.ZERO));
    }

    @Test
    void customerValidation() {
        assertThrows(InvalidInputException.class, () -> service.createCustomer("A", "a@b.com", "9876543210"));
        assertThrows(InvalidInputException.class, () -> service.createCustomer("Good Name", "not-an-email", "9876543210"));
        assertThrows(InvalidInputException.class, () -> service.createCustomer("Good Name", "g@example.com", "12345"));
        assertThrows(InvalidInputException.class, () -> service.createCustomer("Good Name", "g@example.com", "5876543210"));
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() {
        assertThrows(InvalidInputException.class,
                () -> service.createCustomer("Another Asha", "ASHA@example.com", "9876543211"));
    }

    @Test
    void findAccountIsCaseInsensitiveAndFailsForUnknown() {
        BankAccount a = service.openAccount(asha.getCustomerId(), AccountType.CURRENT, BigDecimal.ZERO);
        assertSame(a, service.findAccount(a.getAccountNumber().toLowerCase()));
        assertThrows(AccountNotFoundException.class, () -> service.findAccount("ACC000000"));
        assertThrows(AccountNotFoundException.class, () -> service.findAccount(null));
    }

    @Test
    void searchByNumberCustomerIdOrNamePart() {
        BankAccount a = service.openAccount(asha.getCustomerId(), AccountType.CURRENT, BigDecimal.ZERO);
        assertEquals(List.of(a), service.search(a.getAccountNumber()));
        assertEquals(List.of(a), service.search(asha.getCustomerId()));
        assertEquals(List.of(a), service.search("ash"));
        assertTrue(service.search("nobody").isEmpty());
        assertThrows(InvalidInputException.class, () -> service.search("  "));
    }
}
