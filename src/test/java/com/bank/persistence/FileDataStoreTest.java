package com.bank.persistence;

import com.bank.exception.PersistenceException;
import com.bank.model.AccountType;
import com.bank.model.BankAccount;
import com.bank.model.Customer;
import com.bank.service.BankService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileDataStoreTest {

    @TempDir
    Path dir;

    @Test
    void missingFileGivesEmptyBank() {
        BankService bank = new BankService(new FileDataStore(dir.resolve("none.dat")));
        assertTrue(bank.listAccounts().isEmpty());
    }

    @Test
    void dataSurvivesRestart() {
        Path file = dir.resolve("nested/bank.dat");

        BankService first = new BankService(new FileDataStore(file));
        Customer c = first.createCustomer("Asha Rao", "asha@example.com", "9876543210");
        BankAccount a = first.openAccount(c.getCustomerId(), AccountType.SAVINGS, new BigDecimal("10000"));
        BankAccount b = first.openAccount(c.getCustomerId(), AccountType.CURRENT, new BigDecimal("0"));
        first.withdraw(a.getAccountNumber(), new BigDecimal("2000"));
        first.transfer(a.getAccountNumber(), b.getAccountNumber(), new BigDecimal("1000"));

        // "Restart": a brand new service reading the same file
        BankService second = new BankService(new FileDataStore(file));
        BankAccount a2 = second.getAccount(a.getAccountNumber());
        BankAccount b2 = second.getAccount(b.getAccountNumber());
        assertEquals(new BigDecimal("7000.00"), a2.getBalance());
        assertEquals(new BigDecimal("1000.00"), b2.getBalance());
        assertEquals(3, a2.getTransactions().size());
        assertEquals("Asha Rao", a2.getCustomer().getName());

        // ID counters continue instead of restarting (no duplicate account numbers)
        BankAccount c2 = second.openAccount(c.getCustomerId(), AccountType.CURRENT, BigDecimal.ZERO);
        assertNotEquals(a.getAccountNumber(), c2.getAccountNumber());
        assertNotEquals(b.getAccountNumber(), c2.getAccountNumber());
    }

    @Test
    void failedOperationIsNotPersistedAsChange() {
        Path file = dir.resolve("bank.dat");
        BankService bank = new BankService(new FileDataStore(file));
        Customer c = bank.createCustomer("Asha Rao", "asha@example.com", "9876543210");
        BankAccount a = bank.openAccount(c.getCustomerId(), AccountType.SAVINGS, new BigDecimal("1000"));
        assertThrows(RuntimeException.class, () -> bank.withdraw(a.getAccountNumber(), new BigDecimal("5000")));

        BankService reloaded = new BankService(new FileDataStore(file));
        assertEquals(new BigDecimal("1000.00"), reloaded.getAccount(a.getAccountNumber()).getBalance());
    }

    @Test
    void corruptFileGivesClearError() throws IOException {
        Path file = dir.resolve("bad.dat");
        Files.writeString(file, "this is not serialized data");
        assertThrows(PersistenceException.class, () -> new FileDataStore(file).load());
    }
}
