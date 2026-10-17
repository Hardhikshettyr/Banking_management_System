package com.bank.exception;

/** Base type for every business-rule failure in the banking system. */
public class BankException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BankException(String message) {
        super(message);
    }

    public BankException(String message, Throwable cause) {
        super(message, cause);
    }
}
