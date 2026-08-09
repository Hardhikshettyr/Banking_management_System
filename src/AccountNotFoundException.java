/**
 * AccountNotFoundException.java
 *
 * Thrown when an operation references an account number that does not
 * exist in the bank's records (e.g. deposit/withdraw/search/close on a
 * non-existent account).
 */
public class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
