package com.bank.exception;

/** Data could not be saved to or loaded from storage. */
public class PersistenceException extends BankException {
    private static final long serialVersionUID = 1L;

    public PersistenceException(String message) {
        super(message);
    }

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
