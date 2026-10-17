package com.bank.persistence;

import com.bank.exception.PersistenceException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * MySQL connection settings. Read from a {@code db.properties} file (keys: url, user, password);
 * the environment variables BANK_DB_URL, BANK_DB_USER and BANK_DB_PASSWORD override the file.
 */
public record DbConfig(String url, String user, String password) {

    public static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/bankdb?createDatabaseIfNotExist=true"
                    + "&allowPublicKeyRetrieval=true&useSSL=false";

    /** @return the settings, or empty if neither a properties file nor environment variables exist */
    public static Optional<DbConfig> load(Path file) {
        Properties props = new Properties();
        boolean fileFound = Files.isRegularFile(file);
        if (fileFound) {
            try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(in);
            } catch (IOException e) {
                throw new PersistenceException("Could not read " + file + ": " + e.getMessage(), e);
            }
        }
        String url = pick(System.getenv("BANK_DB_URL"), props.getProperty("url"));
        String user = pick(System.getenv("BANK_DB_USER"), props.getProperty("user"));
        String password = System.getenv("BANK_DB_PASSWORD") != null
                ? System.getenv("BANK_DB_PASSWORD") : props.getProperty("password", "");
        if (!fileFound && url == null && user == null) {
            return Optional.empty();
        }
        return Optional.of(new DbConfig(url != null ? url : DEFAULT_URL,
                user != null ? user : "root", password));
    }

    private static String pick(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second != null && !second.isBlank() ? second.trim() : null;
    }
}
