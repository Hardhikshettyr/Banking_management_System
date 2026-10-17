package com.bank.exception;

/** The amount is not valid for this operation. */
public class InvalidAmountException extends BankException {
    private static final long serialVersionUID = 1L;

    public InvalidAmountException(String message) {
        super(message);
    }
}
