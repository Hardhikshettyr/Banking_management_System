/**
 * InvalidInputException.java
 *
 * Thrown when data supplied to a banking operation (name, phone number,
 * account type, amount) fails validation rules before any account state
 * is touched. Keeping this separate from AccountNotFoundException and
 * InsufficientBalanceException makes it easy for calling code to react
 * differently to "your input was malformed" versus "the operation
 * itself is not allowed".
 */
public class InvalidInputException extends Exception {
    public InvalidInputException(String message) {
        super(message);
    }
}
