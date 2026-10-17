package com.bank.service;

import com.bank.exception.AccountNotFoundException;
import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;
import com.bank.model.AccountType;
import com.bank.model.BankAccount;
import com.bank.model.Customer;
import com.bank.model.TransactionType;
import com.bank.persistence.BankData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TransactionServiceTest {

    private TransactionService tx;
    private BankAccount savings;
    private BankAccount current;

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @BeforeEach
    void setUp() {
        AccountService accounts = new AccountService(new BankData());
        tx = new TransactionService(accounts);
        Customer c = accounts.createCustomer("Asha Rao", "asha@example.com", "9876543210");
        savings = accounts.openAccount(c.getCustomerId(), AccountType.SAVINGS, bd("10000"));
        current = accounts.openAccount(c.getCustomerId(), AccountType.CURRENT, bd("1000"));
    }

    @Test
    void depositAndWithdrawByAccountNumber() {
        tx.deposit(savings.getAccountNumber(), bd("500"));
        tx.withdraw(savings.getAccountNumber(), bd("2000"));
        assertEquals(bd("8500.00"), savings.getBalance());
        assertEquals(3, tx.getHistory(savings.getAccountNumber()).size());
    }

    @Test
    void withdrawalOfTwentyThousandFromTenThousandIsRejected() {
        assertThrows(InsufficientFundsException.class,
                () -> tx.withdraw(savings.getAccountNumber(), bd("20000")));
        assertEquals(bd("10000.00"), savings.getBalance());
    }

    @Test
    void operationsOnNonexistentAccountFail() {
        assertThrows(AccountNotFoundException.class, () -> tx.deposit("ACC000000", bd("10")));
        assertThrows(AccountNotFoundException.class, () -> tx.withdraw("ACC000000", bd("10")));
        assertThrows(AccountNotFoundException.class, () -> tx.getHistory("ACC000000"));
    }

    @Test
    void transferMovesFundsBetweenAccounts() {
        tx.transfer(savings.getAccountNumber(), current.getAccountNumber(), bd("2500.50"));
        assertEquals(bd("7499.50"), savings.getBalance());
        assertEquals(bd("3500.50"), current.getBalance());
        assertEquals(TransactionType.TRANSFER_OUT, savings.getTransactions().get(1).getType());
        assertEquals(TransactionType.TRANSFER_IN, current.getTransactions().get(1).getType());
    }

    @Test
    void transferToMissingAccountLeavesSourceUntouched() {
        assertThrows(AccountNotFoundException.class,
                () -> tx.transfer(savings.getAccountNumber(), "ACC000000", bd("100")));
        assertEquals(bd("10000.00"), savings.getBalance());
        assertEquals(1, savings.getTransactions().size());
    }

    @Test
    void transferWithInsufficientFundsLeavesBothUntouched() {
        assertThrows(InsufficientFundsException.class,
                () -> tx.transfer(savings.getAccountNumber(), current.getAccountNumber(), bd("9600")));
        assertEquals(bd("10000.00"), savings.getBalance());
        assertEquals(bd("1000.00"), current.getBalance());
    }

    @Test
    void transferRejectsBadAmounts() {
        assertThrows(InvalidAmountException.class,
                () -> tx.transfer(savings.getAccountNumber(), current.getAccountNumber(), bd("0")));
        assertThrows(InvalidAmountException.class,
                () -> tx.transfer(savings.getAccountNumber(), current.getAccountNumber(), bd("-1")));
    }

    @Test
    void interestOnlyCreditedToSavings() {
        assertEquals(bd("33.33"), tx.applyInterest(savings.getAccountNumber())); // 10000*4%/12
        assertEquals(0, tx.applyInterest(current.getAccountNumber()).signum());
        assertEquals(1, tx.applyInterestToAll());
    }
}
