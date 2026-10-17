package com.bank.service;

import com.bank.exception.AccessDeniedException;
import com.bank.exception.AccountNotFoundException;
import com.bank.model.AccountType;
import com.bank.model.BankAccount;
import com.bank.model.Customer;
import com.bank.model.Role;
import com.bank.model.Transaction;
import com.bank.model.User;

import java.math.BigDecimal;
import java.util.List;

/**
 * The bank as seen by one signed-in user. Every operation is authorised here, in the service
 * layer, so the rules hold no matter which front end (console, web, ...) is used.
 *
 * <ul>
 *   <li>ADMIN: everything.</li>
 *   <li>CUSTOMER: only their own accounts (view, history, deposit, withdraw, transfer out),
 *       plus changing their own password.</li>
 * </ul>
 */
public class SecureBankService {

    private static final String NOT_YOURS = "Account not found or you do not have access to it.";

    private final BankService bank;
    private final User user;

    public SecureBankService(BankService bank, User user) {
        this.bank = bank;
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    private boolean isAdmin() {
        return user.getRole() == Role.ADMIN;
    }

    private void requireAdmin(String action) {
        if (!isAdmin()) {
            throw new AccessDeniedException("Access denied: only administrators can " + action + ".");
        }
    }

    /** Returns the account if the user may use it. Unknown and foreign accounts look the same to customers. */
    private BankAccount requireAccess(String accountNumber) {
        BankAccount account;
        try {
            account = bank.getAccount(accountNumber);
        } catch (AccountNotFoundException e) {
            if (isAdmin()) {
                throw e;
            }
            throw new AccessDeniedException(NOT_YOURS);
        }
        if (!isAdmin() && !account.getCustomer().getCustomerId().equals(user.getCustomerId())) {
            throw new AccessDeniedException(NOT_YOURS);
        }
        return account;
    }

    // ----- admin only -----

    public Customer createCustomer(String name, String email, String phone) {
        requireAdmin("create customers");
        return bank.createCustomer(name, email, phone);
    }

    public BankAccount openAccount(String customerId, AccountType type, BigDecimal initialDeposit) {
        requireAdmin("open accounts");
        return bank.openAccount(customerId, type, initialDeposit);
    }

    public List<BankAccount> search(String query) {
        requireAdmin("search all accounts");
        return bank.search(query);
    }

    public BigDecimal applyInterest(String accountNumber) {
        requireAdmin("apply interest");
        return bank.applyInterest(accountNumber);
    }

    public int applyInterestToAll() {
        requireAdmin("apply interest");
        return bank.applyInterestToAll();
    }

    public User createCustomerLogin(String customerId, String username, String password) {
        requireAdmin("create logins");
        return bank.createUser(username, password, Role.CUSTOMER, customerId);
    }

    public User createAdminLogin(String username, String password) {
        requireAdmin("create logins");
        return bank.createUser(username, password, Role.ADMIN, null);
    }

    // ----- admin sees everything, customers only their own -----

    public List<BankAccount> listMyAccounts() {
        if (isAdmin()) {
            return bank.listAccounts();
        }
        return bank.listAccounts().stream()
                .filter(a -> a.getCustomer().getCustomerId().equals(user.getCustomerId()))
                .toList();
    }

    public BankAccount getAccount(String accountNumber) {
        return requireAccess(accountNumber);
    }

    public List<Transaction> getHistory(String accountNumber) {
        requireAccess(accountNumber);
        return bank.getHistory(accountNumber);
    }

    public BankAccount deposit(String accountNumber, BigDecimal amount) {
        requireAccess(accountNumber);
        return bank.deposit(accountNumber, amount);
    }

    public BankAccount withdraw(String accountNumber, BigDecimal amount) {
        requireAccess(accountNumber);
        return bank.withdraw(accountNumber, amount);
    }

    /** The source account must be yours (customers); the destination can be any account. */
    public void transfer(String fromAccount, String toAccount, BigDecimal amount) {
        requireAccess(fromAccount);
        bank.transfer(fromAccount, toAccount, amount);
    }

    public void changeOwnPassword(String oldPassword, String newPassword) {
        bank.changePassword(user.getUsername(), oldPassword, newPassword);
    }
}
