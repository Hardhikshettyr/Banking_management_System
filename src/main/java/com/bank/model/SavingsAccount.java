package com.bank.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Savings: must keep a minimum balance, earns 4% annual interest (credited monthly). */
public class SavingsAccount extends BankAccount {
    private static final long serialVersionUID = 1L;

    public static final BigDecimal MINIMUM_BALANCE = new BigDecimal("500.00");
    public static final BigDecimal ANNUAL_INTEREST_RATE = new BigDecimal("0.04");

    public SavingsAccount(String accountNumber, Customer customer) {
        super(accountNumber, customer);
    }

    public SavingsAccount(String accountNumber, Customer customer, java.time.LocalDate openedOn) {
        super(accountNumber, customer, openedOn);
    }

    @Override
    public AccountType getType() { return AccountType.SAVINGS; }

    @Override
    public BigDecimal getMinimumAllowedBalance() { return MINIMUM_BALANCE; }

    @Override
    public BigDecimal getMinimumOpeningDeposit() { return MINIMUM_BALANCE; }

    @Override
    public BigDecimal applyMonthlyInterest() {
        BigDecimal interest = getBalance()
                .multiply(ANNUAL_INTEREST_RATE)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_EVEN);
        if (interest.signum() > 0) {
            creditInterest(interest);
        }
        return interest;
    }
}
