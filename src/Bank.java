import java.util.ArrayList;
import java.util.List;

/**
 * Bank.java
 *
 * Implements BankOperations. This is the heart of the business logic:
 * it owns the collection of accounts and enforces the rules for
 * opening, depositing, withdrawing, searching, and closing accounts.
 *
 * OOP concepts demonstrated:
 *  - Encapsulation: the ArrayList of accounts is private; the outside
 *    world can only interact with it through the methods defined by
 *    BankOperations.
 *  - Polymorphism: openAccount() creates either a SavingsAccount or a
 *    CurrentAccount but stores/returns/manipulates it via the Account
 *    reference type. deposit()/withdraw() call account.withdraw(...)
 *    without caring which concrete subclass it is - the correct
 *    overridden method runs automatically.
 *
 * Java Collections:
 *  - ArrayList<Account> is used as the primary storage structure because
 *    the project needs ordered, index-accessible storage that supports
 *    frequent iteration (for "view all accounts") and simple linear
 *    search (the account count in a console app is small, so O(n)
 *    search is perfectly fine and avoids the overhead of a Map wrapper
 *    while still keeping the code simple to read).
 *  - add() is used in openAccount(), remove() is used in closeAccount(),
 *    iteration (for-each) is used throughout, and searching is used in
 *    getAccount()/searchAccounts().
 */
public class Bank implements BankOperations {

    private final List<Account> accounts = new ArrayList<>();
    private int nextAccountNumber = 1001;

    private String generateAccountNumber() {
        return "ACC" + (nextAccountNumber++);
    }

    @Override
    public Account openAccount(String holderName, String phoneNumber, String accountType, double initialDeposit)
            throws InvalidInputException {

        if (ValidationUtil.isNullOrEmpty(holderName)) {
            throw new InvalidInputException("Account holder name cannot be empty.");
        }
        if (!ValidationUtil.isValidPhoneNumber(phoneNumber)) {
            throw new InvalidInputException("Phone number is invalid. Expected a 10-digit number (optional +country code).");
        }
        if (!ValidationUtil.isValidAccountType(accountType)) {
            throw new InvalidInputException("Account type must be either 'Savings' or 'Current'.");
        }
        if (!ValidationUtil.isNonNegative(initialDeposit)) {
            throw new InvalidInputException("Initial deposit cannot be negative.");
        }

        String accountNumber = generateAccountNumber();

        Account account;
        if ("SAVINGS".equalsIgnoreCase(accountType)) {
            account = new SavingsAccount(accountNumber, holderName, phoneNumber, initialDeposit);
        } else {
            account = new CurrentAccount(accountNumber, holderName, phoneNumber, initialDeposit);
        }

        accounts.add(account); // ArrayList.add()
        return account;
    }

    @Override
    public void deposit(String accountNumber, double amount) throws AccountNotFoundException, InvalidInputException {
        if (!ValidationUtil.isPositive(amount)) {
            throw new InvalidInputException("Deposit amount must be greater than zero.");
        }
        Account account = getAccount(accountNumber);
        account.deposit(amount);
    }

    @Override
    public void withdraw(String accountNumber, double amount)
            throws AccountNotFoundException, InvalidInputException, InsufficientBalanceException {
        if (!ValidationUtil.isPositive(amount)) {
            throw new InvalidInputException("Withdrawal amount must be greater than zero.");
        }
        Account account = getAccount(accountNumber);
        account.withdraw(amount); // polymorphic call
    }

    @Override
    public Account getAccount(String accountNumber) throws AccountNotFoundException {
        for (Account account : accounts) { // iteration
            if (account.getAccountNumber().equalsIgnoreCase(accountNumber)) {
                return account;
            }
        }
        throw new AccountNotFoundException("No account found with account number: " + accountNumber);
    }

    @Override
    public List<Account> getAllAccounts() {
        return new ArrayList<>(accounts); // defensive copy: caller can't mutate internal list
    }

    @Override
    public List<Account> searchAccounts(String keyword) {
        List<Account> results = new ArrayList<>();
        if (ValidationUtil.isNullOrEmpty(keyword)) {
            return results;
        }
        String lowerKeyword = keyword.toLowerCase();
        for (Account account : accounts) { // searching by iteration
            if (account.getAccountNumber().toLowerCase().contains(lowerKeyword)
                    || account.getAccountHolderName().toLowerCase().contains(lowerKeyword)) {
                results.add(account);
            }
        }
        return results;
    }

    @Override
    public void closeAccount(String accountNumber) throws AccountNotFoundException {
        Account account = getAccount(accountNumber);
        accounts.remove(account); // ArrayList.remove()
    }

    public boolean accountNumberExists(String accountNumber) {
        for (Account account : accounts) {
            if (account.getAccountNumber().equalsIgnoreCase(accountNumber)) {
                return true;
            }
        }
        return false;
    }
}
