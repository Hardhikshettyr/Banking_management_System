/**
 * Account.java
 *
 * Abstract base class representing a generic bank account.
 * Defines the common state (account number, holder name, phone, balance)
 * and behavior (deposit, withdraw) shared by every account type.
 *
 * OOP concepts demonstrated:
 *  - Encapsulation: all fields are private, accessed only via getters/setters.
 *  - Abstraction: getAccountType() is abstract; subclasses must define what
 *    "type" of account they are. Account itself can never be instantiated.
 *  - Inheritance: SavingsAccount and CurrentAccount extend this class.
 */
public abstract class Account {

    private final String accountNumber;
    private String accountHolderName;
    private String phoneNumber;
    private double balance;

    public Account(String accountNumber, String accountHolderName, String phoneNumber, double balance) {
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.phoneNumber = phoneNumber;
        this.balance = balance;
    }

    // ---------- Getters ----------
    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public double getBalance() {
        return balance;
    }

    // ---------- Setters (controlled access) ----------
    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Protected so only Account and its subclasses can directly change
     * balance. External classes must go through deposit()/withdraw().
     */
    protected void setBalance(double balance) {
        this.balance = balance;
    }

    /**
     * Deposits an amount into the account.
     * Common to all account types, so it lives here rather than being
     * duplicated in every subclass.
     */
    public void deposit(double amount) {
        this.balance += amount;
    }

    /**
     * Withdraws an amount from the account.
     * Declared abstract because different account types may apply
     * different withdrawal rules (e.g. a Current account might allow
     * overdraft in the future, while Savings never does). This is the
     * classic hook for polymorphism.
     */
    public abstract void withdraw(double amount) throws InsufficientBalanceException;

    /**
     * Abstract method that forces every subclass to state its own type.
     * This is the abstraction: calling code can treat any Account
     * polymorphically and still ask "what type are you?"
     */
    public abstract String getAccountType();

    @Override
    public String toString() {
        return String.format(
                "Account No: %-10s | Holder: %-20s | Type: %-10s | Phone: %-12s | Balance: %.2f",
                accountNumber, accountHolderName, getAccountType(), phoneNumber, balance
        );
    }
}
