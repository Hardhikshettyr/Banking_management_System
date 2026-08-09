# 🏦 Banking Management System

A console-based **Banking Management System** built in **Core Java**, demonstrating strong fundamentals in Object-Oriented Programming, Java Collections, exception handling, and input validation — no frameworks, no database, no GUI.

![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk&logoColor=white)
![Type](https://img.shields.io/badge/Type-Console%20Application-blue)
![Status](https://img.shields.io/badge/Status-Complete-brightgreen)


---

## 📖 Overview

This project simulates the core operations of a bank entirely through a terminal menu — opening accounts, deposits, withdrawals, balance checks, searching, and closing accounts — built to demonstrate practical, real-world use of Core Java concepts rather than just theory.

---

## ✨ Features

- 🆕 Open a **Savings** or **Current** account with auto-generated, unique account numbers
- 💰 Deposit and withdraw funds with rule-based validation
- 🔍 Check balance and view full account details
- 📋 View all accounts / search by account number or holder name
- ❌ Close an account
- 🛡️ Graceful handling of invalid input — the app never crashes on bad input

---

## 🧠 OOP Concepts Demonstrated

| Concept | Implementation |
|---|---|
| **Encapsulation** | `Account` — private fields, controlled access via getters/setters |
| **Abstraction** | `Account` (abstract class), `BankOperations` (interface) |
| **Inheritance** | `SavingsAccount` / `CurrentAccount` extend `Account` |
| **Polymorphism** | `withdraw()` behaves differently per account type; `Bank` operates on `Account` references without knowing the concrete subclass at compile time |

## 📦 Java Collections Used

`ArrayList<Account>` is the primary storage structure inside `Bank`:

- `add()` → opening a new account
- `remove()` → closing an account
- iteration (for-each) → viewing all accounts, balance lookups
- linear search → finding an account by number or keyword

## ⚠️ Exception Handling

Custom checked exceptions keep error semantics explicit and domain-specific:

- `InsufficientBalanceException`
- `AccountNotFoundException`
- `InvalidInputException`
- `DuplicateAccountException`

Combined with handling of `InputMismatchException` and `NumberFormatException` so no invalid console input can crash the app.

---

## 🗂️ Project Structure

```
BankingManagementSystem/
│
├── src/
│   ├── Account.java                     # abstract base class
│   ├── SavingsAccount.java              # extends Account
│   ├── CurrentAccount.java              # extends Account
│   ├── BankOperations.java              # interface — abstraction over bank actions
│   ├── Bank.java                        # implements BankOperations, holds ArrayList
│   ├── BankingService.java              # menu / console I/O layer
│   ├── ValidationUtil.java              # static validation helpers
│   ├── Main.java                        # entry point
│   ├── InsufficientBalanceException.java
│   ├── DuplicateAccountException.java
│   ├── AccountNotFoundException.java
│   └── InvalidInputException.java
│
└── README.md
```

Each class has a single responsibility: `Account` and its subclasses model *what an account is*; `BankOperations` / `Bank` model *what the bank can do*; `BankingService` handles *console interaction*; `ValidationUtil` centralizes *input rules*; `Main` just wires it all together.

---

## 🚀 Getting Started

### Prerequisites
- JDK 17 or higher installed
- A terminal

### Compile & Run

```bash
git clone https://github.com/<your-username>/BankingManagementSystem.git
cd Banking_management_system/src

javac *.java -d ../out
cd ../out
java Main
```

---

## 💻 Example Flow

```
===== Banking Management System =====
1. Open Account
2. Deposit
3. Withdraw
4. Check Balance
5. View Account Details
6. View All Accounts
7. Search Account
8. Close Account
9. Exit
Enter your choice: 1

--- Open New Account ---
Enter account holder name: Hardhik Rao
Enter phone number: 9876543210
Enter account type (Savings/Current): Savings
Enter initial deposit: 1000

Account created successfully!
Account No: ACC1001    | Holder: Hardhik Rao          | Type: Savings    | Phone: 9876543210   | Balance: 1000.00
```

---

## ✅ Test Scenarios

### Valid Cases

| Action | Expected Result |
|---|---|
| Create account (valid inputs) | Account created with unique auto-generated number |
| Deposit positive amount | Balance increases accordingly |
| Withdraw within limit | Balance decreases; Savings enforces ₹500 minimum |
| Check balance | Correct name, number, type, balance printed |
| Search by partial name/number | All matching accounts returned |
| Close an existing account | Account removed from list |

### Invalid Cases

| Action | Expected Result |
|---|---|
| Negative deposit | Rejected — amount must be greater than zero |
| Zero withdrawal | Rejected — amount must be greater than zero |
| Withdrawal exceeding balance | Rejected — insufficient balance |
| Withdrawal breaking Savings min balance | Rejected — minimum balance message |
| Invalid account number | "No account found with account number: ..." |
| Duplicate account number | Prevented — account numbers are auto-generated |
| Invalid menu input | "Invalid choice" — returns to menu, no crash |
| Empty account holder name | Rejected — name cannot be empty |
| Invalid phone number | Rejected — invalid format |

---



## 👤 Author

**Hardhik**
Computer Science Engineering Student | Aspiring Software Engineer
