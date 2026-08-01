// BankingSystem.java — Efraín Rojas Artavia
// Simple banking system in Java

import java.util.*;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// ── Transaction ────────────────────────────────────────────────────────────────
class Transaction {
    enum Type { DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT }

    private final Type      type;
    private final double    amount;
    private final String    description;
    private final String    timestamp;

    public Transaction(Type type, double amount, String description) {
        this.type        = type;
        this.amount      = amount;
        this.description = description;
        this.timestamp   = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public String toString() {
        String sign = (type == Type.DEPOSIT || type == Type.TRANSFER_IN) ? "+" : "-";
        return String.format("  [%s] %s$%.2f  %s", timestamp, sign, amount, description);
    }

    public Type getType() { return type; }
    public double getAmount() { return amount; }
}

// ── Account ────────────────────────────────────────────────────────────────────
class Account {
    private final String          id;
    private final String          owner;
    private       double          balance;
    private final List<Transaction> history = new ArrayList<>();

    public Account(String id, String owner, double initialDeposit) {
        this.id      = id;
        this.owner   = owner;
        this.balance = 0;
        if (initialDeposit > 0) deposit(initialDeposit, "Initial deposit");
    }

    public boolean deposit(double amount, String reason) {
        if (amount <= 0) return false;
        balance += amount;
        history.add(new Transaction(Transaction.Type.DEPOSIT, amount, reason));
        return true;
    }

    public boolean withdraw(double amount, String reason) {
        if (amount <= 0 || amount > balance) return false;
        balance -= amount;
        history.add(new Transaction(Transaction.Type.WITHDRAWAL, amount, reason));
        return true;
    }

    public boolean transfer(Account target, double amount) {
        if (amount <= 0 || amount > balance) return false;
        balance -= amount;
        history.add(new Transaction(Transaction.Type.TRANSFER_OUT, amount,
            "Transfer to " + target.getId()));
        target.balance += amount;
        target.history.add(new Transaction(Transaction.Type.TRANSFER_IN, amount,
            "Transfer from " + this.id));
        return true;
    }

    public void printStatement() {
        System.out.println("\n  ╔══════════════════════════════════════════╗");
        System.out.printf("  ║  Account: %-32s║%n", id);
        System.out.printf("  ║  Owner:   %-32s║%n", owner);
        System.out.printf("  ║  Balance: $%-31.2f║%n", balance);
        System.out.println("  ╚══════════════════════════════════════════╝");
        System.out.println("\n  Transaction History:");
        if (history.isEmpty()) { System.out.println("  No transactions yet."); return; }
        history.stream().skip(Math.max(0, history.size() - 10))
               .forEach(System.out::println);
    }

    public String getId()      { return id; }
    public String getOwner()   { return owner; }
    public double getBalance() { return balance; }
}

// ── Bank ───────────────────────────────────────────────────────────────────────
class Bank {
    private final String              name;
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private       int                 nextId   = 1001;

    public Bank(String name) { this.name = name; }

    public Account createAccount(String owner, double initial) {
        String id = "ACC-" + nextId++;
        Account acc = new Account(id, owner, initial);
        accounts.put(id, acc);
        return acc;
    }

    public Account find(String id) { return accounts.get(id.toUpperCase()); }

    public void listAccounts() {
        System.out.printf("%n  %-12s %-20s %s%n", "Account ID", "Owner", "Balance");
        System.out.println("  " + "─".repeat(45));
        accounts.values().forEach(a ->
            System.out.printf("  %-12s %-20s $%.2f%n", a.getId(), a.getOwner(), a.getBalance()));
    }

    public String getName() { return name; }
}

// ── UI ─────────────────────────────────────────────────────────────────────────
class UI {
    private static final Scanner sc = new Scanner(System.in);

    static void header(String title) {
        System.out.println("\n══════════════════════════════════════════════════");
        System.out.println("  " + title);
        System.out.println("══════════════════════════════════════════════════");
    }

    static void ok(String msg)  { System.out.println("  ✅ " + msg); }
    static void err(String msg) { System.out.println("  ❌ " + msg); }
    static void info(String msg){ System.out.println("  ℹ  " + msg); }

    static String prompt(String label) {
        System.out.print("\n  " + label + " > ");
        return sc.nextLine().trim();
    }

    static double promptDouble(String label) {
        try { return Double.parseDouble(prompt(label)); }
        catch (NumberFormatException e) { return -1; }
    }
}

// ── Main ───────────────────────────────────────────────────────────────────────
public class BankingSystem {

    static Bank   bank;
    static Account session; // logged-in account

    public static void main(String[] args) {
        // Force UTF-8 on stdout so accents, box-drawing chars and emoji render
        // correctly regardless of the host OS's default console charset
        // (this was previously showing as "?" on non-UTF-8 locales, e.g. many
        // default Windows/Linux setups, when run with the plain README commands).
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        bank = new Bank("EfraBank");
        seedData();

        UI.header("🏦 EFRABANK — Banking System");
        UI.info("Welcome! Please log in or create an account.");

        boolean running = true;
        while (running) {
            if (session == null) running = mainMenu();
            else                 running = accountMenu();
        }
        System.out.println("\n  Goodbye! 👋\n");
    }

    static void seedData() {
        bank.createAccount("Efraín Rojas", 1500.00);
        bank.createAccount("Maria García", 3200.00);
        bank.createAccount("Carlos López", 800.00);
    }

    static boolean mainMenu() {
        UI.header("MAIN MENU");
        System.out.println("  1. Login to account");
        System.out.println("  2. Create new account");
        System.out.println("  3. List all accounts");
        System.out.println("  0. Exit");

        String choice = UI.prompt("Option");
        switch (choice) {
            case "1" -> login();
            case "2" -> createAccount();
            case "3" -> bank.listAccounts();
            case "0" -> { return false; }
            default  -> UI.err("Invalid option.");
        }
        return true;
    }

    static boolean accountMenu() {
        UI.header("ACCOUNT MENU — " + session.getOwner());
        System.out.printf("  Balance: $%.2f%n%n", session.getBalance());
        System.out.println("  1. Deposit");
        System.out.println("  2. Withdraw");
        System.out.println("  3. Transfer");
        System.out.println("  4. View statement");
        System.out.println("  5. Logout");
        System.out.println("  0. Exit");

        String choice = UI.prompt("Option");
        switch (choice) {
            case "1" -> deposit();
            case "2" -> withdraw();
            case "3" -> transfer();
            case "4" -> session.printStatement();
            case "5" -> { session = null; UI.ok("Logged out."); }
            case "0" -> { return false; }
            default  -> UI.err("Invalid option.");
        }
        return true;
    }

    static void login() {
        String id = UI.prompt("Account ID (e.g. ACC-1001)");
        Account acc = bank.find(id);
        if (acc == null) { UI.err("Account not found."); return; }
        session = acc;
        UI.ok("Welcome back, " + acc.getOwner() + "!");
    }

    static void createAccount() {
        String name = UI.prompt("Full name");
        double initial = UI.promptDouble("Initial deposit ($)");
        if (initial < 0) { UI.err("Invalid amount."); return; }
        Account acc = bank.createAccount(name, initial);
        UI.ok("Account created: " + acc.getId());
    }

    static void deposit() {
        double amount = UI.promptDouble("Amount to deposit ($)");
        String reason = UI.prompt("Reason");
        if (session.deposit(amount, reason)) UI.ok(String.format("Deposited $%.2f", amount));
        else UI.err("Invalid amount.");
    }

    static void withdraw() {
        double amount = UI.promptDouble("Amount to withdraw ($)");
        String reason = UI.prompt("Reason");
        if (session.withdraw(amount, reason)) UI.ok(String.format("Withdrew $%.2f", amount));
        else UI.err("Insufficient funds or invalid amount.");
    }

    static void transfer() {
        String targetId = UI.prompt("Target account ID");
        Account target = bank.find(targetId);
        if (target == null) { UI.err("Target account not found."); return; }
        if (target == session) { UI.err("Cannot transfer to yourself."); return; }
        double amount = UI.promptDouble("Amount to transfer ($)");
        if (session.transfer(target, amount))
            UI.ok(String.format("Transferred $%.2f to %s", amount, target.getOwner()));
        else UI.err("Insufficient funds or invalid amount.");
    }
}
