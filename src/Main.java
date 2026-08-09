import java.util.Scanner;

/**
 * Main.java
 *
 * Entry point of the application. Its only job is to wire up the
 * dependencies (Bank + Scanner -> BankingService) and start the menu
 * loop. It deliberately contains no business or validation logic -
 * that all lives in Bank and BankingService respectively.
 */
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        BankOperations bank = new Bank();
        BankingService service = new BankingService(bank, scanner);
        service.run();
        scanner.close();
    }
}
