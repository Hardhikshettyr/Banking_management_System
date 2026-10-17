package com.bank.exception;

/** Login failed. */
public class AuthenticationException extends BankException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
