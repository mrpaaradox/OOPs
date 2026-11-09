public class OOP5 {
    public static void main(String[] args) {
        AbstractMain.main(args);
        InterfacesMain.main(args);
        DefaultMain.main(args);
        NestedMain.main(args);
    }
}

abstract class AbstractParent {
    int age;
    final int VALUE;

    public AbstractParent(int age) {
        this.age = age;
        VALUE = 32456789;
    }

    static void hello() {
        System.out.println("hey");
    }

    void normal() {
        System.out.println("this is a normal method");
    }

    abstract void career();

    abstract void partner();
}

abstract class AbstractParent2 {
    int age;
    final int VALUE;

    public AbstractParent2(int age) {
        this.age = age;
        VALUE = 32456789;
    }

    static void hello() {
        System.out.println("hey");
    }

    void normal() {
        System.out.println("this is a normal method");
    }

    abstract void career();

    abstract void partner();
}

class AbstractSon extends AbstractParent {
    public AbstractSon(int age) {
        super(age);
    }

    @Override
    void normal() {
        super.normal();
    }

    @Override
    void career() {
        System.out.println("I am going to be a doctor");
    }

    @Override
    void partner() {
        System.out.println("I love Pepper Potts");
    }
}

class AbstractDaughter extends AbstractParent {
    public AbstractDaughter(int age) {
        super(age);
    }

    @Override
    void career() {
        System.out.println("I am going to be a coder");
    }

    @Override
    void partner() {
        System.out.println("I love Iron Man");
    }
}

class AbstractMain {
    public static void main(String[] args) {
        AbstractSon son = new AbstractSon(30);
        son.career();

        son.normal();

        AbstractParent daughter = new AbstractDaughter(28);
        daughter.career();

        AbstractParent.hello();
    }
}

interface Engine {
    int PRICE = 78000;

    void start();

    void stop();

    void acc();
}

interface Brake {
    void brake();
}

interface Media {
    void start();

    void stop();
}

class Car implements Engine, Brake, Media {
    int a = 30;

    @Override
    public void brake() {
        System.out.println("I brake like a normal Car");
    }

    @Override
    public void start() {
        System.out.println("I start engine like a normal Car");
    }

    @Override
    public void stop() {
        System.out.println("I stop engine like a normal Car");
    }

    @Override
    public void acc() {
        System.out.println("I accelerate like a normal Car");
    }
}

class ElectricEngine implements Engine {
    @Override
    public void start() {
        System.out.println("Electric engine start");
    }

    @Override
    public void stop() {
        System.out.println("Electric engine stop");
    }

    @Override
    public void acc() {
        System.out.println("Electric engine accelerate");
    }
}

class PowerEngine implements Engine {
    @Override
    public void start() {
        System.out.println("Power engine start");
    }

    @Override
    public void stop() {
        System.out.println("Power engine stop");
    }

    @Override
    public void acc() {
        System.out.println("Power engine accelerate");
    }
}

class CDPlayer implements Media {
    @Override
    public void start() {
        System.out.println("Music start");
    }

    @Override
    public void stop() {
        System.out.println("Music stop");
    }
}

class NiceCar {
    private Engine engine;
    private Media player = new CDPlayer();

    public NiceCar() {
        engine = new PowerEngine();
    }

    public NiceCar(Engine engine) {
        this.engine = engine;
    }

    public void start() {
        engine.start();
    }

    public void stop() {
        engine.stop();
    }

    public void startMusic() {
        player.start();
    }

    public void stopMusic() {
        player.stop();
    }

    public void upgradeEngine() {
        this.engine = new ElectricEngine();
    }
}

class InterfacesMain {
    public static void main(String[] args) {
        NiceCar car = new NiceCar();

        car.start();
        car.startMusic();
        car.upgradeEngine();
        car.start();
    }
}

interface ExtendA {
    void fun();
}

interface ExtendB extends ExtendA {
    void greet();
}

class ExtendMain implements ExtendB {
    @Override
    public void fun() {
    }

    @Override
    public void greet() {
    }
}

interface DefaultA {
    static void greeting() {
        System.out.println("Hey I am static method");
    }

    default void fun() {
        System.out.println("I am in A");
    }
}

interface DefaultB {
    void greet();
}

class DefaultMain implements DefaultA, DefaultB {
    @Override
    public void greet() {
    }

    public static void main(String[] args) {
        DefaultMain obj = new DefaultMain();
        DefaultA.greeting();
    }
}

class NestedA {
    public interface NestedInterface {
        boolean isOdd(int num);
    }
}

class NestedB implements NestedA.NestedInterface {
    @Override
    public boolean isOdd(int num) {
        return (num & 1) == 1;
    }
}

class NestedMain {
    public static void main(String[] args) {
        NestedB obj = new NestedB();
        System.out.println(obj.isOdd(6));
    }
}
