# Java OOP — Complete Study Guide

A topic-by-topic guide to the **Object Oriented Programming in Java** playlist. Every major concept has a short explanation, a focused code example, and the output that code produces.

The complete runnable programs are in `OOPs/Learnings/`, one file per video. The capstone application,
`OOPs/WalletApp.java`, puts all seven parts into one running program.

```bash
cd OOPs/Learnings
javac OOP1.java
java OOP1

cd ..                    # the capstone app
javac WalletApp.java
java WalletApp
```

---

## Contents

| Part | Video | Topics |
|---|---|---|
| 1 | OOP 1 | Classes, objects, references, constructors, `this`, `final`, wrappers, garbage collection |
| 2 | OOP 2 | Packages, imports, `static`, static blocks, inner classes, singleton |
| 3 | OOP 3 | Inheritance, polymorphism, encapsulation, abstraction |
| 4 | OOP 4 | Access modifiers, built-in packages, `Object` class |
| 5 | OOP 5 | Abstract classes, interfaces, annotations |
| 6 | OOP 6 | Generics, custom ArrayList, lambdas, exceptions, cloning |
| 7 | OOP 7 | Collections, `Vector`, enums |

---

# Part 1 — OOP 1: Foundations

## 1. Class vs Object

A **class** is a blueprint. An **object** is a real instance created from that blueprint. The class defines the data an object stores and the behavior it can perform.

```java
class Student {
    String name;
    int rollNo;

    void introduce() {
        System.out.println("I am " + name + ", roll " + rollNo);
    }
}

public class Main {
    public static void main(String[] args) {
        Student s = new Student();
        s.name = "Kunal";
        s.rollNo = 13;
        s.introduce();
    }
}
```

Output:

```text
I am Kunal, roll 13
```

`Student` is the class. `s` refers to one `Student` object.

---

## 2. References and `new`

A variable of a class type is a **reference**, not the object itself. The object lives in heap memory; the reference points to it.

```java
Student a = new Student();
a.name = "Kunal";

Student b = a;
b.name = "Rahul";

System.out.println(a.name);
System.out.println(a == b);
```

Output:

```text
Rahul
true
```

`new` allocates memory at runtime and returns a reference. Assigning `b = a` copies the reference, so both variables point to the same object.

---

## 3. Constructors

A constructor is a special method that runs when an object is created. It has the same name as the class and no return type.

```java
class Student {
    String name;
    int rollNo;

    Student() {
        this("Unknown", 0);
    }

    Student(String name, int rollNo) {
        this.name = name;
        this.rollNo = rollNo;
    }

    Student(Student other) {
        this.name = other.name;
        this.rollNo = other.rollNo;
    }
}

public class Main {
    public static void main(String[] args) {
        Student a = new Student();
        Student b = new Student("Kunal", 13);
        Student c = new Student(b);

        System.out.println(a.name);
        System.out.println(b.name);
        System.out.println(c.name);
    }
}
```

Output:

```text
Unknown
Kunal
Kunal
```

This shows four ideas:

- **Default constructor:** `Student()`
- **Parameterized constructor:** `Student(String, int)`
- **Copy constructor:** `Student(Student)`
- **Constructor overloading:** multiple constructors with different parameters

---

## 4. `this` and `final`

`this` refers to the current object. It is used to distinguish an instance variable from a parameter with the same name, and `this(...)` calls another constructor.

```java
class Student {
    final int PASS_MARKS = 40;
    String name;

    Student(String name) {
        this.name = name;
    }

    void rename(String name) {
        this.name = name;
    }
}

public class Main {
    public static void main(String[] args) {
        Student s = new Student("Kunal");
        s.rename("Rahul");

        System.out.println(s.name);

        final Student fixed = new Student("Arpit");
        fixed.name = "Arpit Kumar";
        System.out.println(fixed.name);
    }
}
```

Output:

```text
Rahul
Arpit Kumar
```

`final` on a primitive makes it a constant. `final` on a reference prevents reassignment, but the object's fields can still change:

```java
final Student s = new Student("Kunal");
s.name = "Rahul";              // allowed
// s = new Student("Other");   // not allowed
```

---

## 5. Wrapper Classes

Wrapper classes turn primitives into objects. They are useful with generics and collections, which work with objects rather than primitives.

```java
public class Main {
    public static void main(String[] args) {
        int a = 10;

        Integer b = a;      // autoboxing: int -> Integer
        int c = b;          // unboxing: Integer -> int

        System.out.println(b.intValue());
        System.out.println(c);
        System.out.println(Integer.parseInt("42"));
        System.out.println(Integer.MAX_VALUE);
    }
}
```

Output:

```text
10
10
42
2147483647
```

Common wrapper classes include `Integer`, `Double`, `Character`, `Boolean`, and `Long`.

---

## 6. Garbage Collection

Java reclaims memory automatically when an object can no longer be reached. You cannot free an object manually.

```java
class Demo {
    @Override
    protected void finalize() {
        System.out.println("Object is being collected");
    }
}

public class Main {
    public static void main(String[] args) {
        Demo d = new Demo();
        d = null;

        System.gc();
    }
}
```

Output on JDK 25: nothing at all. The finalizer thread may not keep up before the JVM exits, and `finalize()` is never guaranteed to run.

Key points:

- `System.gc()` is only a request; the JVM decides when to collect.
- `finalize()` is deprecated and is not guaranteed to run.
- For files, streams, and connections, use `try-with-resources` or an explicit `close()` instead of relying on garbage collection.

---

# Part 2 — OOP 2: Packages, Static, Singleton

## 7. Packages and Imports

A package groups related classes and prevents name collisions. The package name must match the directory structure.

```java
// File: com/example/util/Message.java
package com.example.util;

public class Message {
    public static void message() {
        System.out.println("Hello from util");
    }
}
```

```java
// File: com/example/app/Main.java
package com.example.app;

import static com.example.util.Message.message;

public class Main {
    public static void main(String[] args) {
        message();
    }
}
```

Output:

```text
Hello from util
```

- `package` declares where the class belongs.
- `import` brings a class into scope.
- `import static` brings a static member into scope.

---

## 8. Static Variables and Methods

`static` members belong to the class, not to individual objects. A static variable is shared by every instance.

```java
class Human {
    static long population;
    String name;

    Human(String name) {
        this.name = name;
        population++;
    }

    static void showPopulation() {
        System.out.println("Population: " + population);
    }
}

public class Main {
    public static void main(String[] args) {
        new Human("Kunal");
        new Human("Rahul");
        new Human("Arpit");

        Human.showPopulation();
    }
}
```

Output:

```text
Population: 3
```

A static method cannot use `this` and cannot directly access instance fields. `main` is static because it must run before any object exists.

---

## 9. Static Blocks

A static block runs once, when the class is loaded. It is used for static initialization that needs more than a single expression.

```java
class Config {
    static int a = 4;
    static int b;

    static {
        System.out.println("Static block ran");
        b = a * 5;
    }

    public static void main(String[] args) {
        System.out.println(a + " " + b);
    }
}
```

Output:

```text
Static block ran
4 20
```

The block runs only once, even if many objects of the class are created.

---

## 10. Inner Classes

A class can be declared inside another class. A **non-static inner class** has access to the outer object. A **static nested class** does not need an outer object.

```java
class Outer {
    int x = 10;

    class Inner {
        void show() {
            System.out.println(x);
        }
    }

    static class Nested {
        void show() {
            System.out.println("Nested");
        }
    }
}

public class Main {
    public static void main(String[] args) {
        Outer outer = new Outer();
        Outer.Inner inner = outer.new Inner();
        inner.show();

        Outer.Nested nested = new Outer.Nested();
        nested.show();
    }
}
```

Output:

```text
10
Nested
```

Only nested classes can be `static`. A top-level class cannot be static.

---

## 11. Singleton Class

A singleton ensures that only one object of a class exists. This is done with a private constructor and a static accessor.

```java
class Singleton {
    private static Singleton instance;

    private Singleton() {
    }

    public static Singleton getInstance() {
        if (instance == null) {
            instance = new Singleton();
        }
        return instance;
    }
}

public class Main {
    public static void main(String[] args) {
        Singleton a = Singleton.getInstance();
        Singleton b = Singleton.getInstance();

        System.out.println(a == b);
    }
}
```

Output:

```text
true
```

Both references point to the same object.

---

# Part 3 — OOP 3: The Four Pillars

## 12. Inheritance

Inheritance lets one class reuse the fields and methods of another class. The child class extends the parent class.

```java
class Box {
    double l, w, h;

    Box(double l, double w, double h) {
        this.l = l;
        this.w = w;
        this.h = h;
    }

    void show() {
        System.out.println(l + " " + w + " " + h);
    }
}

class BoxWeight extends Box {
    double weight;

    BoxWeight(double l, double w, double h, double weight) {
        super(l, w, h);
        this.weight = weight;
    }
}

public class Main {
    public static void main(String[] args) {
        BoxWeight b = new BoxWeight(2, 3, 4, 8);
        b.show();
        System.out.println(b.weight);
    }
}
```

Output:

```text
2.0 3.0 4.0
8.0
```

`super(...)` calls the parent constructor and must be the first statement in the child constructor. Java supports single inheritance for classes.

---

## 13. Polymorphism

Polymorphism means the same method call can behave differently depending on the object. Method **overriding** is resolved at runtime.

```java
class Shape {
    void area() {
        System.out.println("Shape area");
    }
}

class Circle extends Shape {
    @Override
    void area() {
        System.out.println("Area is pi * r * r");
    }
}

class Square extends Shape {
    @Override
    void area() {
        System.out.println("Area is side * side");
    }
}

public class Main {
    public static void main(String[] args) {
        Shape s1 = new Circle();
        Shape s2 = new Square();

        s1.area();
        s2.area();
    }
}
```

Output:

```text
Area is pi * r * r
Area is side * side
```

Method **overloading** is compile-time polymorphism:

```java
int add(int a, int b) {
    return a + b;
}

double add(double a, double b) {
    return a + b;
}
```

---

## 14. Encapsulation

Encapsulation protects an object's data by making fields private and exposing controlled methods.

```java
class BankAccount {
    private double balance;

    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
        }
    }

    public void withdraw(double amount) {
        if (amount > 0 && amount <= balance) {
            balance -= amount;
        }
    }

    public double getBalance() {
        return balance;
    }
}

public class Main {
    public static void main(String[] args) {
        BankAccount account = new BankAccount();
        account.deposit(500);
        account.withdraw(200);

        System.out.println(account.getBalance());
    }
}
```

Output:

```text
300.0
```

The field `balance` cannot be changed directly from outside the class. All changes go through methods that can validate the input.

---

## 15. Abstraction

Abstraction hides implementation details and exposes only the essential behavior. Abstract classes and interfaces are the main tools for this.

```java
abstract class Vehicle {
    abstract void start();

    void stop() {
        System.out.println("Vehicle stopped");
    }
}

class Car extends Vehicle {
    @Override
    void start() {
        System.out.println("Car starts with a key");
    }
}

public class Main {
    public static void main(String[] args) {
        Vehicle v = new Car();
        v.start();
        v.stop();
    }
}
```

Output:

```text
Car starts with a key
Vehicle stopped
```

`Vehicle` cannot be instantiated directly. The subclass must provide the missing behavior.

---

# Part 4 — OOP 4: Access Control and Object Class

## 16. Access Modifiers

Access modifiers control who can see a class, field, method, or constructor.

| Modifier | Same class | Same package | Subclass in another package | Anywhere |
|---|---|---|---|---|
| `private` | Yes | No | No | No |
| default (no modifier) | Yes | Yes | No | No |
| `protected` | Yes | Yes | Yes, through inheritance | No |
| `public` | Yes | Yes | Yes | Yes |

```java
class Demo {
    private int a = 1;
    int b = 2;
    protected int c = 3;
    public int d = 4;

    private void privateMethod() {
    }

    void defaultMethod() {
    }

    protected void protectedMethod() {
    }

    public void publicMethod() {
    }
}
```

A common pattern is to keep fields `private` and provide `public` getters and setters.

---

## 17. Built-in Packages

Java ships with many ready-made packages.

| Package | Common contents |
|---|---|
| `java.lang` | `String`, `Object`, `Integer`, `Math`, `System` |
| `java.util` | `ArrayList`, `HashMap`, `Collections`, `Scanner` |
| `java.io` | Files, streams, readers, writers |
| `java.time` | `LocalDate`, `LocalTime`, `Duration` |

```java
import java.util.ArrayList;
import java.util.Collections;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(3);
        list.add(1);
        list.add(2);

        Collections.sort(list);

        System.out.println(list);
        System.out.println(LocalDate.now());
    }
}
```

Output:

```text
[1, 2, 3]
2026-11-06
```

`java.lang` is imported automatically. Other packages must be imported.

---

## 18. Object Class Methods

Every class in Java indirectly extends `Object`. Important methods include `toString()`, `equals()`, `hashCode()`, `getClass()`, and `clone()`.

```java
import java.util.Objects;

class Point {
    int x, y;

    Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "Point(" + x + ", " + y + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Point)) {
            return false;
        }
        Point p = (Point) o;
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}

public class Main {
    public static void main(String[] args) {
        Point a = new Point(1, 2);
        Point b = new Point(1, 2);

        System.out.println(a);
        System.out.println(a == b);
        System.out.println(a.equals(b));
    }
}
```

Output:

```text
Point(1, 2)
false
true
```

`==` compares references. `equals()` can compare contents. If you override `equals()`, you should also override `hashCode()`.

---

# Part 5 — OOP 5: Abstract Classes, Interfaces, Annotations

## 19. Abstract Classes

An abstract class can contain abstract methods, concrete methods, fields, and constructors. It cannot be instantiated.

```java
abstract class Animal {
    String name;

    Animal(String name) {
        this.name = name;
    }

    abstract void sound();

    void sleep() {
        System.out.println(name + " is sleeping");
    }
}

class Dog extends Animal {
    Dog(String name) {
        super(name);
    }

    @Override
    void sound() {
        System.out.println("Woof");
    }
}

public class Main {
    public static void main(String[] args) {
        Animal a = new Dog("Tommy");
        a.sound();
        a.sleep();
    }
}
```

Output:

```text
Woof
Tommy is sleeping
```

Use an abstract class when related classes share state or behavior but must implement some parts themselves.

---

## 20. Interfaces

An interface is a contract. A class that implements an interface must provide the required methods.

```java
interface Engine {
    int PRICE = 78000;

    void start();

    void stop();

    default void info() {
        System.out.println("Engine info");
    }

    static void rules() {
        System.out.println("Follow the rules");
    }
}

interface Brake {
    void brake();
}

class Car implements Engine, Brake {
    @Override
    public void start() {
        System.out.println("Car starts");
    }

    @Override
    public void stop() {
        System.out.println("Car stops");
    }

    @Override
    public void brake() {
        System.out.println("Car brakes");
    }
}

public class Main {
    public static void main(String[] args) {
        Car car = new Car();

        car.start();
        car.brake();
        car.info();
        Engine.rules();
    }
}
```

Output:

```text
Car starts
Car brakes
Engine info
Follow the rules
```

Key points:

- Interface methods are `public abstract` by default.
- Interface fields are `public static final` by default.
- A class can implement multiple interfaces.
- `default` methods provide a body.
- `static` interface methods are called using the interface name.

---

## 21. Annotations

Annotations add metadata to code. They do not change the program logic by themselves.

```java
@interface Author {
    String name();
}

@Author(name = "Kunal")
class Book {
    @Deprecated
    void oldMethod() {
    }

    @Override
    public String toString() {
        return "Book";
    }
}

public class Main {
    public static void main(String[] args) {
        Book b = new Book();
        System.out.println(b);
        b.oldMethod();
    }
}
```

Output:

```text
Book
```

Common annotations:

- `@Override` asks the compiler to verify that a method overrides a parent method.
- `@Deprecated` marks code that should no longer be used.
- `@SuppressWarnings` suppresses selected compiler warnings.
- Custom annotations are declared with `@interface`.

---

# Part 6 — OOP 6: Generics, Lambdas, Exceptions, Cloning

## 22. Generics

Generics let a class or method work with a type parameter while keeping compile-time type safety.

```java
class Box<T> {
    private T value;

    void set(T value) {
        this.value = value;
    }

    T get() {
        return value;
    }
}

public class Main {
    public static void main(String[] args) {
        Box<String> b = new Box<>();
        b.set("Hello");

        String s = b.get();
        System.out.println(s);

        Box<Integer> n = new Box<>();
        n.set(42);
        System.out.println(n.get());
    }
}
```

Output:

```text
Hello
42
```

Generics avoid casts and catch type errors at compile time. Bounded types and wildcards add more control:

```java
class NumberBox<T extends Number> {
}

void printNumbers(List<? extends Number> numbers) {
}
```

---

## 23. Custom ArrayList

A custom ArrayList shows how a dynamic array grows automatically.

```java
class CustomList<T> {
    private Object[] data = new Object[10];
    private int size;

    void add(T value) {
        if (size == data.length) {
            resize();
        }
        data[size++] = value;
    }

    @SuppressWarnings("unchecked")
    T get(int index) {
        return (T) data[index];
    }

    int size() {
        return size;
    }

    private void resize() {
        Object[] bigger = new Object[data.length * 2];
        System.arraycopy(data, 0, bigger, 0, data.length);
        data = bigger;
    }
}

public class Main {
    public static void main(String[] args) {
        CustomList<String> list = new CustomList<>();

        for (int i = 0; i < 20; i++) {
            list.add("Item " + i);
        }

        System.out.println(list.size());
        System.out.println(list.get(19));
    }
}
```

Output:

```text
20
Item 19
```

The internal array doubles when full. Adding is usually O(1) amortized.

---

## 24. Lambda Expressions

A lambda is a short way to implement a functional interface — an interface with one abstract method.

```java
interface Operation {
    int apply(int a, int b);
}

public class Main {
    public static void main(String[] args) {
        Operation add = (a, b) -> a + b;
        Operation multiply = (a, b) -> a * b;

        System.out.println(add.apply(2, 3));
        System.out.println(multiply.apply(2, 3));
    }
}
```

Output:

```text
5
6
```

Lambdas are commonly used with `forEach`, `Comparator`, `Runnable`, and the `java.util.function` interfaces.

```java
List<Integer> numbers = List.of(1, 2, 3);
numbers.forEach(n -> System.out.println(n * 2));
```

Output:

```text
2
4
6
```

---

## 25. Exception Handling

Exceptions represent errors. `try` contains risky code, `catch` handles an exception, and `finally` always runs.

```java
class MyException extends Exception {
    MyException(String message) {
        super(message);
    }
}

public class Main {
    static int divide(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return a / b;
    }

    static void checkAge(int age) throws MyException {
        if (age < 18) {
            throw new MyException("Too young");
        }
    }

    public static void main(String[] args) {
        try {
            divide(10, 0);
        } catch (ArithmeticException e) {
            System.out.println(e.getMessage());
        } finally {
            System.out.println("Done");
        }

        try {
            checkAge(16);
        } catch (MyException e) {
            System.out.println(e.getMessage());
        }
    }
}
```

Output:

```text
Cannot divide by zero
Done
Too young
```

- `throw` raises an exception.
- `throws` declares that a method may pass a checked exception to its caller.
- Checked exceptions must be handled or declared.
- Unchecked exceptions extend `RuntimeException`.

---

## 26. Object Cloning

Cloning creates a copy of an object. A **shallow copy** shares referenced objects; a **deep copy** duplicates them.

```java
class Human implements Cloneable {
    int age;
    int[] marks = {90, 80};

    @Override
    public Object clone() throws CloneNotSupportedException {
        Human copy = (Human) super.clone();
        copy.marks = marks.clone();
        return copy;
    }
}

public class Main {
    public static void main(String[] args) throws CloneNotSupportedException {
        Human a = new Human();
        a.age = 20;

        Human b = (Human) a.clone();
        b.marks[0] = 100;

        System.out.println(a.marks[0]);
        System.out.println(b.marks[0]);
    }
}
```

Output:

```text
90
100
```

`super.clone()` creates a shallow copy. Cloning the `marks` array makes the copy independent. A copy constructor is often clearer than `clone()`.

---

# Part 7 — OOP 7: Collections, Vector, Enums

## 27. Collections Framework

The collections framework provides reusable data structures. The main interfaces are `List`, `Set`, `Queue`, and `Map`.

```java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("Java");
        list.add("OOP");

        Set<Integer> set = new HashSet<>();
        set.add(1);
        set.add(1);
        set.add(2);

        Map<String, Integer> map = new HashMap<>();
        map.put("Java", 1);
        map.put("OOP", 2);

        System.out.println(list);
        System.out.println(set);
        System.out.println(map.get("Java"));
    }
}
```

Output:

```text
[Java, OOP]
[1, 2]
1
```

- `List` keeps insertion order and allows duplicates.
- `Set` does not allow duplicates.
- `Map` stores key-value pairs.

---

## 28. Vector Class

`Vector` is a dynamic array like `ArrayList`, but its methods are synchronized.

```java
import java.util.Vector;

public class Main {
    public static void main(String[] args) {
        Vector<Integer> v = new Vector<>();

        v.add(45);
        v.add(5);
        v.add(15);

        System.out.println(v);
        System.out.println(v.get(1));
        System.out.println(v.size());
    }
}
```

Output:

```text
[45, 5, 15]
5
3
```

`Vector` is thread-safe but slower than `ArrayList` because of synchronization. Prefer `ArrayList` unless you specifically need synchronized access.

---

## 29. Enums

An enum is a fixed set of named constants. Enums are type-safe and can have constructors, fields, methods, and interfaces.

```java
interface Greeting {
    void hello();
}

enum Week implements Greeting {
    MONDAY, TUESDAY, WEDNESDAY;

    Week() {
        System.out.println("Constructor: " + this);
    }

    @Override
    public void hello() {
        System.out.println("Hello");
    }
}

public class Main {
    public static void main(String[] args) {
        Week day = Week.MONDAY;

        day.hello();
        System.out.println(day.ordinal());
        System.out.println(Week.valueOf("TUESDAY"));

        for (Week w : Week.values()) {
            System.out.println(w);
        }
    }
}
```

Output:

```text
Constructor: MONDAY
Constructor: TUESDAY
Constructor: WEDNESDAY
Hello
0
TUESDAY
MONDAY
TUESDAY
WEDNESDAY
```

Useful enum methods:

- `values()` returns all constants.
- `valueOf(String)` returns the constant with that name.
- `ordinal()` returns the zero-based position.
- `name()` returns the constant name.

---

# Quick Revision Checklist

- [ ] Explain the difference between a class and an object.
- [ ] Write a class with default, parameterized, and copy constructors.
- [ ] Explain `this`, `final`, and constructor chaining.
- [ ] Create and use a package with imports.
- [ ] Explain static variables, methods, and blocks.
- [ ] Write a singleton class.
- [ ] Explain inheritance and `super`.
- [ ] Write an example of method overriding and overloading.
- [ ] Explain encapsulation and abstraction.
- [ ] Create and use an interface.
- [ ] Explain `private`, default, `protected`, and `public`.
- [ ] Override `toString()`, `equals()`, and `hashCode()`.
- [ ] Write a generic class.
- [ ] Use a lambda with a functional interface.
- [ ] Handle an exception with `try`, `catch`, and `finally`.
- [ ] Explain shallow vs deep cloning.
- [ ] Use `ArrayList`, `HashSet`, and `HashMap`.
- [ ] Explain when to use `Vector`.
- [ ] Create an enum with a constructor and method.
