package com.bank.service;

import com.bank.exception.AccessDeniedException;
import com.bank.exception.AccountNotFoundException;
import com.bank.exception.AuthenticationException;
import com.bank.model.AccountType;
import com.bank.model.BankAccount;
import com.bank.model.Customer;
import com.bank.persistence.InMemoryDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class SecureBankServiceTest {

    private BankService bank;
    private SecureBankService admin;
    private SecureBankService asha;
    private BankAccount ashaAccount;
    private BankAccount raviAccount;

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @BeforeEach
    void setUp() {
        bank = new BankService(new InMemoryDataStore());
        bank.createFirstAdmin("admin", "Admin@1234");
        admin = new SecureBankService(bank, bank.authenticate("admin", "Admin@1234"));

        Customer a = admin.createCustomer("Asha Rao", "asha@example.com", "9876543210");
        Customer r = admin.createCustomer("Ravi Kumar", "ravi@example.com", "9123456780");
        ashaAccount = admin.openAccount(a.getCustomerId(), AccountType.SAVINGS, bd("10000"));
        raviAccount = admin.openAccount(r.getCustomerId(), AccountType.CURRENT, bd("2000"));
        admin.createCustomerLogin(a.getCustomerId(), "asha", "Asha@1234");
        asha = new SecureBankService(bank, bank.authenticate("asha", "Asha@1234"));
    }

    @Test
    void customerCanUseOwnAccount() {
        asha.deposit(ashaAccount.getAccountNumber(), bd("500"));
        asha.withdraw(ashaAccount.getAccountNumber(), bd("1000"));
        assertEquals(bd("9500.00"), asha.getAccount(ashaAccount.getAccountNumber()).getBalance());
        assertEquals(3, asha.getHistory(ashaAccount.getAccountNumber()).size());
    }

    @Test
    void customerCannotTouchSomeoneElsesAccount() {
        String ravi = raviAccount.getAccountNumber();
        assertThrows(AccessDeniedException.class, () -> asha.getAccount(ravi));
        assertThrows(AccessDeniedException.class, () -> asha.getHistory(ravi));
        assertThrows(AccessDeniedException.class, () -> asha.deposit(ravi, bd("10")));
        assertThrows(AccessDeniedException.class, () -> asha.withdraw(ravi, bd("10")));
        assertEquals(bd("2000.00"), raviAccount.getBalance());
    }

    @Test
    void customerCannotTransferOutOfSomeoneElsesAccount() {
        assertThrows(AccessDeniedException.class,
                () -> asha.transfer(raviAccount.getAccountNumber(), ashaAccount.getAccountNumber(), bd("100")));
        assertEquals(bd("2000.00"), raviAccount.getBalance());
        assertEquals(bd("10000.00"), ashaAccount.getBalance());
    }

    @Test
    void customerCanTransferFromOwnAccountToAnyAccount() {
        asha.transfer(ashaAccount.getAccountNumber(), raviAccount.getAccountNumber(), bd("1500"));
        assertEquals(bd("8500.00"), ashaAccount.getBalance());
        assertEquals(bd("3500.00"), raviAccount.getBalance());
    }

    @Test
    void unknownAndForeignAccountsLookTheSameToACustomer() {
        AccessDeniedException foreign = assertThrows(AccessDeniedException.class,
                () -> asha.getAccount(raviAccount.getAccountNumber()));
        AccessDeniedException unknown = assertThrows(AccessDeniedException.class,
                () -> asha.getAccount("ACC999999"));
        assertEquals(foreign.getMessage(), unknown.getMessage());
    }

    @Test
    void customerCannotUseAdminFunctions() {
        assertThrows(AccessDeniedException.class, () -> asha.createCustomer("New Person", "n@example.com", "9876543299"));
        assertThrows(AccessDeniedException.class, () -> asha.openAccount("CUST1001", AccountType.CURRENT, bd("0")));
        assertThrows(AccessDeniedException.class, () -> asha.search("ravi"));
        assertThrows(AccessDeniedException.class, () -> asha.applyInterest(ashaAccount.getAccountNumber()));
        assertThrows(AccessDeniedException.class, asha::applyInterestToAll);
        assertThrows(AccessDeniedException.class, () -> asha.createAdminLogin("sneaky", "Sneaky@1234"));
        assertThrows(AccessDeniedException.class, () -> asha.createCustomerLogin("CUST1002", "ravi", "Ravi@12345"));
        assertThrows(AuthenticationException.class, () -> bank.authenticate("sneaky", "Sneaky@1234"));
    }

    @Test
    void customerSeesOnlyOwnAccountsAdminSeesAll() {
        assertEquals(1, asha.listMyAccounts().size());
        assertEquals(ashaAccount.getAccountNumber(), asha.listMyAccounts().get(0).getAccountNumber());
        assertEquals(2, admin.listMyAccounts().size());
    }

    @Test
    void adminCanUseAnyAccountButUnknownOnesStillFail() {
        admin.deposit(raviAccount.getAccountNumber(), bd("100"));
        assertEquals(bd("2100.00"), raviAccount.getBalance());
        assertThrows(AccountNotFoundException.class, () -> admin.getAccount("ACC999999"));
    }

    @Test
    void usersCanChangeOnlyTheirOwnPassword() {
        asha.changeOwnPassword("Asha@1234", "NewAsha@5678");
        assertThrows(AuthenticationException.class, () -> bank.authenticate("asha", "Asha@1234"));
        assertNotNull(bank.authenticate("asha", "NewAsha@5678"));
        assertNotNull(bank.authenticate("admin", "Admin@1234"));
    }
}
