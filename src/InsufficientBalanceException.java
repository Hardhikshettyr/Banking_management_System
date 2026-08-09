/**
 * InsufficientBalanceException.java
 *
 * Custom checked exception thrown when a withdrawal amount exceeds the
 * available balance of an account.
 *
 * Why a custom exception instead of a generic one?
 *  - It makes the calling code's intent explicit (catch this specific
 *    business-rule violation rather than a vague RuntimeException).
 *  - It keeps banking-specific error semantics inside the banking domain
 *    instead of overloading Java's built-in exceptions.
 */
public class InsufficientBalanceException extends Exception {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
