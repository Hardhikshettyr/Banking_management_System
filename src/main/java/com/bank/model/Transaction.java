package com.bank.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Immutable record of one balance change on one account. */
public final class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final TransactionType type;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;
    private final String accountNumber;
    private final String counterpartyAccount; // null for deposits/withdrawals/interest
    private final BigDecimal balanceAfter;
    private final String description;

    public Transaction(TransactionType type, BigDecimal amount, String accountNumber,
                       String counterpartyAccount, BigDecimal balanceAfter, String description) {
        this.id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.type = type;
        this.amount = amount;
        this.timestamp = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        this.accountNumber = accountNumber;
        this.counterpartyAccount = counterpartyAccount;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    /** Rebuilds a stored transaction exactly as it was saved (used by persistence only). */
    public Transaction(String id, TransactionType type, BigDecimal amount, LocalDateTime timestamp,
                       String accountNumber, String counterpartyAccount, BigDecimal balanceAfter,
                       String description) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
        this.accountNumber = accountNumber;
        this.counterpartyAccount = counterpartyAccount;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    public String getId() { return id; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getAccountNumber() { return accountNumber; }
    public String getCounterpartyAccount() { return counterpartyAccount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }
}
