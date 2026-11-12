/*
 * JavaPay - a small bank you drive from the terminal, written to show OOP 1..7.
 *
 *     cd OOPs && javac WalletApp.java && java WalletApp          (interactive)
 *     cd OOPs && javac WalletApp.java && java WalletApp demo     (scripted day)
 *
 * Type help at the prompt: open accounts, deposit, withdraw, transfer, freeze,
 * pay interest, charge fees, read a statement, watch each entry as it is
 * recorded. The program reads a command, looks it up and hands it to the
 * objects below, so every run is a different day at the bank; `demo` replays
 * the recorded day from the earlier version for anyone who just wants to look.
 *
 * Every class is introduced by a comment saying what it is for and which part
 * of the course it demonstrates, so the file reads top to bottom.
 */

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

// ===========================================================================
// OOP 7 - Kind: the six events a balance can record. The enum carries its own
// method, so "does this add money?" is answered by the type itself instead of
// by an if-chain copied into every caller.
// ===========================================================================
enum Kind {
    DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT, FEE, INTEREST;

    // One rule in one place: these three raise the balance, the rest lower it.
    boolean credit() { return this == DEPOSIT || this == TRANSFER_IN || this == INTEREST; }
}

// ===========================================================================
// OOP 1 + OOP 4 - Money: the value object. An amount is a whole number of paise
// (1 rupee = 100 paise) in a final long, never a double, because 0.10 has no
// exact double form and a bank must not lose a paisa to rounding. Nothing
// mutates a Money; arithmetic returns a new object. Only toString() turns money
// into text, and only it knows about the rupee sign.
// ===========================================================================
final class Money {
    final long paise;                                   // the entire state, assigned once
    Money(long paise) { this.paise = paise; }           // built through the factories below

    // OOP 1: two named constructors, so every call site states its unit.
    static Money rs(long rupees) { return new Money(rupees * 100); }
    static Money of(long paise) { return new Money(paise); }

    // The only two operations banking needs here, both returning fresh objects.
    Money plus(Money m) { return new Money(paise + m.paise); }
    Money times(double factor) { return new Money(Math.round(paise * factor)); }

    // Reading "1,250.75" from the terminal. BigDecimal shifts the point two
    // places and refuses fractions of a paisa; Double.parseDouble would drag
    // the same rounding error back in that this class exists to avoid.
    static Money parse(String text) {
        try { return new Money(new BigDecimal(text.replace(",", "")).movePointRight(2).longValueExact()); }
        catch (ArithmeticException | NumberFormatException e) { throw new IllegalArgumentException("not an amount: " + text); }
    }

    // OOP 4: value equality - two notes of 500 are the same 500.
    public boolean equals(Object o) { return o instanceof Money m && m.paise == paise; }
    public int hashCode() { return Long.hashCode(paise); }

    // Negative balances read as -Rs 4,060.00 rather than Rs -4,060.00.
    public String toString() { return (paise < 0 ? "-\u20B9" : "\u20B9") + String.format("%,.2f", Math.abs(paise) / 100.0); }
}

// ===========================================================================
// OOP 6 + OOP 2 - Tx: one line of the ledger, immutable once constructed, with
// a natural order so a list of them can be sorted. Its id comes from a static
// counter, which is a single value shared by every Tx in the program.
// ===========================================================================
class Tx implements Comparable<Tx> {
    private static int seq;                             // OOP 2: shared, not per-object
    final int id = ++seq;                               // handed out in construction order
    final Kind kind; final Money amount; final String note;
    Tx(Kind kind, Money amount, String note) { this.kind = kind; this.amount = amount; this.note = note; }

    // OOP 6: ordering by id is what lets Bank sort its ledger copy.
    public int compareTo(Tx o) { return Integer.compare(id, o.id); }
    public String toString() { return "#" + id + " " + kind + " " + amount + " " + note; }
}

// ===========================================================================
// OOP 6 - History: the balance after every transaction, kept as a snapshot the
// caller may hold on to. clone() copies the array as well as the object, so an
// edited copy cannot rewrite the original - shallow versus deep, shown in main.
// ===========================================================================
class History implements Cloneable {
    private final String owner; private long[] points;
    History(String owner, long[] points) { this.owner = owner; this.points = points; }
    long[] points() { return points; }                  // main() uses this to edit the copy
    public History clone() { try { History c = (History) super.clone(); c.points = points.clone(); return c; }
        catch (CloneNotSupportedException e) { throw new AssertionError(e); } }
    // Rupees with two decimals, so the snapshot is readable rather than raw paise.
    public String toString() { return owner + Arrays.stream(points).mapToObj(p -> String.format("%.2f", p / 100.0)).toList(); }
}

// ===========================================================================
// OOP 6 - the two ways a request is refused. WalletException is the family;
// Insufficient is the case worth catching on its own because it carries the
// numbers. Both are checked, so the compiler forces every caller to decide.
// ===========================================================================
@SuppressWarnings("serial") class WalletException extends Exception { WalletException(String m) { super(m); } }
@SuppressWarnings("serial") class Insufficient extends WalletException {
    // "available" is what could actually leave, so it never prints a negative.
    Insufficient(String id, Money want, Money have) {
        super(id + " needs " + want + ", available " + Money.of(Math.max(0, have.paise)));
    }
}

// ===========================================================================
// OOP 5 - two capabilities, deliberately tiny. A class says "I pay interest" or
// "I charge this way" by implementing one; no other class has to know about it.
// ===========================================================================
interface InterestBearing { double rate(); }
@FunctionalInterface interface Fee { long fee(long paise); }   // OOP 6: a lambda is one of these

// ===========================================================================
// OOP 5 + OOP 3 + OOP 4 - Account: the abstract base that runs the bank's rules.
// deposit, withdraw, transferTo and settle are final template methods: they fix
// the order of every money move (frozen, floor, debit, fee, credit, ledger), so
// no product can skip a step. A product only answers the three questions below.
// ===========================================================================
abstract class Account {
    private static int count;                           // OOP 2: numbers accounts SAV-1, PREM-2...
    private final String id;
    protected long paise;                               // OOP 4: visible to subclasses only
    private final List<Tx> trail = new ArrayList<>();
    private boolean frozen;

    // A new account may open with money in it, which is itself a transaction.
    Account(String prefix, long opening) {
        id = prefix + "-" + (++count); paise = opening;
        if (opening > 0) record(Kind.DEPOSIT, opening, "opening");
    }

    // The three questions only the product can answer. floor() is the lowest
    // balance it may reach; make it negative and the account can go overdrawn.
    abstract long floor();
    abstract Money monthlyFee();
    abstract long transferFee(long paise);

    String id() { return id; }
    Money balance() { return Money.of(paise); }
    Money available() { return Money.of(Math.max(0, paise - floor())); }   // what may still leave
    List<Tx> recent(int n) { return trail.subList(Math.max(0, trail.size() - n), trail.size()); }
    void freeze() { frozen = true; } void unfreeze() { frozen = false; }

    // OOP 3: overloading - the short form is the long form with a default note.
    final void deposit(Money m) throws WalletException { deposit(m, "deposit"); }
    final void deposit(Money m, String note) throws WalletException {
        if (m.paise <= 0) throw new IllegalArgumentException("amount must be positive");
        alive(); paise += m.paise; record(Kind.DEPOSIT, m.paise, note);
    }

    // The rule for "money leaves" lives in pay(), so withdrawing is a rename.
    final void withdraw(Money m) throws WalletException { alive(); pay(m.paise, "withdraw", Kind.WITHDRAW); }

    // A transfer is two debits and two ledger lines: the sender pays the amount
    // plus its own product's fee, the receiver is credited the amount alone.
    // OOP 3: transferFee() is the polymorphic call - Savings pays nothing, Current
    // pays 0.5% - yet this method never names either of them.
    final void transferTo(Account to, Money m, String note) throws WalletException {
        alive(); to.alive();
        long fee = transferFee(m.paise);
        if (m.paise + fee > paise - floor()) throw new Insufficient(id, Money.of(m.paise + fee), Money.of(paise - floor()));
        paise -= m.paise; record(Kind.TRANSFER_OUT, m.paise, note + " -> " + to.id);
        if (fee > 0) { paise -= fee; record(Kind.FEE, fee, "transfer fee"); }
        to.paise += m.paise; to.record(Kind.TRANSFER_IN, m.paise, "from " + id);
    }

    // Month end: charge the product's fee, if it has one.
    final void settle() throws WalletException { Money f = monthlyFee(); if (f.paise > 0) pay(f.paise, "monthly fee", Kind.FEE); }

    // OOP 4: instanceof with a pattern variable. Only accounts that implement
    // InterestBearing earn interest, and the cast happens in the binding.
    final void accrue(int days) {
        if (this instanceof InterestBearing b) {
            long i = Math.round(paise * b.rate() * days / 365);
            if (i > 0) { paise += i; record(Kind.INTEREST, i, "interest"); }
        }
    }

    // OOP 5: another final template method. Replaying the ledger is a rule, not a
    // choice, so a product cannot reimplement it - it just supplies the trail.
    final History history() {
        long[] pts = new long[trail.size()]; long run = 0;
        for (int i = 0; i < pts.length; i++) {
            Tx t = trail.get(i); run += t.kind.credit() ? t.amount.paise : -t.amount.paise; pts[i] = run;
        }
        return new History(id, pts);
    }

    // Guards every operation shares.
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

// ===========================================================================
// The three products differ only in their answers. Savings is the base; Premium
// is a Savings with stricter numbers, three levels deep; Current is not a
// Savings at all, and overrides floor() to allow a negative balance.
// ===========================================================================
class Savings extends Account implements InterestBearing {
    Savings(long opening) { this("SAV", opening); }      // OOP 3: constructor chaining
    Savings(String prefix, long opening) { super(prefix, opening); }
    long floor() { return 10_000; }                      // Rs 100 always stays behind
    Money monthlyFee() { return Money.rs(0); }
    long transferFee(long p) { return 0; }
    public double rate() { return 0.035; }
}
class Premium extends Savings {
    Premium(long opening) { super("PREM", opening); }
    long floor() { return 500_000; }                     // Rs 5,000 always stays behind
    Money monthlyFee() { return super.monthlyFee().plus(Money.rs(99)); }   // OOP 3: super call
    public double rate() { return 0.055; }
}
class Current extends Account {
    private final long overdraft;
    private final Fee feeRule = p -> Math.max(100, p / 200);           // OOP 6: a lambda as the rule
    Current(long opening, long overdraft) { super("CUR", opening); this.overdraft = overdraft; }
    long floor() { return -overdraft; }                  // OOP 4: the overridden hook
    Money monthlyFee() { return Money.rs(499); }
    long transferFee(long p) { return feeRule.fee(p); }  // 0.5%, but never under Rs 1
}

// ===========================================================================
// OOP 2 + OOP 7 - Bank: exactly one exists. The private constructor plus the
// static instance is the singleton, and the static block runs once, before any
// account is opened, to fill the branches. It also owns everything shared: the
// ledger, the accounts by id, a count per transaction kind, and the listeners.
// ===========================================================================
final class Bank {
    private static final Bank INSTANCE;
    private static final List<String> BRANCHES = new ArrayList<>();
    static { BRANCHES.addAll(List.of("MUM-01", "BLR-02")); INSTANCE = new Bank(); }   // OOP 2: static block
    private final Map<String, Account> byId = new LinkedHashMap<>();    // OOP 7: keeps opening order
    private final Vector<Tx> ledger = new Vector<>();                   // OOP 7: the legacy collection
    private final Map<Kind, Integer> counts = new HashMap<>();           // OOP 7: enum keys, Integer values
    private final List<Listener> listeners = new ArrayList<>();

    private Bank() { }
    static Bank get() { return INSTANCE; }
    static String branches() { return String.join(", ", BRANCHES); }
    Map<Kind, Integer> counts() { return counts; }
    List<Account> all() { return new ArrayList<>(byId.values()); }

    // OOP 3: a factory. main() names a product as a string; the switch picks the
    // class, and from then on the rest of the program speaks only Account.
    Account open(String kind, long opening) {
        Account a = switch (kind) { case "premium" -> new Premium(opening); case "current" -> new Current(opening, Money.rs(5_000).paise); default -> new Savings(opening); };
        byId.put(a.id(), a); return a;
    }

    // The shell types an id; the map answers. Null means "no such account", which
    // the command layer turns into a helpful message instead of a stack trace.
    Account find(String id) { return byId.get(id.toUpperCase()); }

    // Every transaction is recorded in both directions: the Vector keeps the
    // arrival order, the map counts kinds (autoboxing in and out), and whoever
    // subscribed hears about it.
    void post(Tx t) {
        ledger.add(t); counts.merge(t.kind, 1, Integer::sum);
        listeners.forEach(l -> l.on(t));
    }

    // OOP 6 + OOP 7: sorting a copy by Tx's Comparable, so callers can never
    // reorder the ledger itself.
    List<Tx> ledger() { List<Tx> c = new ArrayList<>(ledger); c.sort(null); return c; }

    // OOP 6: a bounded wildcard - a list of anything that is an Account.
    static long totalPaise(List<? extends Account> as) { return as.stream().mapToLong(a -> a.paise).sum(); }

    // OOP 5: a nested interface. The bank announces; it does not know who listens.
    interface Listener { void on(Tx t); }
    void subscribe(Listener l) { listeners.add(l); }

    // OOP 2: an inner class, bound to this Bank instance and free to read it.
    class Teller {
        String fd(Account a, long rupees, int months) { Money m = Money.rs(rupees); return a.id() + " FD " + m + " x " + months + "m -> " + m.times(1 + 0.071 * months / 12); }
    }
    Teller teller() { return new Teller(); }
}

// ===========================================================================
// OOP 1 + OOP 6 - WalletApp: the terminal. It reads one command per line, finds
// the objects it names and calls them; the objects do the work and know nothing
// about the keyboard. Every command may fail with a checked exception, and one
// catch block in the loop turns the failure into a message instead of a crash.
// ===========================================================================
public class WalletApp {
    private static final Bank BANK = Bank.get();
    private static final BufferedReader IN = new BufferedReader(new InputStreamReader(System.in));
    private static final Set<String> PRODUCTS = Set.of("savings", "premium", "current");

    public static void main(String[] args) throws Exception {
        System.out.println("JavaPay  |  " + Bank.branches());
        System.out.println("=".repeat(64));
        if (args.length > 0 && args[0].equalsIgnoreCase("demo")) { demo(); return; }
        System.out.println("Type help for commands, demo for a scripted day, quit to leave.");
        loop();
    }

    // Reads until quit or end of input. Nothing here knows what a command does.
    private static void loop() throws Exception {
        while (true) {
            System.out.print("\n> ");
            System.out.flush();
            String line = IN.readLine();                       // null at end of input
            if (line == null) { System.out.println(" bye"); return; }
            String[] c = line.trim().split("\\s+");
            if (c[0].isEmpty()) continue;
            try {
                if (!dispatch(c)) { System.out.println(" bye"); return; }
            } catch (WalletException | IllegalArgumentException e) {
                System.out.println("  ! " + e.getMessage());   // one place handles every refusal
            }
        }
    }

    // The command table. Returns false when the session should end.
    private static boolean dispatch(String[] c) throws WalletException {
        switch (c[0].toLowerCase()) {
            case "help" -> help();
            case "open" -> open(c);
            case "accounts" -> accounts();
            case "deposit" -> deposit(c);
            case "withdraw" -> withdraw(c);
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
            case "demo" -> demo();
            case "quit", "exit" -> { return false; }
            default -> throw new IllegalArgumentException("unknown command '" + c[0] + "' - try help");
        }
        return true;
    }

    private static void help() {
        System.out.print("""
                  open savings|premium|current [opening]   start an account
                  accounts                                 balances and products
                  deposit  <account> <amount> [note]       add money
                  withdraw <account> <amount>              take money out
                  transfer <from> <to> <amount> [note]     move money, fee applies
                  statement <account> [n]                  last n entries, default 5
                  history <account>                        balance after each entry
                  watch                                    announce every new entry
                  freeze | unfreeze <account>              stop or resume activity
                  accrue [days]                            pay interest, default 365
                  fees                                     charge the monthly fees
                  teller <account> <rupees> <months>       fixed-deposit quote
                  bank                                     ledger and totals
                  demo                                     replay a scripted day
                  help | quit
                """);
    }

    // ---- one method per command, each a thin translation of words to calls ----

    private static void open(String[] c) throws WalletException {
        need(c, 2, "open savings|premium|current [opening]");
        String kind = c[1].toLowerCase();
        if (!PRODUCTS.contains(kind)) throw new IllegalArgumentException("no such product '" + c[1] + "'");
        Account a = BANK.open(kind, c.length > 2 ? Money.parse(c[2]).paise : 0);
        System.out.println("  opened " + a + "  available " + a.available());
    }

    private static void accounts() {
        if (BANK.all().isEmpty()) { System.out.println("  no accounts yet - try: open savings 5000"); return; }
        System.out.printf("  %-7s %-8s %13s   %s%n", "id", "product", "balance", "available");
        for (Account a : BANK.all())
            System.out.printf("  %-7s %-8s %13s   %s%n", a.id(), a.getClass().getSimpleName(), a.balance(), a.available());
    }

    private static void deposit(String[] c) throws WalletException {
        need(c, 3, "deposit <account> <amount> [note]");
        Account a = account(c[1]);
        a.deposit(Money.parse(c[2]), c.length > 3 ? words(c, 3) : "deposit");
        System.out.println("  " + a + "  available " + a.available());
    }

    private static void withdraw(String[] c) throws WalletException {
        need(c, 3, "withdraw <account> <amount>");
        Account a = account(c[1]);
        a.withdraw(Money.parse(c[2]));
        System.out.println("  " + a + "  available " + a.available());
    }

    private static void transfer(String[] c) throws WalletException {
        need(c, 4, "transfer <from> <to> <amount> [note]");
        Account from = account(c[1]), to = account(c[2]);
        if (from == to) throw new IllegalArgumentException("cannot transfer to the same account");
        from.transferTo(to, Money.parse(c[3]), c.length > 4 ? words(c, 4) : "transfer");
        System.out.println("  " + from + "  |  " + to);
    }

    private static void statement(String[] c) {
        need(c, 2, "statement <account> [n]");
        Account a = account(c[1]);
        int n = c.length > 2 ? number(c[2]) : 5;
        System.out.println("  last " + n + " entries of " + a.id());
        a.recent(n).forEach(t -> System.out.println("    " + t));
    }

    private static void history(String[] c) {
        need(c, 2, "history <account>");
        Account a = account(c[1]);
        History h = a.history();
        if (h.points().length == 0) { System.out.println("  " + a.id() + " has no entries yet"); return; }
        System.out.println("  " + a.id() + " balance after each entry");
        System.out.println("    " + h);
        // OOP 6: editing the clone leaves the line above exactly as it was.
        History copy = h.clone(); copy.points()[copy.points().length - 1] = 1;
        System.out.println("    copy, edited " + copy);
    }

    // OOP 5 + OOP 6: subscribe a lambda to the bank's nested Listener interface,
    // and every transaction from now on is announced as it is recorded.
    private static void watch() {
        BANK.subscribe(t -> System.out.println("    heard " + t));
        System.out.println("  watching new transactions - they will appear as they happen");
    }

    private static void freeze(String[] c, boolean frozen) {
        need(c, 2, (frozen ? "freeze" : "unfreeze") + " <account>");
        Account a = account(c[1]);
        if (frozen) a.freeze(); else a.unfreeze();
        System.out.println("  " + a.id() + (frozen ? " is frozen" : " is active again"));
    }

    private static void accrue(String[] c) {
        int days = c.length > 1 ? number(c[1]) : 365;
        for (Account a : BANK.all()) a.accrue(days);
        System.out.println("  interest for " + days + " days");
        accounts();
    }

    private static void fees() {
        for (Account a : BANK.all()) {
            try { a.settle(); }
            catch (WalletException e) { System.out.println("  ! " + e.getMessage()); }
        }
        System.out.println("  monthly fees charged");
        accounts();
    }

    private static void teller(String[] c) {
        need(c, 4, "teller <account> <rupees> <months>");
        System.out.println("  " + BANK.teller().fd(account(c[1]), number(c[2]), number(c[3])));
    }

    private static void bankSummary() {
        System.out.println("  accounts  " + BANK.all().size());
        System.out.println("  ledger    " + BANK.ledger().size() + " entries, newest last");
        System.out.println("  by kind   " + new TreeMap<>(BANK.counts()));
        System.out.println("  held      " + Money.of(Bank.totalPaise(BANK.all())));
    }

    // A recorded session, typed through the same commands a user would type. It
    // needs a fresh session because it refers to the ids it is about to create.
    private static void demo() throws WalletException {
        if (!BANK.all().isEmpty()) throw new IllegalArgumentException("demo needs a fresh session - restart without input");
        String[] day = {
            "open savings", "open premium", "open current",
            "deposit SAV-1 42000 salary", "deposit PREM-2 6000 bonus", "deposit CUR-3 8000 invoice",
            "transfer SAV-1 PREM-2 18000 rent", "transfer CUR-3 PREM-2 12000 supplier",
            "accounts", "watch",
            "withdraw SAV-1 30000",
            "freeze CUR-3", "deposit CUR-3 500", "unfreeze CUR-3",
            "accrue 365", "fees",
            "statement SAV-1 3", "history SAV-1",
            "teller PREM-2 10000 12", "bank"
        };
        for (String line : day) {
            System.out.println("\n> " + line);
            try { dispatch(line.split(" ")); }
            catch (WalletException | IllegalArgumentException e) { System.out.println("  ! " + e.getMessage()); }
        }
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
    private static void need(String[] c, int n, String usage) {
        if (c.length < n) throw new IllegalArgumentException("usage: " + usage);
    }
}
