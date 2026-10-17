package com.bank.persistence;

import com.bank.exception.PersistenceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/** Test double: keeps a serialized snapshot in memory and can be told to fail the next save. */
public class InMemoryDataStore implements DataStore {

    private byte[] snapshot;
    private boolean failNextSave;

    public void failNextSave() {
        this.failNextSave = true;
    }

    @Override
    public BankData load() {
        if (snapshot == null) {
            return new BankData();
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(snapshot))) {
            return (BankData) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenceException("load failed", e);
        }
    }

    @Override
    public void save(BankData data) {
        if (failNextSave) {
            failNextSave = false;
            throw new PersistenceException("simulated database outage");
        }
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(data);
            out.flush();
            snapshot = bytes.toByteArray();
        } catch (IOException e) {
            throw new PersistenceException("save failed", e);
        }
    }
}
