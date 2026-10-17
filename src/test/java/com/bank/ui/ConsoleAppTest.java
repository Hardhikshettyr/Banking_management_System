package com.bank.ui;

import com.bank.persistence.InMemoryDataStore;
import com.bank.service.BankService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/** Drives the real menus with scripted keystrokes. */
class ConsoleAppTest {

    private String run(BankService bank, String... lines) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        Scanner in = new Scanner(new ByteArrayInputStream(
                (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        new ConsoleApp(bank, in, out, false).run();
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void fullAdminAndCustomerJourney() {
        BankService bank = new BankService(new InMemoryDataStore());
        String screen = run(bank,
                // first-run setup
                "admin", "Admin@1234", "Admin@1234",
                // admin logs in, creates customer + savings account + a login for them
                "1", "admin", "Admin@1234",
                "1", "Asha Rao", "asha@example.com", "9876543210",
                "2", "CUST1001", "1", "10000",
                "1", "Ravi Kumar", "ravi@example.com", "9123456780",
                "2", "CUST1002", "2", "0",
                "11", "CUST1001", "asha", "Asha@1234", "Asha@1234",
                "0",
                // customer: one wrong password, then correct
                "1", "asha", "wrong-password1", "asha", "Asha@1234",
                "1",
                "3", "ACC100001", "500",
                "4", "ACC100001", "20000",
                "2", "ACC100002",
                "6", "ACC100001",
                "13", // not a customer menu option
                "0",
                "0");

        assertTrue(screen.contains("Administrator created"), screen);
        assertTrue(screen.contains("Customer created. Customer ID: CUST1001"), screen);
        assertTrue(screen.contains("Account opened. Account number: ACC100001"), screen);
        assertTrue(screen.contains("Login created: asha for CUST1001"), screen);
        assertTrue(screen.contains("Invalid username or password. (attempt 1 of 3)"), screen);
        assertTrue(screen.contains("Welcome, asha (CUSTOMER)"), screen);
        assertTrue(screen.contains("Deposit successful. New balance: ₹10,500.00"), screen);
        assertTrue(screen.contains("Insufficient funds"), screen);
        assertTrue(screen.contains("DEPOSIT"), screen); // transaction history was shown
        assertTrue(screen.contains("Account not found or you do not have access to it."), screen);
        assertTrue(screen.contains("Unknown option"), screen); // admin-only "13" is not offered to customers
        assertTrue(screen.contains("Goodbye"), screen);
        assertFalse(screen.contains("Admin@1234"), "typed passwords are never echoed back");
    }

    @Test
    void threeWrongPasswordsEndTheProgram() {
        BankService bank = new BankService(new InMemoryDataStore());
        bank.createFirstAdmin("admin", "Admin@1234");
        String screen = run(bank,
                "1", "admin", "bad1pass1", "admin", "bad2pass2", "admin", "bad3pass3");
        assertTrue(screen.contains("Too many failed attempts"), screen);
        assertFalse(screen.contains("Welcome"), screen);
    }

    @Test
    void firstRunSetupRejectsMismatchedAndWeakPasswordsThenAccepts() {
        BankService bank = new BankService(new InMemoryDataStore());
        String screen = run(bank,
                "admin", "Admin@1234", "Different@1",
                "admin", "weak", "weak",
                "admin", "Admin@1234", "Admin@1234",
                "0");
        assertTrue(screen.contains("Passwords do not match"), screen);
        assertTrue(screen.contains("Password must be 8-128 characters"), screen);
        assertTrue(screen.contains("Administrator created"), screen);
        assertFalse(bank.isFirstRun());
    }

    @Test
    void endOfInputExitsCleanly() {
        BankService bank = new BankService(new InMemoryDataStore());
        String screen = run(bank, "admin", "Admin@1234", "Admin@1234", "1", "admin");
        assertTrue(screen.contains("Goodbye"), screen);
    }
}
