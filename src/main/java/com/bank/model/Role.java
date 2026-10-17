package com.bank.model;

public enum Role {
    /** Bank staff: can manage customers and see every account. */
    ADMIN,
    /** A bank customer: can only use their own accounts. */
    CUSTOMER
}
