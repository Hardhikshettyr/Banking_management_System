package com.bank.service;

import com.bank.model.BankAccount;
import com.bank.model.Transaction;

import java.math.BigDecimal;
import java.util.List;

/** Money movement. Looks accounts up, then delegates the rule enforcement to the account. */
public class TransactionService {

    private final AccountService accounts;

    public TransactionService(AccountService accounts) {
        this.accounts = accounts;
    }

    public BankAccount deposit(String accountNumber, BigDecimal amount) {
        BankAccount account = accounts.findAccount(accountNumber);
        account.deposit(amount);
        return account;
    }

    public BankAccount withdraw(String accountNumber, BigDecimal amount) {
        BankAccount account = accounts.findAccount(accountNumber);
        account.withdraw(amount);
        return account;
    }

    public void transfer(String fromAccount, String toAccount, BigDecimal amount) {
        // Resolve both accounts before changing anything: a missing account leaves state untouched.
        BankAccount from = accounts.findAccount(fromAccount);
        BankAccount to = accounts.findAccount(toAccount);
        from.transferTo(to, amount);
    }

    public List<Transaction> getHistory(String accountNumber) {
        return accounts.findAccount(accountNumber).getTransactions();
    }

    /** Credits one month of interest; returns the amount (zero for accounts without interest). */
    public BigDecimal applyInterest(String accountNumber) {
        return accounts.findAccount(accountNumber).applyMonthlyInterest();
    }

    /** Credits interest to every account that earns it; returns how many were credited. */
    public int applyInterestToAll() {
        int count = 0;
        for (BankAccount a : accounts.listAccounts()) {
            if (a.applyMonthlyInterest().signum() > 0) {
                count++;
            }
        }
        return count;
    }
}
