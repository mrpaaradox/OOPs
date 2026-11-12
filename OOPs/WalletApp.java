/*
 * JavaPay - one small bank, written to show OOP 1..7 working together.
 *
 *     cd OOPs && javac WalletApp.java && java WalletApp
 *
 * WHAT IT DOES
 *   Opens three accounts, moves money between them, charges fees, pays interest
 *   and prints a statement: one business day of a bank, start to finish. Every
 *   idea from the seven course files appears once here, doing a real job.
 *
 * HOW MONEY IS HANDLED
 *   Money holds paise (1 rupee = 100 paise) in a final long, never a double,
 *   because 0.10 is exact in a long and not in binary floating point - so no
 *   balance can ever drift by a paisa. A Money never changes: every operation
 *   returns a new object. An Account stores a plain paise field instead, so the
 *   common path allocates nothing. "Rs 1,234.56" exists only in Money.toString().
 *
 * WHY IT IS SHAPED THIS WAY
 *   An Account never knows which product it is. Four final template methods
 *   (deposit, withdraw, transferTo, settle) fix the order of every money move:
 *   frozen check, floor check, debit, fee, credit, ledger entry. A product only
 *   answers floor() (lowest balance allowed; negative means overdraft),
 *   monthlyFee() and transferFee(). So adding a product never edits an existing
 *   algorithm, and no caller has to ask "which subclass are you?". Bank is a
 *   singleton because one ledger is shared by the whole app.
 *
 * WHO IS WHO                     what it is for                                   course
 * Kind                           six transaction types, which ones add money      OOP7
 * Money                          immutable amount, equals/hashCode/toString       OOP1/4
 * Tx                             one ledger line, Comparable by id                OOP6/2
 * History                        balance over time, clone copies the array        OOP6
 * WalletException, Insufficient  failures a customer can cause                    OOP6
 * InterestBearing, Fee           pays interest / charges by rule                  OOP5/6
 * Account                        abstract: balance, trail, 4 final operations     OOP5
 * Savings                        floor Rs 100, 3.5% a year, free transfers        OOP3
 * Premium                        Savings + floor Rs 5,000, Rs 99 monthly          OOP3
 * Current                        floor -overdraft, Rs 499 fee, 0.5% transfer      OOP4
 * Bank                           the only instance: branches, ledger, counts      OOP2/7
 * WalletApp                      main(): the day that exercises it all            OOP1
 *
 * EVERYTHING ELSE THE COURSE TAUGHT        (each is also tagged // OOPn inline)
 *   Three-level inheritance Savings > Premium with super calls and constructor
 *   chaining; overloading of deposit(); runtime polymorphism through floor()
 *   and transferFee(); protected state; getClass(); instanceof pattern;
 *   abstract hooks; interfaces as capabilities; Comparable; Cloneable
 *   deep copy; checked exceptions; try/finally; lambdas as Fee and Listener;
 *   wildcard generics in totalPaise(); enum behaviour; ArrayList, Vector,
 *   LinkedHashMap, HashMap and TreeMap; sorting a copy by Comparable.
 *
 * READING ORDER
 *   Kind, Money, Tx, History, exceptions, interfaces, Account, the three
 *   products, Bank, then main() last - it is the story the rest exists to serve.
 */
import java.util.*;

enum Kind {                                                     // OOP7: enum with behaviour
    DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT, FEE, INTEREST;
    boolean credit() { return this == DEPOSIT || this == TRANSFER_IN || this == INTEREST; }
}

final class Money {                                             // OOP1 immutable + OOP4
    final long paise;
    Money(long paise) { this.paise = paise; }
    static Money rs(long rupees) { return new Money(rupees * 100); }
    static Money of(long paise) { return new Money(paise); }
    Money plus(Money m) { return new Money(paise + m.paise); }
    Money times(double factor) { return new Money(Math.round(paise * factor)); }
    public boolean equals(Object o) { return o instanceof Money m && m.paise == paise; }
    public int hashCode() { return Long.hashCode(paise); }
    public String toString() { return (paise < 0 ? "-\u20B9" : "\u20B9") + String.format("%,.2f", Math.abs(paise) / 100.0); }
}

class Tx implements Comparable<Tx> {                            // OOP6: Comparable
    private static int seq;                                     // OOP2: shared static counter
    final int id = ++seq; final Kind kind; final Money amount; final String note;
    Tx(Kind kind, Money amount, String note) { this.kind = kind; this.amount = amount; this.note = note; }
    public int compareTo(Tx o) { return Integer.compare(id, o.id); }
    public String toString() { return "#" + id + " " + kind + " " + amount + " " + note; }
}

class History implements Cloneable {                            // OOP6: clone that owns its array
    private final String owner; private long[] points;
    History(String owner, long[] points) { this.owner = owner; this.points = points; }
    long[] points() { return points; }
    public History clone() { try { History c = (History) super.clone(); c.points = points.clone(); return c; }
        catch (CloneNotSupportedException e) { throw new AssertionError(e); } }
    public String toString() { return owner + Arrays.stream(points).mapToObj(p -> String.format("%.2f", p / 100.0)).toList(); }
}

@SuppressWarnings("serial") class WalletException extends Exception { WalletException(String m) { super(m); } }
@SuppressWarnings("serial") class Insufficient extends WalletException {      // checked hierarchy
    Insufficient(String id, Money want, Money have) { super(id + " needs " + want + ", has " + have); }
}
interface InterestBearing { double rate(); }                    // OOP5: capability interface
@FunctionalInterface interface Fee { long fee(long paise); }    // OOP6: a lambda is one of these

abstract class Account {                                        // OOP5 + OOP4
    private static int count;
    private final String id;
    protected long paise;                                       // OOP4: subclasses may touch it
    private final List<Tx> trail = new ArrayList<>();
    private boolean frozen;

    Account(String prefix, long opening) {
        id = prefix + "-" + (++count); paise = opening; if (opening > 0) record(Kind.DEPOSIT, opening, "opening");
    }
    abstract long floor();                                      // negative floor == overdraft
    abstract Money monthlyFee();
    abstract long transferFee(long paise);
    String id() { return id; }
    Money balance() { return Money.of(paise); }
    Money available() { return Money.of(paise - floor()); }
    List<Tx> recent(int n) { return trail.subList(Math.max(0, trail.size() - n), trail.size()); }
    void freeze() { frozen = true; } void unfreeze() { frozen = false; }

    final void deposit(Money m) throws WalletException { deposit(m, "deposit"); }         // OOP3: overload
    final void deposit(Money m, String note) throws WalletException {
        if (m.paise <= 0) throw new IllegalArgumentException("amount must be positive");
        alive(); paise += m.paise; record(Kind.DEPOSIT, m.paise, note);
    }
    final void withdraw(Money m) throws WalletException { alive(); pay(m.paise, "withdraw", Kind.WITHDRAW); }
    final void transferTo(Account to, Money m, String note) throws WalletException {
        alive(); to.alive();
        long fee = transferFee(m.paise);                         // OOP3: polymorphic call
        if (m.paise + fee > paise - floor()) throw new Insufficient(id, Money.of(m.paise + fee), Money.of(paise - floor()));
        paise -= m.paise; record(Kind.TRANSFER_OUT, m.paise, note + " -> " + to.id);
        if (fee > 0) { paise -= fee; record(Kind.FEE, fee, "transfer fee"); }
        to.paise += m.paise; to.record(Kind.TRANSFER_IN, m.paise, "from " + id);
    }
    final void settle() throws WalletException { Money f = monthlyFee(); if (f.paise > 0) pay(f.paise, "monthly fee", Kind.FEE); }
    final void accrue(int days) {                                // OOP4: instanceof pattern
        if (this instanceof InterestBearing b) {
            long i = Math.round(paise * b.rate() * days / 365);
            if (i > 0) { paise += i; record(Kind.INTEREST, i, "interest"); }
        }
    }
    final History history() {                                    // OOP5: final template method
        long[] pts = new long[trail.size()]; long run = 0;
        for (int i = 0; i < pts.length; i++) {
            Tx t = trail.get(i); run += t.kind.credit() ? t.amount.paise : -t.amount.paise; pts[i] = run;
        }
        return new History(id, pts);
    }
    final void alive() throws WalletException { if (frozen) throw new WalletException(id + " is frozen"); }
    private void pay(long amount, String note, Kind kind) throws WalletException {
        if (amount > paise - floor()) throw new Insufficient(id, Money.of(amount), Money.of(paise - floor()));
        paise -= amount; record(kind, amount, note);
    }
    private void record(Kind kind, long amount, String note) {
        Tx t = new Tx(kind, Money.of(amount), note); trail.add(t); Bank.get().post(t);
    }
    @Override public boolean equals(Object o) { return o instanceof Account a && id.equals(a.id); }
    @Override public int hashCode() { return id.hashCode(); }
    @Override public String toString() { return id + " " + getClass().getSimpleName() + " " + balance(); }
}

class Savings extends Account implements InterestBearing {        // OOP3: extends + capability
    Savings(long opening) { this("SAV", opening); }
    Savings(String prefix, long opening) { super(prefix, opening); }
    long floor() { return 10_000; }  Money monthlyFee() { return Money.rs(0); }
    long transferFee(long p) { return 0; }  public double rate() { return 0.035; }
}
class Premium extends Savings {                                   // OOP3: three levels + super call
    Premium(long opening) { super("PREM", opening); }
    long floor() { return 500_000; }
    Money monthlyFee() { return super.monthlyFee().plus(Money.rs(99)); }
    public double rate() { return 0.055; }
}
class Current extends Account {                                   // OOP4: overrides the floor hook
    private final long overdraft;
    private final Fee feeRule = p -> Math.max(100, p / 200);       // OOP6: lambda as interface
    Current(long opening, long overdraft) { super("CUR", opening); this.overdraft = overdraft; }
    long floor() { return -overdraft; }
    Money monthlyFee() { return Money.rs(499); }
    long transferFee(long p) { return feeRule.fee(p); }
}

final class Bank {                                                // OOP2 + OOP7
    private static final Bank INSTANCE;
    private static final List<String> BRANCHES = new ArrayList<>();
    static { BRANCHES.addAll(List.of("MUM-01", "BLR-02")); INSTANCE = new Bank(); }   // OOP2: static block
    private final Map<String, Account> byId = new LinkedHashMap<>();
    private final Vector<Tx> ledger = new Vector<>();              // OOP7: Vector
    private final Map<Kind, Integer> counts = new HashMap<>();      // OOP7: enum map keys
    private final List<Listener> listeners = new ArrayList<>();

    private Bank() { }
    static Bank get() { return INSTANCE; }
    static String branches() { return String.join(", ", BRANCHES); }
    Map<Kind, Integer> counts() { return counts; }
    List<Account> all() { return new ArrayList<>(byId.values()); }
    Account open(String kind, long opening) {                      // OOP3: factory
        Account a = switch (kind) { case "premium" -> new Premium(opening); case "current" -> new Current(opening, Money.rs(5_000).paise); default -> new Savings(opening); };
        byId.put(a.id(), a); return a;
    }
    void post(Tx t) {               // OOP1: autoboxing both ways when merging the count
        ledger.add(t); counts.merge(t.kind, 1, Integer::sum); listeners.forEach(l -> l.on(t));
    }
    List<Tx> ledger() { List<Tx> c = new ArrayList<>(ledger); c.sort(null); return c; }
    static long totalPaise(List<? extends Account> as) { return as.stream().mapToLong(a -> a.paise).sum(); }   // OOP6: wildcard
    interface Listener { void on(Tx t); }                          // OOP5: nested interface
    void subscribe(Listener l) { listeners.add(l); }
    class Teller {                                                // OOP2: inner class
        String fd(Account a, long rupees, int months) { Money m = Money.rs(rupees); return a.id() + " FD " + m + " x " + months + "m -> " + m.times(1 + 0.071 * months / 12); }
    }
    Teller teller() { return new Teller(); }
}

public class WalletApp {
    public static void main(String[] args) throws Exception {
        Bank bank = Bank.get();
        System.out.println("JavaPay [" + Bank.branches() + "]");
        Account aarav = bank.open("savings", 0), diya = bank.open("premium", 0), zoya = bank.open("current", 0);

        aarav.deposit(Money.rs(42_000), "salary");                  // OOP1: an object, not a value
        diya.deposit(Money.rs(6_000), "bonus");
        zoya.deposit(Money.rs(8_000), "invoice");
        aarav.transferTo(diya, Money.rs(18_000), "rent");
        zoya.transferTo(diya, Money.rs(12_000), "supplier");        // dips into the overdraft

        List<Account> accounts = bank.all();
        accounts.forEach(a -> System.out.println("  " + a + "  available " + a.available()));
        try { aarav.withdraw(Money.rs(30_000)); }
        catch (Insufficient e) { System.out.println("  declined " + e.getMessage()); }
        zoya.freeze();                                              // OOP6: finally always runs
        try { zoya.deposit(Money.rs(500)); }
        catch (WalletException e) { System.out.println("  declined " + e.getMessage()); }
        finally { zoya.unfreeze(); }
        accounts.forEach(a -> a.accrue(365));
        for (Account a : accounts) try { a.settle(); } catch (WalletException e) { System.out.println("  " + a.id() + " fee skipped"); }

        System.out.println("  statement " + aarav.id());
        aarav.recent(3).forEach(t -> System.out.println("   " + t));
        History history = aarav.history(), replay = history.clone();
        replay.points()[replay.points().length - 1] = 1;             // OOP6: the copy owns its array
        System.out.println("  history " + history + "\n  edited  " + replay);
        int[] seen = {0};
        bank.subscribe(t -> seen[0]++);                              // OOP6: lambda for a nested interface
        aarav.deposit(Money.rs(1_000), "top-up");
        System.out.println("  " + bank.teller().fd(diya, 10_000, 12) + "\n  listener " + seen[0]
                + ", ledger " + bank.ledger().size() + ", counts " + new TreeMap<>(bank.counts())
                + ", total " + Money.of(Bank.totalPaise(accounts)));
    }
}
