package com.bank.persistence;

import com.bank.exception.PersistenceException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Stores the bank data in a single file using Java serialization.
 * Writes go to a temporary file first and are then moved into place, so a crash
 * mid-save cannot corrupt the previous good file.
 */
public class FileDataStore implements DataStore {

    private final Path file;

    public FileDataStore(Path file) {
        this.file = file;
    }

    @Override
    public BankData load() {
        if (!Files.exists(file)) {
            return new BankData();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            return (BankData) in.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new PersistenceException("Could not read data file " + file + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void save(BankData data) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
                out.writeObject(data);
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new PersistenceException("Could not write data file " + file + ": " + e.getMessage(), e);
        }
    }
}
