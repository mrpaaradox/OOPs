import java.util.Arrays;

public class OOP1 {
    public static void main(String[] args) {
        int[] numbers = new int[5];
        String[] names = new String[5];

        int[] rno = new int[5];
        String[] name = new String[5];
        float[] marks = new float[5];

        Student[] students = new Student[5];
        System.out.println(Arrays.toString(students));

        Student kunal = new Student(15, "Kunal Kushwaha", 85.4f);
        Student rahul = new Student(18, "Rahul Rana", 90.3f);

        System.out.println(kunal.rno + " " + kunal.name + " " + kunal.marks);
        System.out.println(rahul.rno + " " + rahul.name + " " + rahul.marks);

        kunal.greeting();
        kunal.changeName("Shoe lover");
        kunal.greeting();

        Student random = new Student(kunal);
        System.out.println(random.name);

        Student random2 = new Student();
        System.out.println(random2.name);

        Student one = new Student();
        Student two = one;

        one.name = "Something something";
        System.out.println(two.name);

        WrapperExample.main(new String[0]);
    }
}

class Student {
    int rno;
    String name;
    float marks = 90;

    void greeting() {
        System.out.println("Hello! My name is " + this.name);
    }

    void changeName(String name) {
        this.name = name;
    }

    Student(Student other) {
        this.name = other.name;
        this.rno = other.rno;
        this.marks = other.marks;
    }

    Student() {
        this(13, "default person", 100.0f);
    }

    Student(int rno, String name, float marks) {
        this.rno = rno;
        this.name = name;
        this.marks = marks;
    }
}

class WrapperExample {
    public static void main(String[] args) {
        int a = 10;
        int b = 20;

        Integer num = 45;

        Integer x = 10;
        Integer y = 20;

        swap(x, y);
        System.out.println(x + " " + y);

        final int bonus = 2;

        final A kunal = new A("Kunal Kushwaha");
        kunal.name = "other name";

        A obj = new A("Random name");
        System.out.println(obj);
    }

    static void swap(Integer a, Integer b) {
        Integer temp = a;
        a = b;
        b = temp;
    }
}

class A {
    final int num = 10;
    String name;

    public A(String name) {
        this.name = name;
    }

    @Override
    @SuppressWarnings("removal")   // finalize() is deprecated for removal; kept only to show it exists
    protected void finalize() throws Throwable {
        System.out.println("Object is destroyed");
    }
}
