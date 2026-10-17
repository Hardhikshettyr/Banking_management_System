package com.bank.service;

import com.bank.exception.AccessDeniedException;
import com.bank.exception.PersistenceException;
import com.bank.model.AccountType;
import com.bank.model.BankAccount;
import com.bank.model.Customer;
import com.bank.persistence.FileDataStore;
import com.bank.persistence.InMemoryDataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BankServiceTest {

    @TempDir
    Path dir;

    @Test
    void firstRunIsTrueOnlyUntilAnAdminExists() {
        BankService bank = new BankService(new InMemoryDataStore());
        assertTrue(bank.isFirstRun());
        bank.createFirstAdmin("admin", "Admin@1234");
        assertFalse(bank.isFirstRun());
        assertThrows(AccessDeniedException.class, () -> bank.createFirstAdmin("hacker", "Hacker@1234"));
    }

    @Test
    void failedSaveUndoesTheChangeInMemory() {
        InMemoryDataStore store = new InMemoryDataStore();
        BankService bank = new BankService(store);
        Customer c = bank.createCustomer("Asha Rao", "asha@example.com", "9876543210");
        BankAccount a = bank.openAccount(c.getCustomerId(), AccountType.SAVINGS, new BigDecimal("1000"));

        store.failNextSave();
        PersistenceException e = assertThrows(PersistenceException.class,
                () -> bank.deposit(a.getAccountNumber(), new BigDecimal("500")));
        assertTrue(e.getMessage().contains("undone"), e.getMessage());

        // memory and storage agree again: the failed deposit is gone, and the bank still works
        assertEquals(new BigDecimal("1000.00"), bank.getAccount(a.getAccountNumber()).getBalance());
        bank.deposit(a.getAccountNumber(), new BigDecimal("200"));
        assertEquals(new BigDecimal("1200.00"), bank.getAccount(a.getAccountNumber()).getBalance());
        assertEquals(new BigDecimal("1200.00"),
                new BankService(store).getAccount(a.getAccountNumber()).getBalance());
    }

    @Test
    void failedSaveOfALoginLeavesNoLogin() {
        InMemoryDataStore store = new InMemoryDataStore();
        BankService bank = new BankService(store);
        store.failNextSave();
        assertThrows(PersistenceException.class, () -> bank.createFirstAdmin("admin", "Admin@1234"));
        assertTrue(bank.isFirstRun());
    }

    @Test
    void loginsSurviveARestartWithFileStorage() {
        Path file = dir.resolve("bank.dat");
        BankService first = new BankService(new FileDataStore(file));
        first.createFirstAdmin("admin", "Admin@1234");
        Customer c = first.createCustomer("Asha Rao", "asha@example.com", "9876543210");
        first.createUser("asha", "Asha@1234", com.bank.model.Role.CUSTOMER, c.getCustomerId());
        first.changePassword("asha", "Asha@1234", "Changed@5678");

        BankService second = new BankService(new FileDataStore(file));
        assertFalse(second.isFirstRun());
        assertNotNull(second.authenticate("admin", "Admin@1234"));
        assertNotNull(second.authenticate("asha", "Changed@5678"));
        assertThrows(com.bank.exception.AuthenticationException.class,
                () -> second.authenticate("asha", "Asha@1234"));
    }
}
