package com.bank.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    @Test
    void correctPasswordVerifiesAndWrongOneDoesNot() {
        String stored = PasswordHasher.hash("Secret123");
        assertTrue(PasswordHasher.verify("Secret123", stored));
        assertFalse(PasswordHasher.verify("secret123", stored));
        assertFalse(PasswordHasher.verify("", stored));
    }

    @Test
    void sameePasswordGivesDifferentHashesBecauseOfRandomSalt() {
        String a = PasswordHasher.hash("Secret123");
        String b = PasswordHasher.hash("Secret123");
        assertNotEquals(a, b);
        assertTrue(a.startsWith("pbkdf2$"));
        assertFalse(a.contains("Secret123"));
    }

    @Test
    void malformedStoredValuesNeverVerify() {
        assertFalse(PasswordHasher.verify("x", null));
        assertFalse(PasswordHasher.verify(null, PasswordHasher.hash("Secret123")));
        assertFalse(PasswordHasher.verify("x", "plaintext"));
        assertFalse(PasswordHasher.verify("x", "pbkdf2$abc$!!$!!"));
        assertFalse(PasswordHasher.verify("x", "md5$1$AAAA$AAAA"));
    }
}
