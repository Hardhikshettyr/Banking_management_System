/**
 * CurrentAccount.java
 *
 * Concrete Account type representing a current (checking) account.
 *
 * Business rule specific to Current accounts: no minimum balance is
 * enforced; the account can be withdrawn down to exactly zero, but
 * never allowed to go negative (no overdraft in this project).
 *
 * OOP concepts demonstrated:
 *  - Inheritance: extends Account.
 *  - Polymorphism: provides its own withdraw() rule that differs from
 *    SavingsAccount, proving that the same method call behaves
 *    differently depending on the actual object type at runtime.
 */
public class CurrentAccount extends Account {

    public CurrentAccount(String accountNumber, String accountHolderName, String phoneNumber, double balance) {
        super(accountNumber, accountHolderName, phoneNumber, balance);
    }

    @Override
    public void withdraw(double amount) throws InsufficientBalanceException {
        if (amount > getBalance()) {
            throw new InsufficientBalanceException(
                    "Withdrawal denied: amount exceeds available balance.");
        }
        setBalance(getBalance() - amount);
    }

    @Override
    public String getAccountType() {
        return "Current";
    }
}
