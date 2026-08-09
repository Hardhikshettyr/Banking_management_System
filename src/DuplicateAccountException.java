/**
 * DuplicateAccountException.java
 *
 * Thrown when an attempt is made to create an account with an account
 * number that already exists in the system. In practice account numbers
 * are auto-generated so this mainly protects against internal bugs, but
 * it also documents the invariant "account numbers are unique" explicitly.
 */
public class DuplicateAccountException extends Exception {
    public DuplicateAccountException(String message) {
        super(message);
    }
}
