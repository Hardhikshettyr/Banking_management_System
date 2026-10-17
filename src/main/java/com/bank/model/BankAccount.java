package com.bank.model;

import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidInputException;
import com.bank.util.Validator;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base for all accounts.
 *
 * <p>Encapsulation: the balance and transaction list are private. The only way to change
 * them is through validated operations (deposit, withdraw, transferTo, interest).
 * Subclasses supply account-specific rules via the abstract methods.
 */
public abstract class BankAccount implements Transferable, Serializable {
    private static final long serialVersionUID = 1L;

    private final String accountNumber;
    private final Customer customer;
    private final LocalDate openedOn;
    private BigDecimal balance = BigDecimal.ZERO.setScale(2);
    private final List<Transaction> transactions = new ArrayList<>();

    protected BankAccount(String accountNumber, Customer customer) {
        this(accountNumber, customer, LocalDate.now());
    }

    protected BankAccount(String accountNumber, Customer customer, LocalDate openedOn) {
        if (customer == null) {
            throw new InvalidInputException("An account must belong to a customer.");
        }
        this.accountNumber = accountNumber;
        this.customer = customer;
        this.openedOn = openedOn;
    }

    /**
     * Rebuilds an account from stored data (used by persistence only). The balance is
     * recomputed from the history, so the transaction list stays the single source of truth.
     */
    public static BankAccount restore(AccountType type, String accountNumber, Customer customer,
                                      LocalDate openedOn, List<Transaction> history) {
        BankAccount account = switch (type) {
            case SAVINGS -> new SavingsAccount(accountNumber, customer, openedOn);
            case CURRENT -> new CurrentAccount(accountNumber, customer, openedOn);
        };
        account.restoreHistory(history);
        return account;
    }

    private void restoreHistory(List<Transaction> history) {
        BigDecimal restored = BigDecimal.ZERO.setScale(2);
        for (Transaction t : history) {
            switch (t.getType()) {
                case DEPOSIT, TRANSFER_IN, INTEREST -> restored = restored.add(t.getAmount());
                case WITHDRAWAL, TRANSFER_OUT -> restored = restored.subtract(t.getAmount());
            }
            transactions.add(t);
        }
        balance = restored;
    }

    // ----- account-specific rules (polymorphism) -----

    public abstract AccountType getType();

    /** Lowest balance the account may hold after a debit (negative means overdraft allowed). */
    public abstract BigDecimal getMinimumAllowedBalance();

    /** Minimum deposit required when the account is opened. */
    public abstract BigDecimal getMinimumOpeningDeposit();

    /** Monthly interest hook; accounts that do not earn interest return zero. */
    public BigDecimal applyMonthlyInterest() {
        return BigDecimal.ZERO.setScale(2);
    }

    // ----- operations -----

    public void deposit(BigDecimal amount) {
        credit(amount, TransactionType.DEPOSIT, null, "Deposit");
    }

    public void withdraw(BigDecimal amount) {
        debit(amount, TransactionType.WITHDRAWAL, null, "Withdrawal");
    }

    @Override
    public void transferTo(BankAccount target, BigDecimal amount) {
        if (target == null) {
            throw new InvalidInputException("Destination account is required.");
        }
        if (target == this || target.accountNumber.equals(this.accountNumber)) {
            throw new InvalidInputException("Cannot transfer to the same account.");
        }
        BigDecimal amt = Validator.requireValidAmount(amount);
        ensureCanDebit(amt); // validate before touching either balance
        debit(amt, TransactionType.TRANSFER_OUT, target.accountNumber,
                "Transfer to " + target.accountNumber);
        target.credit(amt, TransactionType.TRANSFER_IN, this.accountNumber,
                "Transfer from " + this.accountNumber);
    }

    /** Lets subclasses post interest without exposing the balance setter. */
    protected final void creditInterest(BigDecimal interest) {
        credit(interest, TransactionType.INTEREST, null, "Interest credited");
    }

    private void credit(BigDecimal amount, TransactionType type, String counterparty, String description) {
        BigDecimal amt = Validator.requireValidAmount(amount);
        balance = balance.add(amt);
        transactions.add(new Transaction(type, amt, accountNumber, counterparty, balance, description));
    }

    private void debit(BigDecimal amount, TransactionType type, String counterparty, String description) {
        BigDecimal amt = Validator.requireValidAmount(amount);
        ensureCanDebit(amt);
        balance = balance.subtract(amt);
        transactions.add(new Transaction(type, amt, accountNumber, counterparty, balance, description));
    }

    private void ensureCanDebit(BigDecimal amt) {
        if (balance.subtract(amt).compareTo(getMinimumAllowedBalance()) < 0) {
            throw new InsufficientFundsException(String.format(
                    "Insufficient funds: available to withdraw ₹%,.2f, requested ₹%,.2f.",
                    getAvailableBalance(), amt));
        }
    }

    // ----- read-only views -----

    /** Amount that can currently be withdrawn (never negative). */
    public BigDecimal getAvailableBalance() {
        return balance.subtract(getMinimumAllowedBalance()).max(BigDecimal.ZERO).setScale(2);
    }

    public BigDecimal getBalance() { return balance; }
    public String getAccountNumber() { return accountNumber; }
    public Customer getCustomer() { return customer; }
    public LocalDate getOpenedOn() { return openedOn; }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | ₹%,.2f", accountNumber, getType(),
                customer.getName(), balance);
    }
}
