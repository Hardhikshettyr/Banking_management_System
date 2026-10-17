package com.bank.persistence;

import com.bank.model.BankAccount;
import com.bank.model.Customer;
import com.bank.model.User;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/** Everything the bank knows. This is the object graph the DataStore saves and loads. */
public class BankData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Customer> customers = new LinkedHashMap<>();
    private final Map<String, BankAccount> accounts = new LinkedHashMap<>();
    // Not final: data files written before logins existed do not contain this field.
    private Map<String, User> users = new LinkedHashMap<>();
    private int nextCustomerSeq = 1001;
    private int nextAccountSeq = 100001;

    public Map<String, Customer> customers() { return customers; }
    public Map<String, BankAccount> accounts() { return accounts; }

    public Map<String, User> users() {
        if (users == null) {
            users = new LinkedHashMap<>();
        }
        return users;
    }

    public String nextCustomerId() { return "CUST" + (nextCustomerSeq++); }
    public String nextAccountNumber() { return "ACC" + (nextAccountSeq++); }

    public int getNextCustomerSeq() { return nextCustomerSeq; }
    public int getNextAccountSeq() { return nextAccountSeq; }

    public void restoreCounters(int nextCustomerSeq, int nextAccountSeq) {
        this.nextCustomerSeq = nextCustomerSeq;
        this.nextAccountSeq = nextAccountSeq;
    }

    /** Makes sure new IDs can never collide with existing ones, whatever the saved counters say. */
    public void syncCounters() {
        for (String id : customers.keySet()) {
            nextCustomerSeq = Math.max(nextCustomerSeq, suffix(id, "CUST") + 1);
        }
        for (String no : accounts.keySet()) {
            nextAccountSeq = Math.max(nextAccountSeq, suffix(no, "ACC") + 1);
        }
    }

    private static int suffix(String id, String prefix) {
        try {
            return Integer.parseInt(id.substring(prefix.length()));
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
