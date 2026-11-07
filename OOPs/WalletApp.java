/*
 * JavaPay - a small wallet/bank app that happens to exercise OOP1 .. OOP7.
 *
 *   javac WalletApp.java && java WalletApp
 *
 * There are no concept demos here: the program opens accounts, moves money,
 * charges fees, pays interest and prints statements. The concepts are used
 * where a real application would use them, tagged inline as  // OOP<n>: ...
 * and listed at the end from the @Concept annotations via reflection.
 *
 * Rule of the design: money is a long number of paise (double loses cents),
 * and "floor()" is the lowest balance a product allows - a negative floor is
 * an overdraft, which is the only thing Current needs to change.
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Concept {                 // OOP5: custom annotation, read back at runtime
    String value();

    int part();
}

/* OOP1 + OOP4: immutable value object. Private ctor, static factories, final
 * field, equals/hashCode/compareTo. This is the ObjectDemo lesson done right. */
@Concept(value = "immutable value object, equals/hashCode/Comparable", part = 4)
final class Money implements Comparable<Money> {

    static final Money ZERO = new Money(0L);

    private final long paise;

    private Money(long paise) {
        this.paise = paise;
    }

    static Money rs(long rupees) {
        return new Money(rupees * 100L);
    }

    static Money paise(long paise) {
        return new Money(paise);
    }

    long paise() {
        return paise;
    }

    boolean isZero() {
        return paise == 0L;
    }

    Money plus(Money other) {
        return new Money(paise + other.paise);
    }

    Money times(double factor) {
        return new Money(Math.round(paise * factor));
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(paise, other.paise);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Money && ((Money) obj).paise == paise;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(paise);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s\u20B9%,.2f", paise < 0 ? "-" : "", Math.abs(paise) / 100.0);
    }
}

/* OOP7: enum with a field and a constructor. */
@Concept(value = "enum with a field, a constructor and a label", part = 7)
enum Kind {

    DEPOSIT(true), WITHDRAWAL(false), TRANSFER_IN(true), TRANSFER_OUT(false), FEE(false), INTEREST(true);

    private final boolean credit;

    Kind(boolean credit) {
        this.credit = credit;
    }

    boolean credit() {
        return credit;
    }

    String label() {
        return name().toLowerCase(Locale.US).replace('_', ' ');
    }
}

/* OOP1/OOP6: Comparable + copy constructor + static counter. The copy ctor is
 * used when a receipt is duplicated; static counter feeds the receipt number. */
@Concept(value = "Comparable + copy constructor + static counter", part = 6)
class Transaction implements Comparable<Transaction> {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.US).withZone(ZoneOffset.UTC);
    private static int sequence;
    private static long clock = Instant.parse("2026-01-01T09:00:00Z").toEpochMilli();

    final int id;
    final Kind kind;
    final Money amount;
    final String account;
    final String note;
    final long at;

    Transaction(Kind kind, Money amount, String account, String note) {
        this.id = ++sequence;
        this.kind = kind;
        this.amount = amount;
        this.account = account;
        this.note = note;
        this.at = clock;
        clock += 60_000L;                       // one business event per minute
    }

    Transaction(Transaction other) {            // OOP1: copy constructor
        this.id = other.id;
        this.kind = other.kind;
        this.amount = other.amount;
        this.account = other.account;
        this.note = other.note;
        this.at = other.at;
    }

    @Override
    public int compareTo(Transaction other) {   // oldest first; ties broken by id
        int byTime = Long.compare(at, other.at);
        return byTime != 0 ? byTime : Integer.compare(id, other.id);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "#%03d %s  %-12s %10s  %s",
                id, STAMP.format(Instant.ofEpochMilli(at)), kind.label(), amount, note);
    }
}

/* OOP6 + OOP4: Cloneable with a mutable array inside, so clone() must copy the
 * array as well - the CloneHuman fix. A shallow copy would alias it. */
@Concept(value = "Cloneable + deep copy of a mutable array", part = 6)
class Statement implements Cloneable {

    private final String owner;
    private long[] points;                      // running balance after each event

    Statement(String owner, long[] points) {
        this.owner = owner;
        this.points = points;
    }

    long[] points() {
        return points;
    }

    @Override
    public Statement clone() {
        try {
            Statement copy = (Statement) super.clone();   // shallow first
            copy.points = points.clone();                 // then deep-copy the array
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder(owner).append(" [");
        for (int i = 0; i < points.length; i++) {
            out.append(i == 0 ? "" : ", ").append(Money.paise(points[i]));
        }
        return out.append(']').toString();
    }
}

/* OOP6: checked exceptions carry data, not just a message. */
@SuppressWarnings("serial")
@Concept(value = "checked exception hierarchy with a payload", part = 6)
class WalletException extends Exception {
    WalletException(String message) {
        super(message);
    }
}

@SuppressWarnings("serial")
class InsufficientFunds extends WalletException {

    private final long shortfall;

    InsufficientFunds(String account, long wanted, long available) {
        super(String.format(Locale.US, "%s needs %s, available %s",
                account, Money.paise(wanted), Money.paise(available)));
        this.shortfall = wanted - available;
    }

    long shortfall() {
        return shortfall;
    }
}

@SuppressWarnings("serial")
class AccountFrozen extends WalletException {
    AccountFrozen(String account) {
        super(account + " is frozen");
    }
}

/* OOP5: interface with a default method, plus a capability interface. */
@Concept(value = "interface with a default method", part = 5)
interface Auditable {

    List<Transaction> trail();

    default List<Transaction> recent(int count) {          // free method for every account
        List<Transaction> all = trail();
        return new ArrayList<>(all.subList(Math.max(0, all.size() - count), all.size()));
    }
}

@Concept(value = "capability interface: default maths + interface static method", part = 5)
interface InterestBearing {

    double rate();

    default Money interest(Money principal, int days) {
        return principal.times(rate() * days / 365.0);
    }

    static InterestBearing none() {                        // interface static method
        return () -> 0.0;
    }
}

@Concept(value = "functional interface: a lambda is the implementation", part = 6)
@FunctionalInterface
interface FeeRule {                                        // OOP6: a lambda is this type
    long fee(long amountPaise);
}

/* OOP5 + OOP3 + OOP4: the abstract parent owns the money rules. Subclasses only
 * answer three questions (floor, monthly fee, transfer fee). Every state change
 * is a final method, so no product can break the invariants. */
@Concept(value = "abstract class: protected state + final template methods", part = 5)
abstract class Account implements Auditable {

    private static int count;                       // OOP2: static, shared by all accounts

    private final String id;                        // OOP1: final, set once
    private final String product;
    private final String holder;
    protected long paise;                           // OOP4: subclasses may touch this
    private final List<Transaction> trail = new ArrayList<>();
    private boolean frozen;

    Account(String prefix, String product, String holder, long openingPaise) {
        this.id = prefix + "-" + (1001 + count++);
        this.product = product;
        this.holder = holder;
        this.paise = openingPaise;
        if (openingPaise > 0L) {
            record(Kind.DEPOSIT, openingPaise, "opening balance");
        }
    }

    /* the three product questions */
    abstract long floor();                          // negative floor == overdraft

    abstract Money monthlyFee();

    abstract long transferFee(long amountPaise);

    String id()             { return id; }

    String product()        { return product; }

    String holder()         { return holder; }

    Money balance()         { return Money.paise(paise); }

    boolean isFrozen()      { return frozen; }

    Money available()       { return Money.paise(paise - floor()); }

    @Override
    public List<Transaction> trail() {
        return Collections.unmodifiableList(trail);
    }

    /* OOP3: overloading - the note-less deposit is a convenience overload. */
    final void deposit(Money amount) throws WalletException {
        deposit(amount, "deposit");
    }

    final void deposit(Money amount, String note) throws WalletException {
        if (amount.paise() <= 0L) {
            throw new IllegalArgumentException("amount must be positive");
        }
        ensureLive();
        paise += amount.paise();
        record(Kind.DEPOSIT, amount.paise(), note);
    }

    final void withdraw(Money amount) throws WalletException {
        ensureLive();
        pay(amount.paise(), "withdrawal", Kind.WITHDRAWAL);
    }

    final void transferTo(Account target, Money amount, String note) throws WalletException {
        ensureLive();
        target.ensureLive();

        long fee = transferFee(amount.paise());                 // OOP3: polymorphic
        long headroom = paise - floor();
        if (amount.paise() + fee > headroom) {
            throw new InsufficientFunds(id, amount.paise() + fee, Math.max(0L, headroom));
        }

        paise -= amount.paise();
        record(Kind.TRANSFER_OUT, amount.paise(), note + " -> " + target.id());

        if (fee > 0L) {
            paise -= fee;
            record(Kind.FEE, fee, "transfer fee");
        }
        target.receive(amount.paise(), "from " + id);
    }

    final void settleMonthly() throws WalletException {
        Money fee = monthlyFee();
        if (!fee.isZero()) {
            pay(fee.paise(), "monthly fee", Kind.FEE);
        }
    }

    /* OOP4: instanceof + cast, because only some products carry this capability. */
    final void accrue(int days) {
        if (this instanceof InterestBearing) {
            Money interest = ((InterestBearing) this).interest(balance(), days);
            if (!interest.isZero()) {
                paise += interest.paise();
                record(Kind.INTEREST, interest.paise(), "interest " + days + "d");
            }
        }
    }

    /* OOP5: final template method - every product reports balance history the
     * same way, and gets a cloneable Statement out of it. */
    final Statement statement() {
        long[] running = new long[trail.size()];
        long total = 0L;
        for (int i = 0; i < trail.size(); i++) {
            Transaction transaction = trail.get(i);
            total += transaction.kind.credit()
                    ? transaction.amount.paise()
                    : -transaction.amount.paise();
            running[i] = total;
        }
        return new Statement(id + " " + holder, running);
    }

    final String summary() {
        return String.format(Locale.US, "%-9s %-16s %-16s %10s",
                id, product, holder, balance());
    }

    void freeze()   { frozen = true; }

    void unfreeze() { frozen = false; }

    void ensureLive() throws AccountFrozen {
        if (frozen) {
            throw new AccountFrozen(id);
        }
    }

    private void receive(long amount, String note) {
        paise += amount;
        record(Kind.TRANSFER_IN, amount, note);
    }

    private void pay(long amount, String note, Kind kind) throws WalletException {
        long headroom = paise - floor();
        if (amount > headroom) {
            throw new InsufficientFunds(id, amount, Math.max(0L, headroom));
        }
        paise -= amount;
        record(kind, amount, note);
    }

    private void record(Kind kind, long amount, String note) {
        Transaction transaction = new Transaction(kind, Money.paise(amount), id, note);
        trail.add(transaction);                 // account's own copy for statements
        Bank.get().post(transaction);           // bank's ledger for reports
    }

    /* OOP4: equals by business identity, hashCode alongside it. */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Account && id.equals(((Account) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}

/* OOP3: inheritance in three levels - Savings -> Account, Premium -> Savings. */
@Concept(value = "inheritance: extends the abstract class, adds a capability", part = 3)
class Savings extends Account implements InterestBearing {

    Savings(String holder, long openingPaise) {
        this("SAV", "Savings", holder, openingPaise);       // this(...) chaining
    }

    Savings(String prefix, String product, String holder, long openingPaise) {
        super(prefix, product, holder, openingPaise);       // super(...) chaining
    }

    @Override
    long floor() {
        return 10_000L;                                     // INR 100 must stay
    }

    @Override
    Money monthlyFee() {
        return Money.ZERO;
    }

    @Override
    long transferFee(long amountPaise) {
        return 0L;                                          // UPI is free
    }

    @Override
    public double rate() {
        return 0.035;
    }
}

@Concept(value = "three-level inheritance + super call inside an override", part = 3)
class Premium extends Savings {

    Premium(String holder, long openingPaise) {
        super("PREM", "Premium Savings", holder, openingPaise);
    }

    @Override
    long floor() {
        return 500_000L;                                    // INR 5,000
    }

    @Override
    Money monthlyFee() {
        return super.monthlyFee().plus(Money.rs(99));       // free + concierge fee
    }

    @Override
    public double rate() {
        return 0.055;
    }
}

/* OOP4: Current overrides the floor to a negative number and nothing else in
 * Account changes. Its fee rule is a lambda, i.e. a FeeRule implementation. */
@Concept(value = "override of the floor hook (overdraft) + lambda fee rule", part = 4)
class Current extends Account {

    private final long overdraft;
    private final FeeRule feeRule = amountPaise -> Math.max(100L, amountPaise / 200L);

    Current(String holder, long openingPaise, long overdraftPaise) {
        super("CUR", "Current", holder, openingPaise);
        this.overdraft = overdraftPaise;
    }

    @Override
    long floor() {
        return -overdraft;
    }

    @Override
    Money monthlyFee() {
        return Money.rs(499);
    }

    @Override
    long transferFee(long amountPaise) {
        return feeRule.fee(amountPaise);                    // INR 1 minimum, else 0.5%
    }
}

/* OOP6: generics with a bounded type parameter + a hand-written growable array
 * (CustomGenArrayList). The Iterator below is an anonymous class - also OOP2. */
@Concept(value = "generics + bounded type + hand-written collection", part = 6)
class Repository<T extends Account> implements Iterable<T> {

    private Object[] data = new Object[4];
    private int size;

    void add(T item) {
        if (size == data.length) {
            data = Arrays.copyOf(data, size * 2);
        }
        data[size++] = item;
    }

    @SuppressWarnings("unchecked")
    T get(int index) {
        return (T) data[index];
    }

    int size() {
        return size;
    }

    List<T> toList() {
        List<T> copy = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            copy.add(get(i));
        }
        return copy;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {                          // anonymous class
            private int cursor;

            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            @Override
            public T next() {
                return get(cursor++);
            }
        };
    }
}

/* OOP2 + OOP7: the singleton that owns everything. Static block runs before
 * INSTANCE is created, so the constructor can trust BRANCHES. */
@Concept(value = "singleton, static block, nested/inner/local classes, collections", part = 2)
final class Bank {

    private static final List<Branch> BRANCHES;
    private static final Bank INSTANCE;

    static {
        BRANCHES = Collections.unmodifiableList(Arrays.asList(
                new Branch("MUM-01", "Mumbai"), new Branch("BLR-02", "Bengaluru")));
        INSTANCE = new Bank();
    }

    private final Repository<Account> accounts = new Repository<>();
    private final Map<String, Account> byId = new LinkedHashMap<>();   // stable order
    private final List<Transaction> ledger = new ArrayList<>();
    private final Vector<Transaction> legacyMirror = new Vector<>();   // OOP7: Vector
    private final Map<Kind, Integer> counts = new HashMap<>();         // OOP7: enum keys
    private final List<Listener> listeners = new ArrayList<>();

    private Bank() {                                                   // OOP2: singleton
    }

    static Bank get() {
        return INSTANCE;
    }

    List<Branch> branches() {
        return BRANCHES;
    }

    /* OOP3: overloading the factory instead of adding a second method name. */
    Account open(String kind, String holder, long rupees) {
        return open(kind, holder, rupees, 0L);
    }

    Account open(String kind, String holder, long rupees, long overdraftRupees) {
        Account account;
        if ("savings".equals(kind)) {
            account = new Savings(holder, rupees * 100L);
        } else if ("premium".equals(kind)) {
            account = new Premium(holder, rupees * 100L);
        } else if ("current".equals(kind)) {
            account = new Current(holder, rupees * 100L, overdraftRupees * 100L);
        } else {
            throw new IllegalArgumentException("unknown product: " + kind);
        }
        accounts.add(account);
        byId.put(account.id(), account);
        return account;
    }

    List<Account> all() {
        return Collections.unmodifiableList(accounts.toList());
    }

    Account require(String id) throws WalletException {
        Account account = byId.get(id);
        if (account == null) {
            throw new WalletException("no account " + id);
        }
        return account;
    }

    void post(Transaction transaction) {
        ledger.add(transaction);
        legacyMirror.add(transaction);                       // kept in step for old reports
        counts.merge(transaction.kind, 1, Integer::sum);     // OOP1: autoboxing both ways
        for (Listener listener : listeners) {
            listener.onTransaction(transaction);
        }
    }

    List<Transaction> ledger() {
        List<Transaction> copy = new ArrayList<>(ledger);
        Collections.sort(copy);                              // Transaction.compareTo
        return copy;
    }

    Vector<Transaction> legacyMirror() {
        return legacyMirror;
    }

    Map<Kind, Integer> counts() {
        return new HashMap<>(counts);
    }

    void freeze(String id) throws WalletException {
        require(id).freeze();
    }

    void unfreeze(String id) throws WalletException {
        require(id).unfreeze();
    }

    List<String> feeRun() {        List<String> log = new ArrayList<>();
        for (Account account : accounts) {
            Money fee = account.monthlyFee();
            if (fee.isZero()) {
                log.add(String.format(Locale.US, "%-9s no monthly fee", account.id()));
                continue;
            }
            try {
                account.settleMonthly();
                log.add(String.format(Locale.US, "%-9s charged %s", account.id(), fee));
            } catch (InsufficientFunds e) {
                log.add(String.format(Locale.US, "%-9s fee skipped: %s", account.id(), e.getMessage()));
            } catch (WalletException e) {
                log.add(String.format(Locale.US, "%-9s fee failed: %s", account.id(), e.getMessage()));
            }
        }
        return log;
    }

    /* OOP6: a wildcard read - any list of accounts, whatever the concrete type. */
    static long totalPaise(List<? extends Account> accounts) {
        long total = 0L;
        for (Account account : accounts) {
            total += account.balance().paise();
        }
        return total;
    }

    /* OOP5: nested interface. Anonymous class and lambda both implement it. */
    interface Listener {
        void onTransaction(Transaction transaction);
    }

    void subscribe(Listener listener) {
        listeners.add(listener);
    }

    /* OOP2: inner class - it reaches the outer Bank's private state directly. */
    class Teller {

        private final String desk;

        Teller(String desk) {
            this.desk = desk;
        }

        String fixedDepositQuote(Account account, long rupees, int months) throws WalletException {
            account.ensureLive();
            Money amount = Money.rs(rupees);
            if (account.available().compareTo(amount) < 0) {
                throw new InsufficientFunds(account.id(), amount.paise(), account.available().paise());
            }
            double rate = 0.071 * months / 12.0;
            return String.format(Locale.US, "%s at %s: %s FD for %d months -> maturity %s",
                    account.id(), desk, amount, months, amount.plus(amount.times(rate)));
        }
    }

    Teller teller(String desk) {
        return new Teller(desk);
    }

    /* OOP6: try-with-resources instead of finalize() for deterministic cleanup. */
    class Audit implements AutoCloseable {

        private final String label;

        Audit(String label) {
            this.label = label;
            System.out.println("  [audit] " + label + " opened");
        }

        String label() {
            return label;
        }

        @Override
        public void close() {
            System.out.println("  [audit] " + label + " closed");
        }
    }

    Audit audit(String label) {
        return new Audit(label);
    }

    /* OOP2: a local class, visible only inside this method. */
    List<String> closeDay() {
        class Totals {
            private final String product;
            private int accounts;
            private long paise;

            Totals(String product) {
                this.product = product;
            }

            void add(Account account) {
                accounts++;
                paise += account.balance().paise();
            }

            String line() {
                return String.format(Locale.US, "   %-16s %d account%s   %s",
                        product, accounts, accounts == 1 ? "" : "s", Money.paise(paise));
            }
        }

        Map<String, Totals> byProduct = new LinkedHashMap<>();
        for (Account account : accounts) {
            Totals totals = byProduct.get(account.product());
            if (totals == null) {
                totals = new Totals(account.product());
                byProduct.put(account.product(), totals);
            }
            totals.add(account);
        }

        List<String> lines = new ArrayList<>();
        for (Totals totals : byProduct.values()) {
            lines.add(totals.line());
        }
        return lines;
    }

    /* OOP2: static nested class - usable without a Bank instance. */
    static class Branch {

        private final String code;
        private final String city;

        Branch(String code, String city) {
            this.code = code;
            this.city = city;
        }

        @Override
        public String toString() {
            return code + " " + city;
        }
    }
}

/* OOP1: the entry point is just an application. Read it top to bottom and you
 * see one business day, not a list of demos. Concept tags mark the machinery. */
@Concept(value = "entry point that runs one business day end to end", part = 1)
public class WalletApp {

    public static void main(String[] args) throws Exception {
        Bank bank = Bank.get();                          // OOP2: singleton

        Account aarav = bank.open("savings", "Aarav Sharma", 0L);
        Account diya = bank.open("premium", "Diya Patel", 0L);
        Account zoya = bank.open("current", "Zoya Traders", 0L, 5_000L);

        System.out.println("JavaPay Wallet - " + bank.branches());
        for (Account account : bank.all()) {
            System.out.println("  opened  " + account.summary());
        }

        /* ---- the day's money ---- */
        aarav.deposit(Money.rs(42_000), "salary");
        diya.deposit(Money.rs(6_000), "bonus");
        zoya.deposit(Money.rs(8_000), "invoice #4471");

        aarav.transferTo(diya, Money.rs(18_000), "rent");
        zoya.transferTo(diya, Money.rs(12_000), "supplier bill");   // takes zoya negative

        for (Account account : bank.all()) {
            System.out.println("  balance " + account.summary());
        }

        /* ---- things the bank must refuse ---- */
        try {
            aarav.withdraw(Money.rs(30_000));
        } catch (InsufficientFunds e) {
            System.out.println("  declined: " + e.getMessage()
                    + "  (short " + Money.paise(e.shortfall()) + ")");
        }

        bank.freeze(zoya.id());
        try {
            zoya.deposit(Money.rs(500));
        } catch (AccountFrozen e) {
            System.out.println("  declined: " + e.getMessage());
        } finally {
            bank.unfreeze(zoya.id());                    // OOP6: finally always runs
        }

        /* ---- month end ---- */
        for (Account account : bank.all()) {
            account.accrue(365);                         // interest, only where the capability exists
        }
        for (String line : bank.feeRun()) {
            System.out.println("  fee     " + line);
        }

        System.out.println();
        System.out.println("  statement " + aarav.id() + " (" + aarav.product() + ")");
        for (Transaction transaction : aarav.recent(3)) {   // OOP5: interface default method
            System.out.println("   " + transaction);
        }
        System.out.println("   balance " + aarav.balance() + "   available " + aarav.available());

        /* ---- cloning a statement: the copy owns its array ---- */
        Statement snapshot = aarav.statement();
        Statement replay = snapshot.clone();                // OOP6: deep clone
        replay.points()[replay.points().length - 1] = 1L;   // scribble on the copy
        System.out.println("  history  " + snapshot);
        System.out.println("  edited   " + replay);
        System.out.println("  original still " + snapshot + "  (arrays are distinct)");

        /* ---- back office ---- */
        System.out.println("  " + bank.teller("Desk-3").fixedDepositQuote(diya, 10_000, 12));

        int[] seen = {0};
        bank.subscribe(transaction -> seen[0]++);           // OOP5 + OOP6: lambda for a nested interface
        aarav.deposit(Money.rs(1_000), "top-up");
        System.out.println("  listener counted " + seen[0] + " transaction(s)");

        try (Bank.Audit audit = bank.audit("day-close")) {  // OOP6: try-with-resources
            System.out.println("  " + audit.label() + ":");
            for (String line : bank.closeDay()) {
                System.out.println(line);                   // OOP2: local class inside closeDay()
            }
        }

        System.out.println("  ledger " + bank.ledger().size() + " entries, legacy mirror "
                + bank.legacyMirror().size() + ", counts " + bank.counts());
        System.out.println("  money in bank " + Money.paise(Bank.totalPaise(bank.all())));
        System.out.println("  latest: " + bank.ledger().get(bank.ledger().size() - 1));

        conceptIndex();                                     // OOP4 + OOP5: reflection over @Concept
    }

    /* OOP4: the Class API + a runtime-retained annotation produce the map of
     * "where is this concept used" without a single demo banner in the output. */
    private static void conceptIndex() {
        System.out.println();
        System.out.println("  concept index (read from @Concept annotations)");

        Class<?>[] types = {
                WalletApp.class, Money.class, Transaction.class, Statement.class,
                WalletException.class, Auditable.class, InterestBearing.class, FeeRule.class,
                Account.class, Savings.class, Premium.class, Current.class,
                Repository.class, Kind.class, Bank.class
        };

        List<Class<?>> ordered = new ArrayList<>(Arrays.asList(types));
        ordered.sort(Comparator.comparingInt(type -> type.getAnnotation(Concept.class).part()));

        for (Class<?> type : ordered) {
            Concept concept = type.getAnnotation(Concept.class);
            System.out.printf(Locale.US, "   OOP%d  %-10s %-16s %s%n",
                    concept.part(),
                    type.isInterface() ? "interface" : "class",
                    type.getSimpleName(),
                    concept.value());
        }
    }
}



