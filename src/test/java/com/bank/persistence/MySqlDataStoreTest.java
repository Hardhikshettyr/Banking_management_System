package com.bank.persistence;

import com.bank.exception.PersistenceException;
import com.bank.model.AccountType;
import com.bank.model.BankAccount;
import com.bank.model.Customer;
import com.bank.model.Role;
import com.bank.service.BankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs only when BANK_TEST_DB_URL is set. WARNING: it empties every bank table in that
 * database, so point it at a throw-away database, never at your real one.
 * Optional: BANK_TEST_DB_USER, BANK_TEST_DB_PASSWORD.
 */
@EnabledIfEnvironmentVariable(named = "BANK_TEST_DB_URL", matches = ".+")
class MySqlDataStoreTest {

    private DbConfig config;

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @BeforeEach
    void cleanDatabase() throws SQLException {
        config = new DbConfig(System.getenv("BANK_TEST_DB_URL"),
                System.getenv().getOrDefault("BANK_TEST_DB_USER", "root"),
                System.getenv().getOrDefault("BANK_TEST_DB_PASSWORD", ""));
        new MySqlDataStore(config); // creates the tables if needed
        try (Connection c = DriverManager.getConnection(config.url(), config.user(), config.password());
             Statement st = c.createStatement()) {
            for (String table : new String[]{"transactions", "users", "accounts", "customers", "counters"}) {
                st.executeUpdate("DELETE FROM " + table);
            }
        }
    }

    private BankService newBank() {
        return new BankService(new MySqlDataStore(config));
    }

    @Test
    void everythingSurvivesARestart() {
        BankService first = newBank();
        assertTrue(first.isFirstRun());
        first.createFirstAdmin("admin", "Admin@1234");
        Customer c = first.createCustomer("Asha Rao", "asha@example.com", "9876543210");
        BankAccount savings = first.openAccount(c.getCustomerId(), AccountType.SAVINGS, bd("10000"));
        BankAccount current = first.openAccount(c.getCustomerId(), AccountType.CURRENT, bd("0"));
        first.withdraw(savings.getAccountNumber(), bd("2000"));
        first.transfer(savings.getAccountNumber(), current.getAccountNumber(), bd("1000"));
        first.applyInterest(savings.getAccountNumber());
        first.createUser("asha", "Asha@1234", Role.CUSTOMER, c.getCustomerId());

        BankService second = newBank();
        assertFalse(second.isFirstRun());
        BankAccount s2 = second.getAccount(savings.getAccountNumber());
        BankAccount c2 = second.getAccount(current.getAccountNumber());
        assertEquals(bd("7023.33"), s2.getBalance());
        assertEquals(bd("1000.00"), c2.getBalance());
        assertEquals(4, s2.getTransactions().size());
        assertEquals(savings.getTransactions().get(2).getId(), s2.getTransactions().get(2).getId());
        assertEquals(savings.getTransactions().get(2).getTimestamp(), s2.getTransactions().get(2).getTimestamp());
        assertEquals(AccountType.CURRENT, c2.getType());
        assertEquals("Asha Rao", s2.getCustomer().getName());
        assertEquals(c.getCustomerId(), second.authenticate("asha", "Asha@1234").getCustomerId());
        assertNotNull(second.authenticate("admin", "Admin@1234"));

        // IDs keep counting up instead of restarting
        BankAccount third = second.openAccount(c.getCustomerId(), AccountType.CURRENT, bd("0"));
        assertNotEquals(savings.getAccountNumber(), third.getAccountNumber());
        assertNotEquals(current.getAccountNumber(), third.getAccountNumber());
    }

    @Test
    void laterChangesAreSavedIncrementally() {
        BankService bank = newBank();
        Customer c = bank.createCustomer("Asha Rao", "asha@example.com", "9876543210");
        BankAccount a = bank.openAccount(c.getCustomerId(), AccountType.CURRENT, bd("100"));
        for (int i = 0; i < 5; i++) {
            bank.deposit(a.getAccountNumber(), bd("10"));
        }
        BankAccount reloaded = newBank().getAccount(a.getAccountNumber());
        assertEquals(bd("150.00"), reloaded.getBalance());
        assertEquals(6, reloaded.getTransactions().size());
    }

    @Test
    void passwordChangeIsStored() {
        BankService bank = newBank();
        bank.createFirstAdmin("admin", "Admin@1234");
        bank.changePassword("admin", "Admin@1234", "Changed@5678");
        assertNotNull(newBank().authenticate("admin", "Changed@5678"));
        assertThrows(com.bank.exception.AuthenticationException.class,
                () -> newBank().authenticate("admin", "Admin@1234"));
    }

    @Test
    void databaseRowsLookRight() throws SQLException {
        BankService bank = newBank();
        Customer c = bank.createCustomer("Asha Rao", "asha@example.com", "9876543210");
        BankAccount a = bank.openAccount(c.getCustomerId(), AccountType.SAVINGS, bd("1000"));
        bank.createFirstAdmin("admin", "Admin@1234");
        try (Connection conn = DriverManager.getConnection(config.url(), config.user(), config.password());
             Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT balance, account_type FROM accounts")) {
                assertTrue(rs.next());
                assertEquals(0, bd("1000.00").compareTo(rs.getBigDecimal(1)));
                assertEquals("SAVINGS", rs.getString(2));
            }
            try (ResultSet rs = st.executeQuery("SELECT password_hash FROM users WHERE username='admin'")) {
                assertTrue(rs.next());
                assertTrue(rs.getString(1).startsWith("pbkdf2$"));
                assertFalse(rs.getString(1).contains("Admin@1234"));
            }
        }
    }

    @Test
    void tamperedBalanceIsDetectedOnLoad() throws SQLException {
        BankService bank = newBank();
        Customer c = bank.createCustomer("Asha Rao", "asha@example.com", "9876543210");
        bank.openAccount(c.getCustomerId(), AccountType.SAVINGS, bd("1000"));
        try (Connection conn = DriverManager.getConnection(config.url(), config.user(), config.password());
             Statement st = conn.createStatement()) {
            st.executeUpdate("UPDATE accounts SET balance = 999999.00");
        }
        assertThrows(PersistenceException.class, this::newBank);
    }

    @Test
    void wrongPasswordGivesAClearConnectionError() {
        DbConfig bad = new DbConfig(config.url(), config.user(), "definitely-wrong-password");
        PersistenceException e = assertThrows(PersistenceException.class, () -> new MySqlDataStore(bad));
        assertTrue(e.getMessage().contains("Cannot connect"), e.getMessage());
    }
}
