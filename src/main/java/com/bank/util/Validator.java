package com.bank.util;

import com.bank.exception.InvalidAmountException;
import com.bank.exception.InvalidInputException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/** Central place for input validation so every layer enforces the same rules. */
public final class Validator {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    /** Indian mobile number: 10 digits starting with 6-9. */
    private static final Pattern PHONE = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9_.]{3,30}$");
    /** Per-transaction ceiling; also keeps values inside the database's DECIMAL(15,2) column. */
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000.00");
    private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]{1,59}$");

    private Validator() {
    }

    /**
     * Ensures the amount is positive and has at most two decimal places.
     *
     * @return the amount normalised to scale 2
     */
    public static BigDecimal requireValidAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidAmountException("Amount is required.");
        }
        if (amount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new InvalidAmountException("Amount cannot have more than two decimal places.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidAmountException(String.format(
                    "Amount cannot exceed ₹%,.2f per transaction.", MAX_AMOUNT));
        }
        return amount.setScale(2);
    }

    /** @return the username normalised to lower case */
    public static String requireValidUsername(String username) {
        String u = username == null ? "" : username.trim().toLowerCase();
        if (!USERNAME.matcher(u).matches()) {
            throw new InvalidInputException(
                    "Username must be 3-30 characters: letters, digits, '_' or '.'.");
        }
        return u;
    }

    public static String requireStrongPassword(String password) {
        boolean ok = password != null && password.length() >= 8 && password.length() <= 128
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
        if (!ok) {
            throw new InvalidInputException(
                    "Password must be 8-128 characters and contain at least one letter and one digit.");
        }
        return password;
    }

    public static String requireValidName(String name) {
        if (name == null || !NAME.matcher(name.trim()).matches()) {
            throw new InvalidInputException(
                    "Name must be 2-60 characters (letters, spaces, . ' - only).");
        }
        return name.trim();
    }

    public static String requireValidEmail(String email) {
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new InvalidInputException("Email address is not valid.");
        }
        return email.trim().toLowerCase();
    }

    public static String requireValidPhone(String phone) {
        if (phone == null || !PHONE.matcher(phone.trim()).matches()) {
            throw new InvalidInputException(
                    "Phone must be a 10-digit mobile number starting with 6-9.");
        }
        return phone.trim();
    }
}
