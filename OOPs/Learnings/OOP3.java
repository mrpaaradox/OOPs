package Learnings;
import java.util.ArrayList;

public class OOP3 {
    public static void main(String[] args) {
        InheritanceMain.main(args);
        PolymorphismMain.main(args);
        Numbers.main(args);
        ObjectPrint.main(args);
    }
}

class Box {
    private double l;
    double h;
    double w;

    static void greeting() {
        System.out.println("Hey, I am in Box class. Greetings!");
    }

    public double getL() {
        return l;
    }

    Box() {
        this.h = -1;
        this.l = -1;
        this.w = -1;
    }

    Box(double side) {
        this.w = side;
        this.l = side;
        this.h = side;
    }

    Box(double l, double h, double w) {
        System.out.println("Box class constructor");
        this.l = l;
        this.h = h;
        this.w = w;
    }

    Box(Box old) {
        this.h = old.h;
        this.l = old.l;
        this.w = old.w;
    }
}

class BoxWeight extends Box {
    double weight;

    public BoxWeight() {
        this.weight = -1;
    }

    static void greeting() {
        System.out.println("Hey, I am in BoxWeight class. Greetings!");
        ArrayList list = new ArrayList();
    }

    BoxWeight(BoxWeight other) {
        super(other);
        weight = other.weight;
    }

    BoxWeight(double side, double weight) {
        super(side);
        this.weight = weight;
    }

    public BoxWeight(double l, double h, double w, double weight) {
        super(l, h, w);
        this.weight = weight;
    }
}

class BoxColor extends BoxWeight {
}

class BoxPrice extends BoxWeight {
    double cost;

    BoxPrice() {
        super();
        this.cost = -1;
    }

    BoxPrice(BoxPrice other) {
        super(other);
        this.cost = other.cost;
    }

    public BoxPrice(double l, double h, double w, double weight, double cost) {
        super(l, h, w, weight);
        this.cost = cost;
    }

    public BoxPrice(double side, double weight, double cost) {
        super(side, weight);
        this.cost = cost;
    }
}

class InheritanceMain {
    public static void main(String[] args) {
        Box box1 = new Box(4.6, 7.9, 9.9);
        box1.getL();
        Box box2 = new Box(box1);

        BoxWeight box = new BoxWeight();
        BoxWeight.greeting();
    }
}

class Shapes {
    void area() {
        System.out.println("I am in shapes");
    }
}

class Circle extends Shapes {
    @Override
    void area() {
        System.out.println("Area is pie * r * r");
    }
}

class Square extends Shapes {
    void area() {
        System.out.println("Area is square of side");
    }
}

class Triangle extends Shapes {
    void area() {
        System.out.println("Area is 0.5 * h * b");
    }
}

class PolymorphismMain {
    public static void main(String[] args) {
        Shapes shape = new Shapes();
        Shapes circle = new Circle();
        Shapes square = new Square();

        circle.area();
    }
}

class Numbers {
    double sum(double a, int b) {
        return a + b;
    }

    double sum(int a, int b) {
        return a + b;
    }

    int sum(int a, int b, int c) {
        return a + b + c;
    }

    public static void main(String[] args) {
        Numbers obj = new Numbers();

        obj.sum(2, 3);
        obj.sum(1, 3, 7);
    }
}

class ObjectPrint {
    int num;

    public ObjectPrint(int num) {
        this.num = num;
    }

    @Override
    public String toString() {
        return "ObjectPrint{" +
                "num=" + num +
                '}';
    }

    public static void main(String[] args) {
        ObjectPrint obj = new ObjectPrint(54);

        System.out.println(obj);
    }
}
