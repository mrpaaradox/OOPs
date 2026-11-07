package Learnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class OOP6 {
    public static void main(String[] args) throws CloneNotSupportedException {
        CloningMain.main(args);
        ExceptionMain.main(args);
        CustomArrayList.main(args);
        CustomGenArrayList.main(args);
        WildcardExample.main(args);
        LambdaFunctions.main(args);
        ComparingMain.main(args);
    }
}

class CloneHuman implements Cloneable {
    int age;
    String name;
    int[] arr;

    public CloneHuman(int age, String name) {
        this.age = age;
        this.name = name;
        this.arr = new int[]{3, 4, 5, 6, 9, 1};
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        CloneHuman twin = (CloneHuman) super.clone();

        twin.arr = new int[twin.arr.length];
        for (int i = 0; i < twin.arr.length; i++) {
            twin.arr[i] = this.arr[i];
        }
        return twin;
    }
}

class CloningMain {
    public static void main(String[] args) throws CloneNotSupportedException {
        CloneHuman kunal = new CloneHuman(34, "Kunal Kushwaha");

        CloneHuman twin = (CloneHuman) kunal.clone();
        System.out.println(twin.age + " " + twin.name);
        System.out.println(Arrays.toString(twin.arr));

        twin.arr[0] = 100;

        System.out.println(Arrays.toString(twin.arr));
        System.out.println(Arrays.toString(kunal.arr));
    }
}

class ExceptionDemo {
    public static void main(String[] args) {
        ExceptionMain.divide(3, 0);
    }
}

class ExceptionMain {
    public static void main(String[] args) {
        int a = 5;
        int b = 0;
        try {
            String name = "Kunal";
            if (name.equals("Kunal")) {
                throw new MyException("name is kunal");
            }
        } catch (MyException e) {
            System.out.println(e.getMessage());
        } catch (ArithmeticException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println("normal exception");
        } finally {
            System.out.println("this will always execute");
        }
    }

    static int divide(int a, int b) throws ArithmeticException {
        if (b == 0) {
            throw new ArithmeticException("please do no divide by zero");
        }

        return a / b;
    }
}

class MyException extends Exception {
    public MyException(String message) {
        super(message);
    }
}

class CustomArrayList {
    private int[] data;
    private static int DEFAULT_SIZE = 10;
    private int size = 0;

    public CustomArrayList() {
        this.data = new int[DEFAULT_SIZE];
    }

    public void add(int num) {
        if (isFull()) {
            resize();
        }
        data[size++] = num;
    }

    private void resize() {
        int[] temp = new int[data.length * 2];

        for (int i = 0; i < data.length; i++) {
            temp[i] = data[i];
        }
        data = temp;
    }

    private boolean isFull() {
        return size == data.length;
    }

    public int remove() {
        int removed = data[--size];
        return removed;
    }

    public int get(int index) {
        return data[index];
    }

    public int size() {
        return size;
    }

    public void set(int index, int value) {
        data[index] = value;
    }

    @Override
    public String toString() {
        return "CustomArrayList{" +
                "data=" + Arrays.toString(data) +
                ", size=" + size +
                '}';
    }

    public static void main(String[] args) {
        CustomArrayList list = new CustomArrayList();

        for (int i = 0; i < 14; i++) {
            list.add(2 * i);
        }

        System.out.println(list);

        ArrayList<Integer> list2 = new ArrayList<>();
    }
}

class CustomGenArrayList<T> {
    private Object[] data;
    private static int DEFAULT_SIZE = 10;
    private int size = 0;

    public CustomGenArrayList() {
        data = new Object[DEFAULT_SIZE];
    }

    public void add(T num) {
        if (isFull()) {
            resize();
        }
        data[size++] = num;
    }

    private void resize() {
        Object[] temp = new Object[data.length * 2];

        for (int i = 0; i < data.length; i++) {
            temp[i] = data[i];
        }
        data = temp;
    }

    private boolean isFull() {
        return size == data.length;
    }

    public T remove() {
        T removed = (T) (data[--size]);
        return removed;
    }

    public T get(int index) {
        return (T) data[index];
    }

    public int size() {
        return size;
    }

    public void set(int index, T value) {
        data[index] = value;
    }

    @Override
    public String toString() {
        return "CustomGenArrayList{" +
                "data=" + Arrays.toString(data) +
                ", size=" + size +
                '}';
    }

    public static void main(String[] args) {
        CustomGenArrayList list = new CustomGenArrayList();

        ArrayList<Integer> list2 = new ArrayList<>();

        CustomGenArrayList<Integer> list3 = new CustomGenArrayList<>();
        for (int i = 0; i < 14; i++) {
            list3.add(2 * i);
        }

        System.out.println(list3);
    }
}

interface GenericInterface<T> {
    void display(T value);
}

class GenericMain implements GenericInterface<Integer> {
    @Override
    public void display(Integer value) {
    }
}

class LambdaFunctions {
    public static void main(String[] args) {
        ArrayList<Integer> arr = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            arr.add(i + 1);
        }

        Consumer<Integer> fun = (item) -> System.out.println(item * 2);
        arr.forEach(fun);

        Operation sum = (a, b) -> a + b;
        Operation prod = (a, b) -> a * b;
        Operation sub = (a, b) -> a - b;

        LambdaFunctions myCalculator = new LambdaFunctions();
        System.out.println(myCalculator.operate(5, 3, sum));
        System.out.println(myCalculator.operate(5, 3, prod));
        System.out.println(myCalculator.operate(5, 3, sub));
    }

    private int operate(int a, int b, Operation op) {
        return op.operation(a, b);
    }
}

interface Operation {
    int operation(int a, int b);
}

class WildcardExample<T extends Number> {
    private Object[] data;
    private static int DEFAULT_SIZE = 10;
    private int size = 0;

    public WildcardExample() {
        data = new Object[DEFAULT_SIZE];
    }

    public void getList(List<? extends Number> list) {
    }

    public void add(T num) {
        if (isFull()) {
            resize();
        }
        data[size++] = num;
    }

    private void resize() {
        Object[] temp = new Object[data.length * 2];

        for (int i = 0; i < data.length; i++) {
            temp[i] = data[i];
        }
        data = temp;
    }

    private boolean isFull() {
        return size == data.length;
    }

    public T remove() {
        T removed = (T) (data[--size]);
        return removed;
    }

    public T get(int index) {
        return (T) data[index];
    }

    public int size() {
        return size;
    }

    public void set(int index, T value) {
        data[index] = value;
    }

    @Override
    public String toString() {
        return "CustomGenArrayList{" +
                "data=" + Arrays.toString(data) +
                ", size=" + size +
                '}';
    }

    public static void main(String[] args) {
        WildcardExample list = new WildcardExample();

        ArrayList<Integer> list2 = new ArrayList<>();

        WildcardExample<Integer> list3 = new WildcardExample<>();
        for (int i = 0; i < 14; i++) {
            list3.add(2 * i);
        }

        System.out.println(list3);
    }
}

class ComparingStudent implements Comparable<ComparingStudent> {
    int rollno;
    float marks;

    public ComparingStudent(int rollno, float marks) {
        this.rollno = rollno;
        this.marks = marks;
    }

    @Override
    public String toString() {
        return marks + "";
    }

    @Override
    public int compareTo(ComparingStudent o) {
        System.out.println("in compareto method");
        int diff = (int) (this.marks - o.marks);

        return diff;
    }
}

class ComparingMain {
    public static void main(String[] args) {
        ComparingStudent kunal = new ComparingStudent(12, 89.76f);
        ComparingStudent rahul = new ComparingStudent(5, 99.52f);
        ComparingStudent arpit = new ComparingStudent(2, 95.52f);
        ComparingStudent karan = new ComparingStudent(13, 77.52f);
        ComparingStudent sachin = new ComparingStudent(9, 96.52f);

        ComparingStudent[] list = {kunal, rahul, arpit, karan, sachin};

        System.out.println(Arrays.toString(list));

        Arrays.sort(list, (o1, o2) -> -(int) (o1.marks - o2.marks));

        System.out.println(Arrays.toString(list));
    }
}
