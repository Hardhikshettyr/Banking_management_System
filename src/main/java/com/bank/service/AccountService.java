package com.bank.service;

import com.bank.exception.AccountNotFoundException;
import com.bank.exception.InvalidAmountException;
import com.bank.exception.InvalidInputException;
import com.bank.model.AccountType;
import com.bank.model.BankAccount;
import com.bank.model.CurrentAccount;
import com.bank.model.Customer;
import com.bank.model.SavingsAccount;
import com.bank.persistence.BankData;
import com.bank.util.Validator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Customer and account management: creation, lookup, search. */
public class AccountService {

    private final BankData data;

    public AccountService(BankData data) {
        this.data = data;
    }

    public Customer createCustomer(String name, String email, String phone) {
        // Validate first so a failed attempt does not burn an ID.
        String cleanEmail = Validator.requireValidEmail(email);
        boolean duplicate = data.customers().values().stream()
                .anyMatch(c -> c.getEmail().equals(cleanEmail));
        if (duplicate) {
            throw new InvalidInputException("A customer with this email already exists.");
        }
        Customer customer = new Customer(data.nextCustomerId(), name, cleanEmail, phone);
        data.customers().put(customer.getCustomerId(), customer);
        return customer;
    }

    public BankAccount openAccount(String customerId, AccountType type, BigDecimal initialDeposit) {
        Customer customer = findCustomer(customerId);
        if (type == null) {
            throw new InvalidInputException("Account type is required.");
        }
        BigDecimal deposit = initialDeposit == null ? BigDecimal.ZERO : initialDeposit;

        // Build a probe account to read the type's rules without consuming a real number yet.
        BankAccount probe = type == AccountType.SAVINGS
                ? new SavingsAccount("PROBE", customer) : new CurrentAccount("PROBE", customer);
        if (deposit.compareTo(probe.getMinimumOpeningDeposit()) < 0) {
            throw new InvalidAmountException(String.format(
                    "A %s account needs an opening deposit of at least ₹%,.2f.",
                    type, probe.getMinimumOpeningDeposit()));
        }
        if (deposit.signum() > 0) {
            Validator.requireValidAmount(deposit);
        }

        String number = data.nextAccountNumber();
        BankAccount account = type == AccountType.SAVINGS
                ? new SavingsAccount(number, customer) : new CurrentAccount(number, customer);
        if (deposit.signum() > 0) {
            account.deposit(deposit);
        }
        data.accounts().put(number, account);
        return account;
    }

    public Customer findCustomer(String customerId) {
        Customer c = customerId == null ? null : data.customers().get(customerId.trim().toUpperCase());
        if (c == null) {
            throw new AccountNotFoundException("Customer not found: " + customerId);
        }
        return c;
    }

    public BankAccount findAccount(String accountNumber) {
        BankAccount a = accountNumber == null ? null : data.accounts().get(accountNumber.trim().toUpperCase());
        if (a == null) {
            throw new AccountNotFoundException("Account not found: " + accountNumber);
        }
        return a;
    }

    /** Matches account number, customer ID, or part of the customer's name (case-insensitive). */
    public List<BankAccount> search(String query) {
        if (query == null || query.isBlank()) {
            throw new InvalidInputException("Search text is required.");
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<BankAccount> result = new ArrayList<>();
        for (BankAccount a : data.accounts().values()) {
            if (a.getAccountNumber().toLowerCase(Locale.ROOT).equals(q)
                    || a.getCustomer().getCustomerId().toLowerCase(Locale.ROOT).equals(q)
                    || a.getCustomer().getName().toLowerCase(Locale.ROOT).contains(q)) {
                result.add(a);
            }
        }
        return result;
    }

    public List<BankAccount> listAccounts() {
        return new ArrayList<>(data.accounts().values());
    }
}
