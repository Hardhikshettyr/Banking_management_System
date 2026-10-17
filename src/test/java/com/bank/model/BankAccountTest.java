package com.bank.model;

import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;
import com.bank.exception.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BankAccountTest {

    private Customer customer;
    private SavingsAccount savings;
    private CurrentAccount current;

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @BeforeEach
    void setUp() {
        customer = new Customer("CUST1001", "Asha Rao", "asha@example.com", "9876543210");
        savings = new SavingsAccount("ACC100001", customer);
        current = new CurrentAccount("ACC100002", customer);
    }

    @Test
    void depositIncreasesBalanceAndRecordsTransaction() {
        savings.deposit(bd("1000"));
        assertEquals(bd("1000.00"), savings.getBalance());
        assertEquals(1, savings.getTransactions().size());
        Transaction t = savings.getTransactions().get(0);
        assertEquals(TransactionType.DEPOSIT, t.getType());
        assertEquals(bd("1000.00"), t.getBalanceAfter());
    }

    @Test
    void withdrawalWorkflowFromSpec() {
        savings.deposit(bd("10000"));
        savings.withdraw(bd("2000"));
        assertEquals(bd("8000.00"), savings.getBalance());
        assertEquals(2, savings.getTransactions().size());
    }

    @Test
    void withdrawalBeyondBalanceIsRejectedAndBalanceUnchanged() {
        savings.deposit(bd("10000"));
        assertThrows(InsufficientFundsException.class, () -> savings.withdraw(bd("20000")));
        assertEquals(bd("10000.00"), savings.getBalance());
        assertEquals(1, savings.getTransactions().size());
    }

    @Test
    void invalidAmountsAreRejected() {
        assertThrows(InvalidAmountException.class, () -> savings.deposit(bd("0")));
        assertThrows(InvalidAmountException.class, () -> savings.deposit(bd("-5")));
        assertThrows(InvalidAmountException.class, () -> savings.deposit(null));
        assertThrows(InvalidAmountException.class, () -> savings.deposit(bd("10.005")));
        assertEquals(BigDecimal.ZERO.setScale(2), savings.getBalance());
    }

    @Test
    void amountWithTrailingZerosBeyondTwoDecimalsIsAccepted() {
        savings.deposit(bd("100.500"));
        assertEquals(bd("100.50"), savings.getBalance());
    }

    @Test
    void savingsMustKeepMinimumBalance() {
        savings.deposit(bd("1000"));
        savings.withdraw(bd("500")); // leaves exactly 500: allowed
        assertEquals(bd("500.00"), savings.getBalance());
        assertThrows(InsufficientFundsException.class, () -> savings.withdraw(bd("0.01")));
    }

    @Test
    void currentAccountAllowsOverdraftUpToLimit() {
        current.deposit(bd("1000"));
        current.withdraw(bd("6000")); // balance -5000: exactly the limit
        assertEquals(bd("-5000.00"), current.getBalance());
        assertEquals(bd("0.00"), current.getAvailableBalance());
        assertThrows(InsufficientFundsException.class, () -> current.withdraw(bd("0.01")));
    }

    @Test
    void transferMovesMoneyAndRecordsBothSides() {
        savings.deposit(bd("5000"));
        savings.transferTo(current, bd("1500"));
        assertEquals(bd("3500.00"), savings.getBalance());
        assertEquals(bd("1500.00"), current.getBalance());

        Transaction out = savings.getTransactions().get(1);
        Transaction in = current.getTransactions().get(0);
        assertEquals(TransactionType.TRANSFER_OUT, out.getType());
        assertEquals(current.getAccountNumber(), out.getCounterpartyAccount());
        assertEquals(TransactionType.TRANSFER_IN, in.getType());
        assertEquals(savings.getAccountNumber(), in.getCounterpartyAccount());
    }

    @Test
    void failedTransferChangesNeitherAccount() {
        savings.deposit(bd("1000"));
        assertThrows(InsufficientFundsException.class, () -> savings.transferTo(current, bd("800")));
        assertEquals(bd("1000.00"), savings.getBalance());
        assertEquals(bd("0.00"), current.getBalance());
        assertTrue(current.getTransactions().isEmpty());
    }

    @Test
    void transferToSameAccountOrNullIsRejected() {
        savings.deposit(bd("1000"));
        assertThrows(InvalidInputException.class, () -> savings.transferTo(savings, bd("10")));
        assertThrows(InvalidInputException.class, () -> savings.transferTo(null, bd("10")));
    }

    @Test
    void savingsInterestIsMonthlyFractionOfAnnualRate() {
        savings.deposit(bd("12000"));
        BigDecimal interest = savings.applyMonthlyInterest();
        assertEquals(bd("40.00"), interest); // 12000 * 4% / 12
        assertEquals(bd("12040.00"), savings.getBalance());
        assertEquals(TransactionType.INTEREST, savings.getTransactions().get(1).getType());
    }

    @Test
    void currentAccountEarnsNoInterest() {
        current.deposit(bd("12000"));
        assertEquals(0, current.applyMonthlyInterest().signum());
        assertEquals(1, current.getTransactions().size());
    }

    @Test
    void transactionListCannotBeModifiedFromOutside() {
        savings.deposit(bd("1000"));
        assertThrows(UnsupportedOperationException.class, () -> savings.getTransactions().clear());
    }
}
