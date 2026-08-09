/**
 * SavingsAccount.java
 *
 * Concrete Account type representing a savings account.
 *
 * Business rule specific to Savings accounts: a minimum balance of
 * MIN_BALANCE must always be maintained. Withdrawals that would drop
 * the balance below this minimum are rejected.
 *
 * OOP concepts demonstrated:
 *  - Inheritance: extends Account, reusing deposit(), getters/setters.
 *  - Polymorphism: overrides withdraw() and getAccountType() with
 *    behavior specific to savings accounts. When a list of Account
 *    references is iterated and withdraw() is called on each, the JVM
 *    dispatches to THIS implementation for any SavingsAccount object.
 */
public class SavingsAccount extends Account {

    private static final double MIN_BALANCE = 500.0;

    public SavingsAccount(String accountNumber, String accountHolderName, String phoneNumber, double balance) {
        super(accountNumber, accountHolderName, phoneNumber, balance);
    }

    @Override
    public void withdraw(double amount) throws InsufficientBalanceException {
        if (getBalance() - amount < MIN_BALANCE) {
            throw new InsufficientBalanceException(
                    "Withdrawal denied: Savings account must maintain a minimum balance of " + MIN_BALANCE);
        }
        setBalance(getBalance() - amount);
    }

    @Override
    public String getAccountType() {
        return "Savings";
    }
}
