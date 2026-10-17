-- Schema for the Java Banking Management System (MySQL 8+).
-- The application runs these statements itself on startup (they are safe to repeat),
-- so you normally do not need to run this file by hand.

CREATE TABLE IF NOT EXISTS customers (
    customer_id VARCHAR(20)  NOT NULL PRIMARY KEY,
    name        VARCHAR(60)  NOT NULL,
    email       VARCHAR(120) NOT NULL UNIQUE,
    phone       VARCHAR(10)  NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS accounts (
    account_number VARCHAR(20)   NOT NULL PRIMARY KEY,
    customer_id    VARCHAR(20)   NOT NULL,
    account_type   VARCHAR(10)   NOT NULL,
    balance        DECIMAL(15,2) NOT NULL,
    opened_on      DATE          NOT NULL,
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS transactions (
    txn_id         VARCHAR(16)   NOT NULL PRIMARY KEY,
    account_number VARCHAR(20)   NOT NULL,
    seq_no         INT           NOT NULL,
    txn_type       VARCHAR(20)   NOT NULL,
    amount         DECIMAL(15,2) NOT NULL,
    balance_after  DECIMAL(15,2) NOT NULL,
    counterparty   VARCHAR(20)   NULL,
    description    VARCHAR(100)  NOT NULL,
    created_at     DATETIME      NOT NULL,
    UNIQUE KEY uq_transactions_account_seq (account_number, seq_no),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_number) REFERENCES accounts (account_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS users (
    username      VARCHAR(30)  NOT NULL PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(10)  NOT NULL,
    customer_id   VARCHAR(20)  NULL UNIQUE,
    CONSTRAINT fk_users_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS counters (
    name       VARCHAR(30) NOT NULL PRIMARY KEY,
    next_value INT         NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
