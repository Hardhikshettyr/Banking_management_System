package com.bank.exception;

/** The signed-in user is not allowed to do this. */
public class AccessDeniedException extends BankException {
    private static final long serialVersionUID = 1L;

    public AccessDeniedException(String message) {
        super(message);
    }
}
