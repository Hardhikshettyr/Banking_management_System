package com.bank.model;

import com.bank.util.Validator;

import java.io.Serializable;

public final class Customer implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String customerId;
    private final String name;
    private final String email;
    private final String phone;

    public Customer(String customerId, String name, String email, String phone) {
        this.customerId = customerId;
        this.name = Validator.requireValidName(name);
        this.email = Validator.requireValidEmail(email);
        this.phone = Validator.requireValidPhone(phone);
    }

    public String getCustomerId() { return customerId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }

    @Override
    public String toString() {
        return customerId + " | " + name + " | " + email + " | " + phone;
    }
}
