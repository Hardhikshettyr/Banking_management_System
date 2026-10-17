package com.bank.persistence;

public interface DataStore {
    /** Loads saved data, or returns an empty {@link BankData} if nothing has been saved yet. */
    BankData load();

    void save(BankData data);
}
