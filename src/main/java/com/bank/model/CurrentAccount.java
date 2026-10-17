package com.bank.model;

import java.math.BigDecimal;

/** Current: no minimum balance, overdraft allowed up to a fixed limit, no interest. */
public class CurrentAccount extends BankAccount {
    private static final long serialVersionUID = 1L;

    public static final BigDecimal OVERDRAFT_LIMIT = new BigDecimal("5000.00");

    public CurrentAccount(String accountNumber, Customer customer) {
        super(accountNumber, customer);
    }

    public CurrentAccount(String accountNumber, Customer customer, java.time.LocalDate openedOn) {
        super(accountNumber, customer, openedOn);
    }

    @Override
    public AccountType getType() { return AccountType.CURRENT; }

    @Override
    public BigDecimal getMinimumAllowedBalance() { return OVERDRAFT_LIMIT.negate(); }

    @Override
    public BigDecimal getMinimumOpeningDeposit() { return BigDecimal.ZERO.setScale(2); }
}
