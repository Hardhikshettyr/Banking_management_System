package com.bank.model;

import java.math.BigDecimal;

/** Contract for anything that can send money to another account. */
public interface Transferable {
    void transferTo(BankAccount target, BigDecimal amount);
}
