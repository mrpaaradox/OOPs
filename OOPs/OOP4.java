public class OOP4 {
    public static void main(String[] args) {
        AccessMain.main(args);
        ObjectDemo.main(args);
        AccessSubClass.main(args);
        SingletonSubClass.main(args);
        SubSubclass.main(args);
        SubClass2.main(args);
        NotSubClass.main(args);
    }
}

class AccessA {
    protected int num;
    String name;
    int[] arr;

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public AccessA(int num, String name) {
        this.num = num;
        this.name = name;
        this.arr = new int[num];
    }
}

class AccessMain {
    public static void main(String[] args) {
        AccessA obj = new AccessA(10, "Kunal");

        obj.getNum();
        int n = obj.num;
    }
}

class ObjectDemo {
    int num;
    float gpa;

    public ObjectDemo(int num, float gpa) {
        this.num = num;
        this.gpa = gpa;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.num == ((ObjectDemo) obj).num;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public static void main(String[] args) {
        ObjectDemo obj1 = new ObjectDemo(12, 56.8f);
        ObjectDemo obj2 = new ObjectDemo(12, 78.9f);

        if (obj1 == obj2) {
            System.out.println("obj1 is equal to obj2");
        }

        if (obj1.equals(obj2)) {
            System.out.println("obj1 is equal to obj2");
        }

        System.out.println(obj1.getClass().getName());
    }
}

class AccessSubClass extends AccessA {
    public AccessSubClass(int num, String name) {
        super(num, name);
    }

    public static void main(String[] args) {
        AccessSubClass obj = new AccessSubClass(45, "Kunal Kushwaha");
        int n = obj.num;

        System.out.println(obj instanceof Object);
    }
}

class NotSubClass {
    public static void main(String[] args) {
        NotSubClass obj = new NotSubClass();
    }
}

class SingletonSubClass extends AccessA {
    public SingletonSubClass(int num, String name) {
        super(num, name);
    }

    public static void main(String[] args) {
        AccessA obj = new AccessA(45, "Kunal Kushwaha");
    }
}

class SubSubclass extends SingletonSubClass {
    public SubSubclass(int num, String name) {
        super(num, name);
    }

    public static void main(String[] args) {
        SubSubclass obj = new SubSubclass(45, "Kunal Kushwaha");
        int n = obj.num;
    }
}

class SubClass2 extends AccessA {
    public SubClass2(int num, String name) {
        super(num, name);
    }

    public static void main(String[] args) {
        SubClass2 obj = new SubClass2(45, "Kunal Kushwaha");
    }
}
