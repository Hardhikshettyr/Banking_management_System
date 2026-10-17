package com.bank.ui;

import com.bank.exception.AuthenticationException;
import com.bank.exception.BankException;
import com.bank.exception.InvalidAmountException;
import com.bank.exception.InvalidInputException;
import com.bank.model.AccountType;
import com.bank.model.BankAccount;
import com.bank.model.Customer;
import com.bank.model.Role;
import com.bank.model.Transaction;
import com.bank.model.User;
import com.bank.service.BankService;
import com.bank.service.SecureBankService;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * Menu-driven console front end: first-run setup, login, then an admin or customer menu.
 * It contains no business or security rules; those live in the service layer.
 */
public class ConsoleApp {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final int MAX_LOGIN_ATTEMPTS = 3;

    private final BankService bank;
    private final Scanner in;
    private final PrintStream out;
    private final boolean hidePasswords;

    /**
     * @param hidePasswords true to read passwords without echoing them (needs a real terminal)
     */
    public ConsoleApp(BankService bank, Scanner in, PrintStream out, boolean hidePasswords) {
        this.bank = bank;
        this.in = in;
        this.out = out;
        this.hidePasswords = hidePasswords;
    }

    /** Thrown when the input ends (Ctrl+D / Ctrl+Z) so the program can exit cleanly. */
    private static final class EndOfInput extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public void run() {
        out.println("==============================================");
        out.println("   Java Banking Management System");
        out.println("==============================================");
        try {
            if (bank.isFirstRun()) {
                firstRunSetup();
            }
            while (true) {
                User user = loginScreen();
                if (user == null) {
                    break;
                }
                SecureBankService session = new SecureBankService(bank, user);
                out.println("\nWelcome, " + user.getUsername() + " (" + user.getRole() + ").");
                if (user.getRole() == Role.ADMIN) {
                    menuLoop(session, true);
                } else {
                    menuLoop(session, false);
                }
                out.println("\nYou have been logged out.");
            }
        } catch (EndOfInput e) {
            out.println();
        }
        out.println("Goodbye! All data has been saved.");
    }

    // ----------------------------------------------------------- setup and login

    private void firstRunSetup() {
        out.println("\nFirst-time setup: create the administrator login.");
        while (true) {
            try {
                String username = prompt("Admin username");
                String password = promptPassword("Choose a password (8+ characters, letters and digits)");
                if (!password.equals(promptPassword("Confirm password"))) {
                    throw new InvalidInputException("Passwords do not match.");
                }
                bank.createFirstAdmin(username, password);
                out.println("\nAdministrator created. Please log in.");
                return;
            } catch (BankException e) {
                out.println("\n[Error] " + e.getMessage());
            }
        }
    }

    /** @return the logged-in user, or null if the user chose to exit */
    private User loginScreen() {
        while (true) {
            out.println();
            out.println("1. Login");
            out.println("0. Exit");
            String choice = prompt("Choose an option").trim();
            if ("0".equals(choice)) {
                return null;
            }
            if (!"1".equals(choice)) {
                out.println("\nUnknown option. Please choose from the menu.");
                continue;
            }
            for (int attempt = 1; attempt <= MAX_LOGIN_ATTEMPTS; attempt++) {
                String username = prompt("Username");
                String password = promptPassword("Password");
                try {
                    return bank.authenticate(username, password);
                } catch (AuthenticationException e) {
                    out.println("\n[Error] " + e.getMessage() + " (attempt " + attempt + " of "
                            + MAX_LOGIN_ATTEMPTS + ")");
                }
            }
            out.println("\nToo many failed attempts. Exiting.");
            return null;
        }
    }

    // --------------------------------------------------------------------- menus

    private void menuLoop(SecureBankService s, boolean admin) {
        boolean running = true;
        while (running) {
            if (admin) {
                printAdminMenu();
            } else {
                printCustomerMenu();
            }
            String choice = prompt("Choose an option").trim();
            try {
                running = admin ? handleAdmin(s, choice) : handleCustomer(s, choice);
            } catch (BankException e) {
                out.println("\n[Error] " + e.getMessage());
            }
        }
    }

    private void printAdminMenu() {
        out.println();
        out.println("1. Create customer        8. Search accounts");
        out.println("2. Open account           9. List all accounts");
        out.println("3. Deposit               10. Apply monthly interest (savings)");
        out.println("4. Withdraw              11. Create customer login");
        out.println("5. Transfer              12. Create admin login");
        out.println("6. View account details  13. Change my password");
        out.println("7. Transaction history    0. Logout");
    }

    private void printCustomerMenu() {
        out.println();
        out.println("1. My accounts            5. Transfer");
        out.println("2. Account details        6. Transaction history");
        out.println("3. Deposit                7. Change my password");
        out.println("4. Withdraw               0. Logout");
    }

    /** @return false to log out */
    private boolean handleAdmin(SecureBankService s, String choice) {
        switch (choice) {
            case "1" -> createCustomer(s);
            case "2" -> openAccount(s);
            case "3" -> deposit(s);
            case "4" -> withdraw(s);
            case "5" -> transfer(s);
            case "6" -> viewAccount(s);
            case "7" -> viewHistory(s);
            case "8" -> printAccounts(s.search(prompt("Search (account no., customer ID, or name)")));
            case "9" -> printAccounts(s.listMyAccounts());
            case "10" -> applyInterest(s);
            case "11" -> createCustomerLogin(s);
            case "12" -> createAdminLogin(s);
            case "13" -> changePassword(s);
            case "0" -> { return false; }
            default -> out.println("\nUnknown option. Please choose from the menu.");
        }
        return true;
    }

    private boolean handleCustomer(SecureBankService s, String choice) {
        switch (choice) {
            case "1" -> printAccounts(s.listMyAccounts());
            case "2" -> viewAccount(s);
            case "3" -> deposit(s);
            case "4" -> withdraw(s);
            case "5" -> transfer(s);
            case "6" -> viewHistory(s);
            case "7" -> changePassword(s);
            case "0" -> { return false; }
            default -> out.println("\nUnknown option. Please choose from the menu.");
        }
        return true;
    }

    // ------------------------------------------------------------------- actions

    private void createCustomer(SecureBankService s) {
        String name = prompt("Full name");
        String email = prompt("Email");
        String phone = prompt("Phone (10 digits)");
        Customer c = s.createCustomer(name, email, phone);
        out.println("\nCustomer created. Customer ID: " + c.getCustomerId());
    }

    private void openAccount(SecureBankService s) {
        String customerId = prompt("Customer ID");
        AccountType type = switch (prompt("Account type (1 = Savings, 2 = Current)").trim()) {
            case "1" -> AccountType.SAVINGS;
            case "2" -> AccountType.CURRENT;
            default -> throw new InvalidInputException("Account type must be 1 or 2.");
        };
        BigDecimal initial = readAmount("Opening deposit (₹, 0 for none)", true);
        BankAccount a = s.openAccount(customerId, type, initial);
        out.println("\nAccount opened. Account number: " + a.getAccountNumber());
        out.printf("Balance: ₹%,.2f%n", a.getBalance());
    }

    private void deposit(SecureBankService s) {
        String acc = prompt("Account number");
        BankAccount a = s.deposit(acc, readAmount("Amount to deposit (₹)", false));
        out.printf("%nDeposit successful. New balance: ₹%,.2f%n", a.getBalance());
    }

    private void withdraw(SecureBankService s) {
        String acc = prompt("Account number");
        BankAccount a = s.withdraw(acc, readAmount("Amount to withdraw (₹)", false));
        out.printf("%nWithdrawal successful. New balance: ₹%,.2f%n", a.getBalance());
    }

    private void transfer(SecureBankService s) {
        String from = prompt("From account number");
        String to = prompt("To account number");
        BigDecimal amount = readAmount("Amount to transfer (₹)", false);
        s.transfer(from, to, amount);
        out.printf("%nTransfer successful. New balance of %s: ₹%,.2f%n",
                from.trim().toUpperCase(), s.getAccount(from).getBalance());
    }

    private void viewAccount(SecureBankService s) {
        BankAccount a = s.getAccount(prompt("Account number"));
        out.println();
        out.println("Account number : " + a.getAccountNumber());
        out.println("Type           : " + a.getType());
        out.println("Holder         : " + a.getCustomer().getName() + " (" + a.getCustomer().getCustomerId() + ")");
        out.println("Email / Phone  : " + a.getCustomer().getEmail() + " / " + a.getCustomer().getPhone());
        out.println("Opened on      : " + a.getOpenedOn());
        out.printf("Balance        : ₹%,.2f%n", a.getBalance());
        out.printf("Available      : ₹%,.2f%n", a.getAvailableBalance());
    }

    private void viewHistory(SecureBankService s) {
        List<Transaction> history = s.getHistory(prompt("Account number"));
        out.println();
        if (history.isEmpty()) {
            out.println("No transactions yet.");
            return;
        }
        out.printf("%-9s %-20s %-13s %14s %14s  %s%n",
                "ID", "Date/Time", "Type", "Amount", "Balance", "Other account");
        for (Transaction t : history) {
            out.printf("%-9s %-20s %-13s %,14.2f %,14.2f  %s%n",
                    t.getId(), t.getTimestamp().format(TS), t.getType(),
                    t.getAmount(), t.getBalanceAfter(),
                    t.getCounterpartyAccount() == null ? "-" : t.getCounterpartyAccount());
        }
    }

    private void applyInterest(SecureBankService s) {
        String choice = prompt("Apply to (1 = one account, 2 = all savings accounts)").trim();
        if ("1".equals(choice)) {
            BigDecimal interest = s.applyInterest(prompt("Account number"));
            if (interest.signum() > 0) {
                out.printf("%nInterest credited: ₹%,.2f%n", interest);
            } else {
                out.println("\nNo interest applicable for this account.");
            }
        } else if ("2".equals(choice)) {
            out.println("\nInterest credited to " + s.applyInterestToAll() + " account(s).");
        } else {
            throw new InvalidInputException("Please choose 1 or 2.");
        }
    }

    private void createCustomerLogin(SecureBankService s) {
        String customerId = prompt("Customer ID");
        String username = prompt("New username");
        String password = promptNewPassword("Password for the customer (8+ characters, letters and digits)");
        User u = s.createCustomerLogin(customerId, username, password);
        out.println("\nLogin created: " + u.getUsername() + " for " + u.getCustomerId());
    }

    private void createAdminLogin(SecureBankService s) {
        String username = prompt("New admin username");
        String password = promptNewPassword("Password (8+ characters, letters and digits)");
        User u = s.createAdminLogin(username, password);
        out.println("\nAdmin login created: " + u.getUsername());
    }

    private void changePassword(SecureBankService s) {
        String oldPassword = promptPassword("Current password");
        String newPassword = promptNewPassword("New password (8+ characters, letters and digits)");
        s.changeOwnPassword(oldPassword, newPassword);
        out.println("\nPassword changed.");
    }

    // ------------------------------------------------------------------- helpers

    private void printAccounts(List<BankAccount> accounts) {
        out.println();
        if (accounts.isEmpty()) {
            out.println("No accounts found.");
            return;
        }
        out.printf("%-11s %-8s %-25s %16s%n", "Account", "Type", "Holder", "Balance");
        for (BankAccount a : accounts) {
            out.printf("%-11s %-8s %-25s %,16.2f%n", a.getAccountNumber(), a.getType(),
                    a.getCustomer().getName(), a.getBalance());
        }
    }

    private String prompt(String label) {
        out.print(label + ": ");
        out.flush();
        if (!in.hasNextLine()) {
            throw new EndOfInput();
        }
        return in.nextLine();
    }

    private String promptPassword(String label) {
        if (hidePasswords && System.console() != null) {
            char[] chars = System.console().readPassword("%s: ", label);
            if (chars == null) {
                throw new EndOfInput();
            }
            String password = new String(chars);
            java.util.Arrays.fill(chars, ' ');
            return password;
        }
        return prompt(label);
    }

    private String promptNewPassword(String label) {
        String password = promptPassword(label);
        if (!password.equals(promptPassword("Confirm password"))) {
            throw new InvalidInputException("Passwords do not match.");
        }
        return password;
    }

    private BigDecimal readAmount(String label, boolean allowZero) {
        String raw = prompt(label).trim().replace(",", "");
        if (raw.isEmpty() && allowZero) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw new InvalidAmountException("\"" + raw + "\" is not a valid number.");
        }
    }
}
