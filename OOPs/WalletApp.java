/*
 * JavaPay - a small bank you drive from the terminal, demonstrating OOP 1..7.
 *
 *     javac WalletApp.java && java WalletApp
 */

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

// OOP 7 - the six events a balance can record; the enum says whether one adds money.
enum Kind {
    DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT, FEE, INTEREST;
    boolean credit() { return this == DEPOSIT || this == TRANSFER_IN || this == INTEREST; }
}

// OOP 1 + OOP 4 - Money: whole paise in a final long, never a double, because 0.10
// has no exact double form. Arithmetic returns a new object.
final class Money {
    final long paise;
    Money(long paise) { this.paise = paise; }

    // OOP 1: named constructors, so every call site states its unit.
    static Money rs(long rupees) { return new Money(rupees * 100); }
    static Money of(long paise) { return new Money(paise); }
    Money plus(Money m) { return new Money(paise + m.paise); }
    Money times(double factor) { return new Money(Math.round(paise * factor)); }

    // BigDecimal shifts the point and rejects fractions of a paisa; a double would round.
    static Money parse(String text) {
        try { return new Money(new BigDecimal(text.replace(",", "")).movePointRight(2).longValueExact()); }
        catch (ArithmeticException | NumberFormatException e) { throw new IllegalArgumentException("not an amount: " + text); }
    }

    // OOP 4: value equality - two notes of 500 are the same 500.
    @Override public boolean equals(Object o) { return o instanceof Money m && m.paise == paise; }
    @Override public int hashCode() { return Long.hashCode(paise); }
    @Override public String toString() { return (paise < 0 ? "-\u20B9" : "\u20B9") + String.format("%,.2f", Math.abs(paise) / 100.0); }
}

// OOP 6 + OOP 2 - one immutable ledger line, ordered by id; the id is a static counter.
class Tx implements Comparable<Tx> {
    private static int seq;
    final int id = ++seq;
    final Kind kind; final Money amount; final String note;
    Tx(Kind kind, Money amount, String note) { this.kind = kind; this.amount = amount; this.note = note; }
    public int compareTo(Tx o) { return Integer.compare(id, o.id); }
    @Override public String toString() { return "#" + id + " " + kind + " " + amount + " " + note; }
}

// OOP 6 - the balance after every transaction; clone() copies the array too, so an
// edited copy cannot rewrite the original.
class History implements Cloneable {
    private final String owner; private long[] points;
    History(String owner, long[] points) { this.owner = owner; this.points = points; }
    long[] points() { return points; }
    @Override public History clone() { try { History c = (History) super.clone(); c.points = points.clone(); return c; }
        catch (CloneNotSupportedException e) { throw new AssertionError(e); } }
    @Override public String toString() { return owner + Arrays.stream(points).mapToObj(p -> String.format("%.2f", p / 100.0)).toList(); }
}

// OOP 6 - a refused request. Both are checked, so every caller must decide;
// Insufficient is caught separately because it carries the numbers.
@SuppressWarnings("serial") class WalletException extends Exception { WalletException(String m) { super(m); } }
@SuppressWarnings("serial") class Insufficient extends WalletException {
    Insufficient(String id, Money want, Money have) { super(id + " needs " + want + ", available " + Money.of(Math.max(0, have.paise))); }
}

// OOP 5 - tiny capabilities: a class opts in to earning interest or to a fee rule.
interface InterestBearing { double rate(); }
@FunctionalInterface interface Fee { long fee(long paise); }   // OOP 6: a lambda target

// OOP 5 + OOP 3 + OOP 4 - the abstract base. Its money moves are final template
// methods, so no product can skip a step; a product answers the three questions below.
abstract class Account {
    private static int count;                           // OOP 2: numbers SAV-1, PREM-2...
    private final String id;
    protected long paise;                               // OOP 4: visible to subclasses only
    private final List<Tx> trail = new ArrayList<>();
    private boolean frozen;

    Account(String prefix, long opening) {
        id = prefix + "-" + (++count); paise = opening;
        if (opening > 0) record(Kind.DEPOSIT, opening, "opening");
    }

    // The lowest balance it may reach; negative means an overdraft is allowed.
    abstract long floor();
    abstract Money monthlyFee();
    abstract long transferFee(long paise);

    String id() { return id; }
    Money balance() { return Money.of(paise); }
    Money available() { return Money.of(Math.max(0, paise - floor())); }
    List<Tx> recent(int n) { return trail.subList(Math.max(0, trail.size() - n), trail.size()); }
    void freeze() { frozen = true; } void unfreeze() { frozen = false; }

    // OOP 3: overloading - the short form is the long form with a default note.
    final void deposit(Money m) throws WalletException { deposit(m, "deposit"); }
    final void deposit(Money m, String note) throws WalletException {
        if (m.paise <= 0) throw new IllegalArgumentException("amount must be positive");
        alive(); paise += m.paise; record(Kind.DEPOSIT, m.paise, note);
    }
    final void withdraw(Money m) throws WalletException { alive(); pay(m.paise, "withdraw", Kind.WITHDRAW); }

    // The sender pays the amount plus its own product's fee; the receiver gets the
    // amount. OOP 3: the fee comes from the overridden transferFee(), not a type test.
    final void transferTo(Account to, Money m, String note) throws WalletException {
        alive(); to.alive();
        long fee = transferFee(m.paise);
        if (m.paise + fee > paise - floor()) throw new Insufficient(id, Money.of(m.paise + fee), Money.of(paise - floor()));
        paise -= m.paise; record(Kind.TRANSFER_OUT, m.paise, note + " -> " + to.id);
        if (fee > 0) { paise -= fee; record(Kind.FEE, fee, "transfer fee"); }
        to.paise += m.paise; to.record(Kind.TRANSFER_IN, m.paise, "from " + id);
    }

    final void settle() throws WalletException { Money f = monthlyFee(); if (f.paise > 0) pay(f.paise, "monthly fee", Kind.FEE); }

    // OOP 4: instanceof with a pattern variable - only interest-bearing accounts earn.
    final void accrue(int days) {
        if (this instanceof InterestBearing b) {
            long i = Math.round(paise * b.rate() * days / 365);
            if (i > 0) { paise += i; record(Kind.INTEREST, i, "interest"); }
        }
    }

    // Replaying the ledger into running balances is a rule, so it is final too.
    final History history() {
        long[] pts = new long[trail.size()]; long run = 0;
        for (int i = 0; i < pts.length; i++) {
            Tx t = trail.get(i); run += t.kind.credit() ? t.amount.paise : -t.amount.paise; pts[i] = run;
        }
        return new History(id, pts);
    }

    final void alive() throws WalletException { if (frozen) throw new WalletException(id + " is frozen"); }
    private void pay(long amount, String note, Kind kind) throws WalletException {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (amount > paise - floor()) throw new Insufficient(id, Money.of(amount), Money.of(paise - floor()));
        paise -= amount; record(kind, amount, note);
    }
    private void record(Kind kind, long amount, String note) {
        Tx t = new Tx(kind, Money.of(amount), note); trail.add(t); Bank.get().post(t);
    }

    // OOP 4: two handles are equal when they are the same account.
    @Override public boolean equals(Object o) { return o instanceof Account a && id.equals(a.id); }
    @Override public int hashCode() { return id.hashCode(); }
    @Override public String toString() { return id + " " + getClass().getSimpleName() + " " + balance(); }
}

// The three products differ only in their answers; Current allows an overdraft.
class Savings extends Account implements InterestBearing {
    Savings(long opening) { this("SAV", opening); }      // OOP 3: constructor chaining
    Savings(String prefix, long opening) { super(prefix, opening); }
    long floor() { return 10_000; }                      // Rs 100 stays behind
    Money monthlyFee() { return Money.rs(0); }
    long transferFee(long p) { return 0; }
    public double rate() { return 0.035; }
}
class Premium extends Savings {
    Premium(long opening) { super("PREM", opening); }
    long floor() { return 500_000; }                     // Rs 5,000 stays behind
    Money monthlyFee() { return super.monthlyFee().plus(Money.rs(99)); }   // OOP 3: super call
    public double rate() { return 0.055; }
}
class Current extends Account {
    private final long overdraft;
    private final Fee feeRule = p -> Math.max(100, p / 200);   // OOP 6: a lambda is the rule
    Current(long opening, long overdraft) { super("CUR", opening); this.overdraft = overdraft; }
    long floor() { return -overdraft; }                  // OOP 4: the overridden hook
    Money monthlyFee() { return Money.rs(499); }
    long transferFee(long p) { return feeRule.fee(p); }  // 0.5%, never under Rs 1
}

// OOP 2 + OOP 7 - Bank: exactly one exists (private constructor plus a static
// instance, filled by a static block). It owns the ledger, accounts, counts, listeners.
final class Bank {
    private static final Bank INSTANCE;
    private static final List<String> BRANCHES = new ArrayList<>();
    static { BRANCHES.addAll(List.of("MUM-01", "BLR-02")); INSTANCE = new Bank(); }   // OOP 2: static block
    private final Map<String, Account> byId = new LinkedHashMap<>();    // OOP 7: opening order
    private final Vector<Tx> ledger = new Vector<>();                   // OOP 7: legacy collection
    private final Map<Kind, Integer> counts = new HashMap<>();          // OOP 7: enum keys
    private final List<Listener> listeners = new ArrayList<>();

    private Bank() { }
    static Bank get() { return INSTANCE; }
    static String branches() { return String.join(", ", BRANCHES); }
    Map<Kind, Integer> counts() { return counts; }
    List<Account> all() { return new ArrayList<>(byId.values()); }

    // OOP 3: a factory. The shell names a product; from then on it speaks Account.
    Account open(String kind, long opening) {
        Account a = switch (kind) {
            case "savings" -> new Savings(opening);
            case "premium" -> new Premium(opening);
            case "current" -> new Current(opening, Money.rs(5_000).paise);
            default -> throw new IllegalArgumentException("no such product '" + kind + "'");
        };
        byId.put(a.id(), a); return a;
    }
    Account find(String id) { return byId.get(id.toUpperCase()); }

    // Each transaction is kept in arrival order, counted by kind, and announced.
    void post(Tx t) {
        ledger.add(t); counts.merge(t.kind, 1, Integer::sum);
        listeners.forEach(l -> l.on(t));
    }

    // OOP 6 + OOP 7: sort a copy by Tx's Comparable; callers cannot reorder the ledger.
    List<Tx> ledger() { List<Tx> c = new ArrayList<>(ledger); c.sort(null); return c; }

    // OOP 6: a bounded wildcard - a list of anything that is an Account.
    static long totalPaise(List<? extends Account> as) { return as.stream().mapToLong(a -> a.paise).sum(); }

    // OOP 5: a nested interface. The bank announces; it does not know who listens.
    interface Listener { void on(Tx t); }
    void subscribe(Listener l) { listeners.add(l); }

    // OOP 2: an inner class, bound to this Bank instance.
    class Teller {
        String fd(Account a, long rupees, int months) { Money m = Money.rs(rupees); return a.id() + " FD " + m + " x " + months + "m -> " + m.times(1 + 0.071 * months / 12); }
    }
    Teller teller() { return new Teller(); }
}

// OOP 1 + OOP 6 - WalletApp: the terminal. main() reads a line, dispatch() picks a
// method, and each method turns words into calls on the objects above. One catch
// block turns a refusal into an "!" line instead of a stack trace.
public class WalletApp {
    private static final Bank BANK = Bank.get();
    private static final BufferedReader IN = new BufferedReader(new InputStreamReader(System.in));
    private static final String HEAD = "  %-7s %-8s %13s   %s%n";
    // Minimum word count per command; help lists the full syntax.
    private static final Map<String, Integer> ARITY = Map.of(
            "open", 2, "deposit", 3, "withdraw", 3, "transfer", 4, "statement", 2,
            "history", 2, "freeze", 2, "unfreeze", 2, "teller", 4);

    public static void main(String[] args) throws Exception {
        System.out.println("JavaPay  |  " + Bank.branches() + "\n" + "=".repeat(64));
        System.out.println("Type help for commands, quit to leave.");
        loop();
    }

    // Read until quit or end of input; nothing here knows what a command does.
    private static void loop() throws Exception {
        while (true) {
            System.out.print("\n> "); System.out.flush();
            String line = IN.readLine();
            if (line == null) { System.out.println(" bye"); return; }
            if (line.isBlank()) continue;
            try { if (!dispatch(line.trim().split("\\s+"))) { System.out.println(" bye"); return; } }
            catch (WalletException | IllegalArgumentException e) { System.out.println("  ! " + e.getMessage()); }
        }
    }

    // The command table; false means the session is over.
    private static boolean dispatch(String[] c) throws WalletException {
        String cmd = c[0].toLowerCase();
        if (c.length < ARITY.getOrDefault(cmd, 0)) throw new IllegalArgumentException("usage: " + cmd + " - try help");
        switch (cmd) {
            case "help" -> help();
            case "open" -> open(c);
            case "accounts" -> accounts();
            case "deposit" -> move(c, true);
            case "withdraw" -> move(c, false);
            case "transfer" -> transfer(c);
            case "statement" -> statement(c);
            case "history" -> history(c);
            case "watch" -> watch();
            case "freeze" -> freeze(c, true);
            case "unfreeze" -> freeze(c, false);
            case "accrue", "interest" -> accrue(c);
            case "fees" -> fees();
            case "teller" -> teller(c);
            case "bank" -> bankSummary();
            case "quit", "exit" -> { return false; }
            default -> throw new IllegalArgumentException("unknown command '" + cmd + "' - try help");
        }
        return true;
    }

    private static void help() {
        System.out.print("""
                  open <savings|premium|current> [opening]  start an account
                  accounts                                  balances and products
                  deposit <acc> <amount> [note]             add money
                  withdraw <acc> <amount>                   take money out
                  transfer <from> <to> <amount> [note]      fee applies
                  statement <acc> [n] | history <acc>       entries / running balance
                  watch | bank                              observe / totals
                  freeze | unfreeze <acc>                   stop / resume
                  accrue [days] | fees                      interest / monthly fees
                  teller <acc> <rupees> <months>            FD quote
                  help | quit
                """);
    }

    // ---- one method per command, each a thin translation of words to calls ----
    private static void open(String[] c) {
        Account a = BANK.open(c[1].toLowerCase(), c.length > 2 ? Money.parse(c[2]).paise : 0);
        System.out.println("  opened " + a + "  available " + a.available());
    }
    private static void accounts() {
        if (BANK.all().isEmpty()) { System.out.println("  no accounts - try: open savings 5000"); return; }
        System.out.printf(HEAD, "id", "product", "balance", "available");
        for (Account a : BANK.all()) System.out.printf(HEAD, a.id(), a.getClass().getSimpleName(), a.balance(), a.available());
    }
    // deposit and withdraw are the same translation with one verb changed.
    private static void move(String[] c, boolean in) throws WalletException {
        Account a = account(c[1]);
        if (in) a.deposit(Money.parse(c[2]), c.length > 3 ? words(c, 3) : "deposit");
        else a.withdraw(Money.parse(c[2]));
        System.out.println("  " + a + "  available " + a.available());
    }
    private static void transfer(String[] c) throws WalletException {
        Account from = account(c[1]), to = account(c[2]);
        if (from.equals(to)) throw new IllegalArgumentException("cannot transfer to the same account");
        from.transferTo(to, Money.parse(c[3]), c.length > 4 ? words(c, 4) : "transfer");
        System.out.println("  " + from + "  |  " + to);
    }
    private static void statement(String[] c) {
        Account a = account(c[1]);
        int n = c.length > 2 ? number(c[2]) : 5;
        System.out.println("  last " + n + " entries of " + a.id());
        a.recent(n).forEach(t -> System.out.println("    " + t));
    }
    private static void history(String[] c) {
        Account a = account(c[1]);
        History h = a.history();
        if (h.points().length == 0) { System.out.println("  " + a.id() + " has no entries yet"); return; }
        System.out.println("  " + a.id() + " balance after each entry\n    " + h);
        // OOP 6: editing the clone leaves the line above exactly as it was.
        History copy = h.clone(); copy.points()[copy.points().length - 1] = 1;
        System.out.println("    copy, edited " + copy);
    }
    // OOP 5 + OOP 6: subscribe a lambda to Bank.Listener; new entries are announced.
    private static void watch() {
        BANK.subscribe(t -> System.out.println("    heard " + t));
        System.out.println("  watching new transactions - they will appear as they happen");
    }
    private static void freeze(String[] c, boolean on) {
        Account a = account(c[1]);
        if (on) a.freeze(); else a.unfreeze();
        System.out.println("  " + a.id() + (on ? " is frozen" : " is active again"));
    }
    private static void accrue(String[] c) {
        int days = c.length > 1 ? number(c[1]) : 365;
        BANK.all().forEach(a -> a.accrue(days));
        System.out.println("  interest for " + days + " days");
        accounts();
    }
    private static void fees() {
        for (Account a : BANK.all()) try { a.settle(); }
        catch (WalletException e) { System.out.println("  ! " + e.getMessage()); }
        System.out.println("  monthly fees charged");
        accounts();
    }
    private static void teller(String[] c) {
        System.out.println("  " + BANK.teller().fd(account(c[1]), number(c[2]), number(c[3])));
    }
    private static void bankSummary() {
        List<Account> all = BANK.all();
        System.out.printf("  accounts  %d%n  ledger    %d entries, newest last%n  by kind   %s%n  held      %s%n",
                all.size(), BANK.ledger().size(), new TreeMap<>(BANK.counts()), Money.of(Bank.totalPaise(all)));
    }

    // ---- argument helpers: every one fails with a sentence, never a trace ----
    private static Account account(String id) {
        Account a = BANK.find(id);
        if (a == null) throw new IllegalArgumentException("no account " + id + " - try accounts");
        return a;
    }
    private static int number(String text) {
        try { return Integer.parseInt(text); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("not a number: " + text); }
    }
    private static String words(String[] c, int from) { return String.join(" ", Arrays.copyOfRange(c, from, c.length)); }
}
