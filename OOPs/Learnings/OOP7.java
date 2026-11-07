package Learnings;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

public class OOP7 {
    public static void main(String[] args) {
        CollectionsMain.main(args);
        EnumBasic.main(args);
    }
}

class CollectionsMain {
    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>();
        List<Integer> list2 = new LinkedList<>();

        List<Integer> vector = new Vector<>();
        vector.add(45);
        vector.add(5);
        vector.add(15);
        vector.add(56);

        System.out.println(vector);
    }
}

interface EnumA {
    void hello();
}

class EnumBasic {
    enum Week implements EnumA {
        Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday;

        void display() {
        }

        Week() {
            System.out.println("Constructor called for " + this);
        }

        @Override
        public void hello() {
            System.out.println("hey how are you");
        }
    }

    public static void main(String[] args) {
        Week week;
        week = Week.Monday;
        week.hello();
        System.out.println(Week.valueOf("Monday"));
    }
}
