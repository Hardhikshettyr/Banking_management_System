package com.bank.service;

import com.bank.exception.AccessDeniedException;
import com.bank.exception.PersistenceException;
import com.bank.model.AccountType;
import com.bank.model.BankAccount;
import com.bank.model.Customer;
import com.bank.model.Role;
import com.bank.model.Transaction;
import com.bank.model.User;
import com.bank.persistence.BankData;
import com.bank.persistence.DataStore;

import java.math.BigDecimal;
import java.util.List;

/**
 * Core facade. Composes the account, transaction and auth services and saves to the
 * {@link DataStore} after every successful change.
 *
 * <p>If saving fails (for example the database is down), the in-memory state is reloaded from
 * the store so memory and storage never silently disagree. This class does not check who is
 * calling; the UI goes through {@link SecureBankService}, which does.
 */
public class BankService {

    private final DataStore store;
    private BankData data;
    private AccountService accountService;
    private TransactionService transactionService;
    private AuthService authService;

    public BankService(DataStore store) {
        this.store = store;
        rebuild(store.load());
    }

    private void rebuild(BankData loaded) {
        this.data = loaded;
        this.accountService = new AccountService(loaded);
        this.transactionService = new TransactionService(accountService);
        this.authService = new AuthService(loaded);
    }

    /** Saves the current state; on failure discards the unsaved change by reloading. */
    private void persist() {
        try {
            store.save(data);
        } catch (PersistenceException e) {
            String outcome;
            try {
                rebuild(store.load());
                outcome = "The change was not saved and has been undone. ";
            } catch (RuntimeException reloadFailure) {
                outcome = "The change was not saved. Restart the application before continuing. ";
            }
            throw new PersistenceException(outcome + e.getMessage(), e);
        }
    }

    // ----- accounts and money -----

    public Customer createCustomer(String name, String email, String phone) {
        Customer c = accountService.createCustomer(name, email, phone);
        persist();
        return c;
    }

    public BankAccount openAccount(String customerId, AccountType type, BigDecimal initialDeposit) {
        BankAccount a = accountService.openAccount(customerId, type, initialDeposit);
        persist();
        return a;
    }

    public BankAccount deposit(String accountNumber, BigDecimal amount) {
        BankAccount a = transactionService.deposit(accountNumber, amount);
        persist();
        return a;
    }

    public BankAccount withdraw(String accountNumber, BigDecimal amount) {
        BankAccount a = transactionService.withdraw(accountNumber, amount);
        persist();
        return a;
    }

    public void transfer(String from, String to, BigDecimal amount) {
        transactionService.transfer(from, to, amount);
        persist();
    }

    public BigDecimal applyInterest(String accountNumber) {
        BigDecimal interest = transactionService.applyInterest(accountNumber);
        persist();
        return interest;
    }

    public int applyInterestToAll() {
        int count = transactionService.applyInterestToAll();
        persist();
        return count;
    }

    public BankAccount getAccount(String accountNumber) {
        return accountService.findAccount(accountNumber);
    }

    public List<Transaction> getHistory(String accountNumber) {
        return transactionService.getHistory(accountNumber);
    }

    public List<BankAccount> search(String query) {
        return accountService.search(query);
    }

    public List<BankAccount> listAccounts() {
        return accountService.listAccounts();
    }

    // ----- logins -----

    /** True until the first administrator has been created. */
    public boolean isFirstRun() {
        return !authService.hasUsers();
    }

    public User createFirstAdmin(String username, String password) {
        if (authService.hasUsers()) {
            throw new AccessDeniedException("Setup has already been completed.");
        }
        return createUser(username, password, Role.ADMIN, null);
    }

    public User createUser(String username, String password, Role role, String customerId) {
        User u = authService.createUser(username, password, role, customerId);
        persist();
        return u;
    }

    public User authenticate(String username, String password) {
        return authService.authenticate(username, password);
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        authService.changePassword(username, oldPassword, newPassword);
        persist();
    }
}
