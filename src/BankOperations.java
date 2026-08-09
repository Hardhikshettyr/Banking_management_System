import java.util.List;

/**
 * BankOperations.java
 *
 * Interface defining the contract for "a thing that can perform core
 * banking operations". Bank implements this.
 *
 * Why an interface here (in addition to the abstract Account class)?
 *  - Account's abstraction is about WHAT AN ACCOUNT IS (its own state
 *    and behavior: deposit, withdraw, type).
 *  - BankOperations's abstraction is about WHAT THE BANK CAN DO with a
 *    collection of accounts (open, close, search...). It describes a
 *    service-level capability, not an object's identity, which is why
 *    it is modeled as an interface rather than another abstract class.
 *  - It also means that if this project ever needed a second, different
 *    Bank implementation (e.g. a database-backed one), it could swap in
 *    without changing any code that depends on BankOperations.
 */
public interface BankOperations {

    Account openAccount(String holderName, String phoneNumber, String accountType, double initialDeposit)
            throws InvalidInputException;

    void deposit(String accountNumber, double amount) throws AccountNotFoundException, InvalidInputException;

    void withdraw(String accountNumber, double amount)
            throws AccountNotFoundException, InvalidInputException, InsufficientBalanceException;

    Account getAccount(String accountNumber) throws AccountNotFoundException;

    List<Account> getAllAccounts();

    List<Account> searchAccounts(String keyword);

    void closeAccount(String accountNumber) throws AccountNotFoundException;
}
