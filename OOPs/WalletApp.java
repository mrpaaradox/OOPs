/*
 * JavaPay - a small bank, written to show OOP 1..7 doing real work.
 *
 *     cd OOPs && javac WalletApp.java && java WalletApp
 *
 * Three accounts live through one business day: money arrives, a transfer is
 * refused, an account is frozen, interest is paid, fees are charged, and the
 * day ends with a statement. Every class below is introduced by a comment
 * saying what it is for and which part of the course it demonstrates, so the
 * file can be read top to bottom without prior knowledge of the app.
 */

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
    Insufficient(String id, Money want, Money have) { super(id + " needs " + want + ", has " + have); }
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
    Money available() { return Money.of(paise - floor()); }        // what may still leave
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
// OOP 1 - main: the day itself. Nothing here is a syntax demo; it is one
// customer journey that happens to walk through every class above.
// ===========================================================================
public class WalletApp {
    public static void main(String[] args) throws Exception {
        Bank bank = Bank.get();
        System.out.println("JavaPay  |  " + Bank.branches());
        System.out.println("=".repeat(64));

        // One account of each product. They answer the three questions
        // differently, so from here on they are simply three Accounts.
        Account aarav = bank.open("savings", 0), diya = bank.open("premium", 0), zoya = bank.open("current", 0);

        // Morning: salaries in, then a rent transfer and a supplier payment.
        aarav.deposit(Money.rs(42_000), "salary");
        diya.deposit(Money.rs(6_000), "bonus");
        zoya.deposit(Money.rs(8_000), "invoice");
        aarav.transferTo(diya, Money.rs(18_000), "rent");
        zoya.transferTo(diya, Money.rs(12_000), "supplier");     // dips into the overdraft

        List<Account> accounts = bank.all();
        System.out.println("\naccounts after the morning");
        System.out.printf("  %-7s %-8s %13s   %s%n", "id", "product", "balance", "available");
        for (Account a : accounts)
            System.out.printf("  %-7s %-8s %13s   %s%n", a.id(), a.getClass().getSimpleName(), a.balance(), a.available());

        // Two refusals: more money than the floor allows, and a frozen account.
        System.out.println("\nrefused by the bank");
        try { aarav.withdraw(Money.rs(30_000)); }
        catch (Insufficient e) { System.out.println("  " + e.getMessage()); }
        zoya.freeze();
        try { zoya.deposit(Money.rs(500)); }
        catch (WalletException e) { System.out.println("  " + e.getMessage()); }
        finally { zoya.unfreeze(); }                             // OOP 6: always runs

        // End of day: pay interest, then charge the monthly fees.
        accounts.forEach(a -> a.accrue(365));
        for (Account a : accounts) try { a.settle(); } catch (WalletException e) { System.out.println("  " + a.id() + " fee skipped"); }
        System.out.println("\nafter interest and fees");
        System.out.printf("  %-7s %13s%n", "id", "balance");
        for (Account a : accounts) System.out.printf("  %-7s %13s%n", a.id(), a.balance());

        // The statement is the last three entries, newest last.
        System.out.println("\nstatement  " + aarav.id());
        aarav.recent(3).forEach(t -> System.out.println("  " + t));

        // OOP 6: the copy is edited, and the original is untouched.
        History history = aarav.history(), replay = history.clone();
        replay.points()[replay.points().length - 1] = 1;
        System.out.println("\nhistory       " + history);
        System.out.println("copy, edited  " + replay);

        // A listener sees every transaction, including the last deposit, and the
        // teller quotes a fixed deposit on top of the day's totals.
        int[] seen = {0};
        bank.subscribe(t -> seen[0]++);                          // OOP 6: lambda for a nested interface
        aarav.deposit(Money.rs(1_000), "top-up");
        System.out.println("\nteller        " + bank.teller().fd(diya, 10_000, 12));
        System.out.printf("bank          listener saw %d, ledger %d entries, counts %s, total %s%n",
                seen[0], bank.ledger().size(), new TreeMap<>(bank.counts()), Money.of(Bank.totalPaise(accounts)));
    }
}
