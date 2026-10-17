package com.bank.exception;

/** The account does not have enough available balance. */
public class InsufficientFundsException extends BankException {
    private static final long serialVersionUID = 1L;

    public InsufficientFundsException(String message) {
        super(message);
    }
}
