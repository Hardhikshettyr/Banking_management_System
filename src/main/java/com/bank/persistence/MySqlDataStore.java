package com.bank.persistence;

import com.bank.exception.PersistenceException;
import com.bank.model.AccountType;
import com.bank.model.BankAccount;
import com.bank.model.Customer;
import com.bank.model.Role;
import com.bank.model.Transaction;
import com.bank.model.TransactionType;
import com.bank.model.User;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores the bank in MySQL using plain JDBC (no ORM).
 *
 * <p>{@link #load()} reads every table into a {@link BankData} snapshot. {@link #save} then writes
 * only what changed since the last load/save (new customers, users, accounts and transactions,
 * plus changed balances and password hashes), all inside ONE database transaction: either the
 * whole change is stored or none of it is. Account balances are verified against the stored
 * transaction history when loading.
 */
public class MySqlDataStore implements DataStore {

    private final DbConfig config;
    private boolean loaded;
    // What the database already contains, so save() can write just the difference.
    private Set<String> savedCustomers = new HashSet<>();
    private Map<String, Integer> savedTxnCounts = new HashMap<>();
    private Map<String, String> savedUserHashes = new HashMap<>();

    public MySqlDataStore(DbConfig config) {
        this.config = config;
        initSchema();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(config.url(), config.user(), config.password());
    }

    // ------------------------------------------------------------------ schema

    private void initSchema() {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            for (String sql : readSchemaStatements()) {
                st.execute(sql);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Cannot connect to or prepare the MySQL database ("
                    + config.url() + "): " + e.getMessage(), e);
        }
    }

    private List<String> readSchemaStatements() {
        try (InputStream in = MySqlDataStore.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new PersistenceException("schema.sql is missing from the application.");
            }
            StringBuilder sql = new StringBuilder();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (!line.strip().startsWith("--")) {
                    sql.append(line).append('\n');
                }
            }
            List<String> statements = new ArrayList<>();
            for (String s : sql.toString().split(";")) {
                if (!s.isBlank()) {
                    statements.add(s.strip());
                }
            }
            return statements;
        } catch (IOException e) {
            throw new PersistenceException("Could not read schema.sql: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------- load

    @Override
    public BankData load() {
        try (Connection c = connect()) {
            BankData data = new BankData();
            Set<String> customerIds = new HashSet<>();
            Map<String, Integer> txnCounts = new HashMap<>();
            Map<String, String> userHashes = new HashMap<>();

            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT customer_id, name, email, phone FROM customers "
                         + "ORDER BY LENGTH(customer_id), customer_id")) {
                while (rs.next()) {
                    Customer cu = new Customer(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4));
                    data.customers().put(cu.getCustomerId(), cu);
                    customerIds.add(cu.getCustomerId());
                }
            }

            Map<String, List<Transaction>> history = new HashMap<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT txn_id, account_number, txn_type, amount, balance_after, "
                         + "counterparty, description, created_at FROM transactions "
                         + "ORDER BY account_number, seq_no")) {
                while (rs.next()) {
                    String account = rs.getString("account_number");
                    history.computeIfAbsent(account, k -> new ArrayList<>()).add(new Transaction(
                            rs.getString("txn_id"),
                            TransactionType.valueOf(rs.getString("txn_type")),
                            rs.getBigDecimal("amount"),
                            rs.getObject("created_at", LocalDateTime.class),
                            account,
                            rs.getString("counterparty"),
                            rs.getBigDecimal("balance_after"),
                            rs.getString("description")));
                }
            }

            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT account_number, customer_id, account_type, balance, opened_on "
                         + "FROM accounts ORDER BY LENGTH(account_number), account_number")) {
                while (rs.next()) {
                    String number = rs.getString("account_number");
                    Customer owner = data.customers().get(rs.getString("customer_id"));
                    List<Transaction> txns = history.getOrDefault(number, List.of());
                    BankAccount account = BankAccount.restore(
                            AccountType.valueOf(rs.getString("account_type")), number, owner,
                            rs.getObject("opened_on", LocalDate.class), txns);
                    BigDecimal stored = rs.getBigDecimal("balance");
                    if (account.getBalance().compareTo(stored) != 0) {
                        throw new PersistenceException("Account " + number + " has a stored balance of "
                                + stored + " but its transactions add up to " + account.getBalance()
                                + ". The data may have been edited by hand.");
                    }
                    data.accounts().put(number, account);
                    txnCounts.put(number, txns.size());
                }
            }

            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT username, password_hash, role, customer_id FROM users "
                         + "ORDER BY username")) {
                while (rs.next()) {
                    User u = new User(rs.getString(1), rs.getString(2),
                            Role.valueOf(rs.getString(3)), rs.getString(4));
                    data.users().put(u.getUsername(), u);
                    userHashes.put(u.getUsername(), u.getPasswordHash());
                }
            }

            int nextCustomer = data.getNextCustomerSeq();
            int nextAccount = data.getNextAccountSeq();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT name, next_value FROM counters")) {
                while (rs.next()) {
                    if ("customer".equals(rs.getString(1))) {
                        nextCustomer = rs.getInt(2);
                    } else if ("account".equals(rs.getString(1))) {
                        nextAccount = rs.getInt(2);
                    }
                }
            }
            data.restoreCounters(nextCustomer, nextAccount);
            data.syncCounters();

            savedCustomers = customerIds;
            savedTxnCounts = txnCounts;
            savedUserHashes = userHashes;
            loaded = true;
            return data;
        } catch (SQLException | IllegalArgumentException e) {
            throw new PersistenceException("Could not read from the database: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------- save

    @Override
    public void save(BankData data) {
        if (!loaded) {
            throw new IllegalStateException("load() must be called before save().");
        }
        Set<String> customers = new HashSet<>(savedCustomers);
        Map<String, Integer> txnCounts = new HashMap<>(savedTxnCounts);
        Map<String, String> userHashes = new HashMap<>(savedUserHashes);
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                write(c, data, customers, txnCounts, userHashes);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                try {
                    c.rollback();
                } catch (SQLException ignored) {
                    // the connection is closed right after; the server rolls back anyway
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not save to the database: " + e.getMessage(), e);
        }
        savedCustomers = customers;
        savedTxnCounts = txnCounts;
        savedUserHashes = userHashes;
    }

    private void write(Connection c, BankData data, Set<String> customers,
                       Map<String, Integer> txnCounts, Map<String, String> userHashes) throws SQLException {
        // Parents before children because of the foreign keys: customers, users, accounts, transactions.
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO customers (customer_id, name, email, phone) VALUES (?, ?, ?, ?)")) {
            for (Customer cu : data.customers().values()) {
                if (customers.add(cu.getCustomerId())) {
                    ps.setString(1, cu.getCustomerId());
                    ps.setString(2, cu.getName());
                    ps.setString(3, cu.getEmail());
                    ps.setString(4, cu.getPhone());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        try (PreparedStatement insert = c.prepareStatement(
                "INSERT INTO users (username, password_hash, role, customer_id) VALUES (?, ?, ?, ?)");
             PreparedStatement update = c.prepareStatement(
                     "UPDATE users SET password_hash = ? WHERE username = ?")) {
            for (User u : data.users().values()) {
                String previous = userHashes.put(u.getUsername(), u.getPasswordHash());
                if (previous == null) {
                    insert.setString(1, u.getUsername());
                    insert.setString(2, u.getPasswordHash());
                    insert.setString(3, u.getRole().name());
                    insert.setString(4, u.getCustomerId());
                    insert.addBatch();
                } else if (!previous.equals(u.getPasswordHash())) {
                    update.setString(1, u.getPasswordHash());
                    update.setString(2, u.getUsername());
                    update.addBatch();
                }
            }
            insert.executeBatch();
            update.executeBatch();
        }

        try (PreparedStatement insAccount = c.prepareStatement(
                "INSERT INTO accounts (account_number, customer_id, account_type, balance, opened_on) "
                        + "VALUES (?, ?, ?, ?, ?)");
             PreparedStatement updAccount = c.prepareStatement(
                     "UPDATE accounts SET balance = ? WHERE account_number = ?");
             PreparedStatement insTxn = c.prepareStatement(
                     "INSERT INTO transactions (txn_id, account_number, seq_no, txn_type, amount, "
                             + "balance_after, counterparty, description, created_at) "
                             + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (BankAccount a : data.accounts().values()) {
                String number = a.getAccountNumber();
                List<Transaction> txns = a.getTransactions();
                Integer saved = txnCounts.get(number);
                if (saved == null) {
                    insAccount.setString(1, number);
                    insAccount.setString(2, a.getCustomer().getCustomerId());
                    insAccount.setString(3, a.getType().name());
                    insAccount.setBigDecimal(4, a.getBalance());
                    insAccount.setObject(5, a.getOpenedOn());
                    insAccount.addBatch();
                    saved = 0;
                } else if (txns.size() > saved) {
                    updAccount.setBigDecimal(1, a.getBalance());
                    updAccount.setString(2, number);
                    updAccount.addBatch();
                }
                for (int i = saved; i < txns.size(); i++) {
                    Transaction t = txns.get(i);
                    insTxn.setString(1, t.getId());
                    insTxn.setString(2, number);
                    insTxn.setInt(3, i);
                    insTxn.setString(4, t.getType().name());
                    insTxn.setBigDecimal(5, t.getAmount());
                    insTxn.setBigDecimal(6, t.getBalanceAfter());
                    insTxn.setString(7, t.getCounterpartyAccount());
                    insTxn.setString(8, t.getDescription());
                    insTxn.setObject(9, t.getTimestamp());
                    insTxn.addBatch();
                }
                txnCounts.put(number, txns.size());
            }
            insAccount.executeBatch();
            updAccount.executeBatch();
            insTxn.executeBatch();
        }

        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO counters (name, next_value) VALUES (?, ?) "
                        + "ON DUPLICATE KEY UPDATE next_value = ?")) {
            upsertCounter(ps, "customer", data.getNextCustomerSeq());
            upsertCounter(ps, "account", data.getNextAccountSeq());
            ps.executeBatch();
        }
    }

    private static void upsertCounter(PreparedStatement ps, String name, int value) throws SQLException {
        ps.setString(1, name);
        ps.setInt(2, value);
        ps.setInt(3, value);
        ps.addBatch();
    }
}
