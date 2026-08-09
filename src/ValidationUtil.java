/**
 * ValidationUtil.java
 *
 * A stateless utility class holding static validation helpers used
 * across the application. Keeping validation logic here (rather than
 * scattered inside menu-handling code) means:
 *   - the rules are defined once and reused everywhere,
 *   - BankingService and Main stay focused on their own responsibilities,
 *   - the rules are easy to find, test, and change.
 *
 * Java concepts used: static methods (no need to instantiate this class),
 * regular expressions for phone validation.
 */
public final class ValidationUtil {

    // Private constructor: this class is never meant to be instantiated.
    private ValidationUtil() {
    }

    public static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Accepts a 10-digit phone number, optionally prefixed with +countrycode.
     * Examples that pass: 9876543210, +919876543210
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (isNullOrEmpty(phoneNumber)) {
            return false;
        }
        return phoneNumber.matches("^(\\+\\d{1,3})?\\d{10}$");
    }

    public static boolean isValidAccountType(String type) {
        return "SAVINGS".equalsIgnoreCase(type) || "CURRENT".equalsIgnoreCase(type);
    }

    public static boolean isNonNegative(double amount) {
        return amount >= 0;
    }

    public static boolean isPositive(double amount) {
        return amount > 0;
    }
}
