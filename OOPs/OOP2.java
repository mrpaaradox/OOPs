public class OOP2 {
    public static void main(String[] args) {
        PackageDemo.main(args);
        GreetingB.main(args);
        StaticDemo.main(args);
        StaticBlock.main(args);
        InnerClasses.main(args);
        SingletonDemo.main(args);
    }
}

class PackageDemo {
    public static void main(String[] args) {
        System.out.println("Hello world");
        Message.message();
    }
}

class GreetingB {
    public static void main(String[] args) {
        System.out.println(" I am awesome");
    }
}

class Message {
    public static void main(String[] args) {
    }

    public static void message() {
        System.out.println("This course is awesome");
    }
}

class Human {
    int age;
    String name;
    int salary;
    boolean married;
    static long population;

    static void message() {
        System.out.println("Hello world");
    }

    public Human(int age, String name, int salary, boolean married) {
        this.age = age;
        this.name = name;
        this.salary = salary;
        this.married = married;
        Human.population += 1;
    }
}

class StaticDemo {
    public static void main(String[] args) {
        Human kunal = new Human(22, "Kunal", 10000, false);
        Human rahul = new Human(34, "Rahul", 15000, true);
        Human arpit = new Human(34, "Arpit", 15000, true);

        System.out.println(Human.population);
        System.out.println(Human.population);
        System.out.println(Human.population);

        StaticDemo funn = new StaticDemo();
        funn.fun2();
    }

    static void fun() {
        StaticDemo obj = new StaticDemo();
        obj.greeting();
    }

    void fun2() {
        greeting();
    }

    void greeting() {
        System.out.println("Hello world");
    }
}

class StaticBlock {
    static int a = 4;
    static int b;

    static {
        System.out.println("I am in static block");
        b = a * 5;
    }

    public static void main(String[] args) {
        StaticBlock obj = new StaticBlock();
        System.out.println(StaticBlock.a + " " + StaticBlock.b);

        StaticBlock.b += 3;

        System.out.println(StaticBlock.a + " " + StaticBlock.b);

        StaticBlock obj2 = new StaticBlock();
        System.out.println(StaticBlock.a + " " + StaticBlock.b);
    }
}

class InnerClasses {
    static class Test {
        String name;

        public Test(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static void main(String[] args) {
        Test a = new Test("Kunal");
        Test b = new Test("Rahul");

        System.out.println(a);
    }
}

class Singleton {
    private Singleton() {
    }

    private static Singleton instance;

    public static Singleton getInstance() {
        if (instance == null) {
            instance = new Singleton();
        }

        return instance;
    }
}

class SingletonDemo {
    public static void main(String[] args) {
        Singleton obj1 = Singleton.getInstance();
        Singleton obj2 = Singleton.getInstance();
        Singleton obj3 = Singleton.getInstance();
    }
}
