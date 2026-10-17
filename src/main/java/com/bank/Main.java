package com.bank;

import com.bank.exception.PersistenceException;
import com.bank.persistence.DataStore;
import com.bank.persistence.DbConfig;
import com.bank.persistence.FileDataStore;
import com.bank.persistence.MySqlDataStore;
import com.bank.service.BankService;
import com.bank.ui.ConsoleApp;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Scanner;

/**
 * Entry point.
 * <pre>
 *   java -jar banking-system.jar                  uses MySQL (settings in db.properties)
 *   java -jar banking-system.jar --file [path]    uses a local file instead (no database needed)
 * </pre>
 */
public class Main {

    public static void main(String[] args) {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        try {
            DataStore store = chooseStore(args, out);
            if (store == null) {
                System.exit(2);
            }
            BankService bank = new BankService(store);
            boolean hide = System.console() != null;
            if (!hide) {
                out.println("Note: this terminal cannot hide typed passwords; they will be visible.");
            }
            new ConsoleApp(bank, new Scanner(System.in, StandardCharsets.UTF_8), out, hide).run();
        } catch (PersistenceException e) {
            out.println("Could not start: " + e.getMessage());
            System.exit(1);
        } catch (RuntimeException e) {
            out.println("Fatal error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static DataStore chooseStore(String[] args, PrintStream out) {
        if (args.length > 0 && args[0].equals("--file")) {
            Path file = Paths.get(args.length > 1 ? args[1] : "data/bank.dat");
            out.println("Storage: local file " + file.toAbsolutePath());
            return new FileDataStore(file);
        }
        Optional<DbConfig> config = DbConfig.load(Paths.get("db.properties"));
        if (config.isEmpty()) {
            out.println("No MySQL settings found.");
            out.println("Create a file named db.properties in this folder (copy db.properties.example)");
            out.println("with your url, user and password, or set BANK_DB_URL, BANK_DB_USER and");
            out.println("BANK_DB_PASSWORD. To try the app without a database, run it with: --file");
            return null;
        }
        out.println("Storage: MySQL " + config.get().url());
        return new MySqlDataStore(config.get());
    }
}
