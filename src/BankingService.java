import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

/**
 * BankingService.java
 *
 * Sits between Main (which just starts the program) and Bank (which
 * owns the business rules). This class is responsible for:
 *   - printing the menu,
 *   - reading and lightly parsing console input,
 *   - calling the right Bank/BankOperations method,
 *   - catching exceptions and printing user-friendly messages.
 *
 * Why separate this from Bank?
 *  - Bank should be reusable and testable without a console attached
 *    (e.g. it could power a GUI or a web API later). Mixing
 *    System.out.println and Scanner calls into Bank would make that
 *    impossible. This is the "keep business logic separate from
 *    menu/input logic" requirement.
 *
 * Java concepts used: Scanner for console input, try/catch for
 * InputMismatchException and NumberFormatException, a while(true) loop
 * driving the menu until the user exits.
 */
public class BankingService {

    private final BankOperations bank;
    private final Scanner scanner;

    public BankingService(BankOperations bank, Scanner scanner) {
        this.bank = bank;
        this.scanner = scanner;
    }

    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readMenuChoice();
            switch (choice) {
                case 1 -> handleOpenAccount();
                case 2 -> handleDeposit();
                case 3 -> handleWithdraw();
                case 4 -> handleCheckBalance();
                case 5 -> handleViewAccountDetails();
                case 6 -> handleViewAllAccounts();
                case 7 -> handleSearchAccount();
                case 8 -> handleCloseAccount();
                case 9 -> {
                    System.out.println("Thank you for using the Banking Management System. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please enter a number between 1 and 9.");
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("===== Banking Management System =====");
        System.out.println("1. Open Account");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. Check Balance");
        System.out.println("5. View Account Details");
        System.out.println("6. View All Accounts");
        System.out.println("7. Search Account");
        System.out.println("8. Close Account");
        System.out.println("9. Exit");
        System.out.print("Enter your choice: ");
    }

    /**
     * Reads the menu choice defensively. If the user types something that
     * is not an integer, Scanner.nextInt() throws InputMismatchException.
     * We catch it, clear the bad token, and return an invalid choice code
     * (-1) so the outer loop just reprints the menu instead of crashing.
     */
    private int readMenuChoice() {
        try {
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume leftover newline
            return choice;
        } catch (InputMismatchException e) {
            System.out.println("Invalid input. Please enter a number.");
            scanner.nextLine(); // discard the invalid token
            return -1;
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    /**
     * Reads a double from the console. Throws NumberFormatException up to
     * the caller if the text isn't a valid number, so each handler can
     * decide how to react (usually: print an error and return to the menu).
     */
    private double readAmount(String prompt) {
        String input = readLine(prompt);
        return Double.parseDouble(input); // may throw NumberFormatException
    }

    private void handleOpenAccount() {
        System.out.println("--- Open New Account ---");
        try {
            String name = readLine("Enter account holder name: ");
            String phone = readLine("Enter phone number: ");
            String type = readLine("Enter account type (Savings/Current): ");
            double initialDeposit = readAmount("Enter initial deposit: ");

            Account account = bank.openAccount(name, phone, type, initialDeposit);
            System.out.println("Account created successfully!");
            System.out.println(account);
        } catch (InvalidInputException e) {
            System.out.println("Could not open account: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount entered. Please enter a valid number.");
        }
    }

    private void handleDeposit() {
        System.out.println("--- Deposit ---");
        try {
            String accNo = readLine("Enter account number: ");
            double amount = readAmount("Enter amount to deposit: ");
            bank.deposit(accNo, amount);
            System.out.println("Deposit successful. New balance: " + bank.getAccount(accNo).getBalance());
        } catch (AccountNotFoundException | InvalidInputException e) {
            System.out.println("Deposit failed: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount entered. Please enter a valid number.");
        }
    }

    private void handleWithdraw() {
        System.out.println("--- Withdraw ---");
        try {
            String accNo = readLine("Enter account number: ");
            double amount = readAmount("Enter amount to withdraw: ");
            bank.withdraw(accNo, amount);
            System.out.println("Withdrawal successful. New balance: " + bank.getAccount(accNo).getBalance());
        } catch (AccountNotFoundException | InvalidInputException | InsufficientBalanceException e) {
            System.out.println("Withdrawal failed: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount entered. Please enter a valid number.");
        }
    }

    private void handleCheckBalance() {
        System.out.println("--- Check Balance ---");
        String accNo = readLine("Enter account number: ");
        try {
            Account account = bank.getAccount(accNo);
            System.out.printf("Balance for %s (%s): %.2f%n",
                    account.getAccountHolderName(), account.getAccountNumber(), account.getBalance());
        } catch (AccountNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleViewAccountDetails() {
        System.out.println("--- Account Details ---");
        String accNo = readLine("Enter account number: ");
        try {
            Account account = bank.getAccount(accNo);
            System.out.println(account);
        } catch (AccountNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleViewAllAccounts() {
        System.out.println("--- All Accounts ---");
        List<Account> accounts = bank.getAllAccounts();
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
            return;
        }
        for (Account account : accounts) {
            System.out.println(account);
        }
    }

    private void handleSearchAccount() {
        System.out.println("--- Search Account ---");
        String keyword = readLine("Enter account number or holder name to search: ");
        List<Account> results = bank.searchAccounts(keyword);
        if (results.isEmpty()) {
            System.out.println("No matching accounts found.");
            return;
        }
        for (Account account : results) {
            System.out.println(account);
        }
    }

    private void handleCloseAccount() {
        System.out.println("--- Close Account ---");
        String accNo = readLine("Enter account number to close: ");
        try {
            bank.closeAccount(accNo);
            System.out.println("Account " + accNo + " closed successfully.");
        } catch (AccountNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }
}
