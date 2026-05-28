package com.javaassignment.data;

import com.javaassignment.model.Problem;
import java.util.Arrays;
import java.util.List;

public class ProblemData {

    public static List<Problem> getDefaultProblems() {
        return Arrays.asList(

            // ─── ASSIGNMENT PART 2 ───────────────────────────────────────────

            new Problem(1, "Part 2", "Q1",
                "Shape Class – Area, Perimeter & Volume",
                "Implement class Shape with members length, breadth, height & calculate area(), perimeter(), volume(). Create another supportive class to create objects. Input will be taken from user.",
                """
                import java.util.Scanner;

                class Shape {
                    double length, breadth, height;

                    void setValues(double l, double b, double h) {
                        length = l;
                        breadth = b;
                        height = h;
                    }

                    double area() {
                        return length * breadth;
                    }

                    double perimeter() {
                        return 2 * (length + breadth);
                    }

                    double volume() {
                        return length * breadth * height;
                    }

                    void display() {
                        System.out.println("Length: " + length);
                        System.out.println("Breadth: " + breadth);
                        System.out.println("Height: " + height);
                        System.out.println("Area: " + area());
                        System.out.println("Perimeter: " + perimeter());
                        System.out.println("Volume: " + volume());
                    }
                }

                public class ShapeMain {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        Shape s = new Shape();
                        System.out.print("Enter length: ");
                        double l = sc.nextDouble();
                        System.out.print("Enter breadth: ");
                        double b = sc.nextDouble();
                        System.out.print("Enter height: ");
                        double h = sc.nextDouble();
                        s.setValues(l, b, h);
                        System.out.println("--- Shape Details ---");
                        s.display();
                        sc.close();
                    }
                }
                """,
                "STDIN", "5\n3\n4"),

            new Problem(2, "Part 2", "Q3",
                "Static Variables and Methods",
                "Implement use of static variables and methods in a class. Demonstrate shared data across all instances using static members.",
                """
                class Student {
                    String name;
                    int rollNo;
                    static String college = "ABC Engineering College";
                    static int count = 0;

                    Student(String n, int r) {
                        name = n;
                        rollNo = r;
                        count++;
                    }

                    void display() {
                        System.out.println(rollNo + " - " + name + " - " + college);
                    }

                    static void showCount() {
                        System.out.println("Total students: " + count);
                    }
                }

                public class StaticDemo {
                    public static void main(String[] args) {
                        Student s1 = new Student("Rahul", 101);
                        Student s2 = new Student("Priya", 102);
                        Student s3 = new Student("Amit", 103);
                        s1.display();
                        s2.display();
                        s3.display();
                        Student.showCount();
                    }
                }
                """,
                "NONE", ""),

            new Problem(3, "Part 2", "Q4",
                "Method Overloading",
                "Perform method overloading with a basic example. Demonstrate multiple methods with the same name but different parameter types or counts.",
                """
                class Calculator {
                    int add(int a, int b) {
                        return a + b;
                    }

                    int add(int a, int b, int c) {
                        return a + b + c;
                    }

                    double add(double a, double b) {
                        return a + b;
                    }
                }

                public class MethodOverloadDemo {
                    public static void main(String[] args) {
                        Calculator calc = new Calculator();
                        System.out.println("add(10, 20) = " + calc.add(10, 20));
                        System.out.println("add(10, 20, 30) = " + calc.add(10, 20, 30));
                        System.out.println("add(5.5, 3.5) = " + calc.add(5.5, 3.5));
                    }
                }
                """,
                "NONE", ""),

            new Problem(4, "Part 2", "Q5",
                "Constructor Overloading",
                "Perform constructor overloading with a basic example. Create a Box class with multiple constructors — default, cube, and custom dimensions.",
                """
                class Box {
                    double length, breadth, height;

                    Box() {
                        length = 1;
                        breadth = 1;
                        height = 1;
                    }

                    Box(double l) {
                        length = breadth = height = l;
                    }

                    Box(double l, double b, double h) {
                        length = l;
                        breadth = b;
                        height = h;
                    }

                    double volume() {
                        return length * breadth * height;
                    }
                }

                public class ConstructorOverloadDemo {
                    public static void main(String[] args) {
                        Box b1 = new Box();
                        Box b2 = new Box(5);
                        Box b3 = new Box(4, 3, 2);
                        System.out.println("Default Box volume: " + b1.volume());
                        System.out.println("Cube Box(5) volume: " + b2.volume());
                        System.out.println("Box(4,3,2) volume: " + b3.volume());
                    }
                }
                """,
                "NONE", ""),

            new Problem(5, "Part 2", "Q6",
                "Room Class – Surface Area & Volume",
                "Create a class Room with private variables for length, width, height. Add constructor, surfaceArea() and volume() methods. Create a Main class to instantiate and display two Room objects.",
                """
                import java.util.Scanner;

                class Room {
                    private double length;
                    private double width;
                    private double height;

                    Room(double length, double width, double height) {
                        this.length = length;
                        this.width = width;
                        this.height = height;
                    }

                    double surfaceArea() {
                        return 2 * (length * width + width * height + height * length);
                    }

                    double volume() {
                        return length * width * height;
                    }

                    void display() {
                        System.out.println("Length: " + length);
                        System.out.println("Width: " + width);
                        System.out.println("Height: " + height);
                        System.out.println("Surface Area: " + surfaceArea());
                        System.out.println("Volume: " + volume());
                    }
                }

                public class RoomMain {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        System.out.println("--- Room 1 ---");
                        System.out.print("Enter length: ");
                        double l1 = sc.nextDouble();
                        System.out.print("Enter width: ");
                        double w1 = sc.nextDouble();
                        System.out.print("Enter height: ");
                        double h1 = sc.nextDouble();
                        Room room1 = new Room(l1, w1, h1);

                        System.out.println("--- Room 2 ---");
                        System.out.print("Enter length: ");
                        double l2 = sc.nextDouble();
                        System.out.print("Enter width: ");
                        double w2 = sc.nextDouble();
                        System.out.print("Enter height: ");
                        double h2 = sc.nextDouble();
                        Room room2 = new Room(l2, w2, h2);

                        System.out.println("\\n=== Room 1 Details ===");
                        room1.display();
                        System.out.println("\\n=== Room 2 Details ===");
                        room2.display();
                        sc.close();
                    }
                }
                """,
                "STDIN", "10\n8\n12\n5\n4\n6"),

            // ─── ASSIGNMENT PART 3 ───────────────────────────────────────────

            new Problem(6, "Part 3", "Q1",
                "Final Keyword – Stop Override, Inheritance & Constant",
                "Demonstrate three uses of the final keyword: (a) Stopping method overriding, (b) Stopping class inheritance, (c) Creating a constant variable.",
                """
                class Animal {
                    final void sound() {
                        System.out.println("Animal makes a sound");
                    }
                }

                class Dog extends Animal {
                    // Cannot override sound() — it is final
                }

                final class Vehicle {
                    void run() {
                        System.out.println("Vehicle is running");
                    }
                }

                class FinalDemo {
                    public static void main(String[] args) {
                        System.out.println("a. Stopping Overriding with Final:");
                        Dog d = new Dog();
                        d.sound();

                        System.out.println("b. Stopping Inheritance with Final:");
                        Vehicle v = new Vehicle();
                        v.run();

                        System.out.println("c. Creating Constant with Final:");
                        final double PI = 3.14159;
                        double radius = 5.0;
                        double area = PI * radius * radius;
                        System.out.println("Value of PI: " + PI);
                        System.out.println("Area of circle with radius " + radius + ": " + area);
                    }
                }
                """,
                "NONE", ""),

            new Problem(7, "Part 3", "Q2",
                "Single Inheritance",
                "Write a Java program to implement single inheritance. A Manager class extends Employee class and inherits its properties.",
                """
                class Employee {
                    String name;
                    double salary;

                    void display() {
                        System.out.println("Name: " + name);
                        System.out.println("Salary: " + salary);
                    }
                }

                class Manager extends Employee {
                    String department;

                    void showDetails() {
                        display();
                        System.out.println("Department: " + department);
                    }
                }

                public class SingleInheritance {
                    public static void main(String[] args) {
                        Manager m = new Manager();
                        m.name = "Rahul Sharma";
                        m.salary = 75000;
                        m.department = "IT";
                        System.out.println("--- Single Inheritance ---");
                        m.showDetails();
                    }
                }
                """,
                "NONE", ""),

            new Problem(8, "Part 3", "Q3",
                "Multilevel Inheritance",
                "Write a Java program to implement multilevel inheritance. Person → Student → GraduateStudent, each level adding more specific data.",
                """
                class Person {
                    String name;

                    void displayName() {
                        System.out.println("Name: " + name);
                    }
                }

                class Student extends Person {
                    int rollNo;

                    void displayStudent() {
                        displayName();
                        System.out.println("Roll No: " + rollNo);
                    }
                }

                class GraduateStudent extends Student {
                    String specialization;

                    void displayAll() {
                        displayStudent();
                        System.out.println("Specialization: " + specialization);
                    }
                }

                public class MultilevelInheritance {
                    public static void main(String[] args) {
                        GraduateStudent gs = new GraduateStudent();
                        gs.name = "Priya Patel";
                        gs.rollNo = 101;
                        gs.specialization = "Data Science";
                        System.out.println("--- Multilevel Inheritance ---");
                        gs.displayAll();
                    }
                }
                """,
                "NONE", ""),

            new Problem(9, "Part 3", "Q4",
                "Hierarchical Inheritance",
                "Write a Java program to implement hierarchical inheritance. Circle, Rectangle, and Triangle all extend the Shape base class.",
                """
                class Shape {
                    String color;

                    void displayColor() {
                        System.out.println("Color: " + color);
                    }
                }

                class Circle extends Shape {
                    double radius;

                    void area() {
                        System.out.println("Circle Area: " + (3.14159 * radius * radius));
                    }
                }

                class Rectangle extends Shape {
                    double length, breadth;

                    void area() {
                        System.out.println("Rectangle Area: " + (length * breadth));
                    }
                }

                class Triangle extends Shape {
                    double base, height;

                    void area() {
                        System.out.println("Triangle Area: " + (0.5 * base * height));
                    }
                }

                public class HierarchicalInheritance {
                    public static void main(String[] args) {
                        System.out.println("--- Hierarchical Inheritance ---");

                        Circle c = new Circle();
                        c.color = "Red";
                        c.radius = 7;
                        c.displayColor();
                        c.area();
                        System.out.println();

                        Rectangle r = new Rectangle();
                        r.color = "Blue";
                        r.length = 10;
                        r.breadth = 5;
                        r.displayColor();
                        r.area();
                        System.out.println();

                        Triangle t = new Triangle();
                        t.color = "Green";
                        t.base = 8;
                        t.height = 6;
                        t.displayColor();
                        t.area();
                    }
                }
                """,
                "NONE", ""),

            new Problem(10, "Part 3", "Q5",
                "Static Binding (Method Overloading)",
                "Write a Java program to implement static binding. Overloaded multiply() methods are resolved at compile time based on parameter types.",
                """
                class Calculator {
                    static int multiply(int a, int b) {
                        return a * b;
                    }

                    static double multiply(double a, double b) {
                        return a * b;
                    }

                    static int multiply(int a, int b, int c) {
                        return a * b * c;
                    }
                }

                public class StaticBinding {
                    public static void main(String[] args) {
                        System.out.println("--- Static Binding (Method Overloading) ---");
                        System.out.println("multiply(4, 5) = " + Calculator.multiply(4, 5));
                        System.out.println("multiply(2.5, 3.5) = " + Calculator.multiply(2.5, 3.5));
                        System.out.println("multiply(2, 3, 4) = " + Calculator.multiply(2, 3, 4));
                    }
                }
                """,
                "NONE", ""),

            // ─── ASSIGNMENT PART 4 ───────────────────────────────────────────

            new Problem(11, "Part 4", "Q1",
                "Packages – Sum & Multiply",
                "Implement sum function in one package and multiply function in another package. Import these two packages and use those functions.",
                """
                // Package: mathops → Sum class
                class Sum {
                    public int add(int a, int b) {
                        return a + b;
                    }
                }

                // Package: mathops2 → Multiply class
                class Multiply {
                    public int mul(int a, int b) {
                        return a * b;
                    }
                }

                // Main class imports both packages
                public class PackageDemo {
                    public static void main(String[] args) {
                        Sum s = new Sum();
                        Multiply m = new Multiply();
                        int a = 10, b = 5;
                        System.out.println("Sum of " + a + " and " + b + " = " + s.add(a, b));
                        System.out.println("Product of " + a + " and " + b + " = " + m.mul(a, b));
                    }
                }
                """,
                "NONE", ""),

            new Problem(12, "Part 4", "Q2",
                "User-Defined Exception",
                "Create a user defined Exception and implement try-catch block to handle it. Throws InsufficientFundsException when withdrawal amount exceeds account balance.",
                """
                class InsufficientFundsException extends Exception {
                    double shortage;

                    InsufficientFundsException(double shortage) {
                        super("Insufficient funds! Short by Rs. " + shortage);
                        this.shortage = shortage;
                    }
                }

                class BankAccount {
                    private String owner;
                    private double balance;

                    BankAccount(String owner, double balance) {
                        this.owner = owner;
                        this.balance = balance;
                    }

                    void deposit(double amount) {
                        balance += amount;
                        System.out.println("Deposited Rs. " + amount + " | Balance: Rs. " + balance);
                    }

                    void withdraw(double amount) throws InsufficientFundsException {
                        if (amount > balance) {
                            throw new InsufficientFundsException(amount - balance);
                        }
                        balance -= amount;
                        System.out.println("Withdrawn Rs. " + amount + " | Balance: Rs. " + balance);
                    }

                    void showBalance() {
                        System.out.println(owner + "'s Balance: Rs. " + balance);
                    }
                }

                public class BankDemo {
                    public static void main(String[] args) {
                        BankAccount acc = new BankAccount("Suvadip", 1000.0);
                        acc.showBalance();

                        try {
                            acc.deposit(500.0);
                            acc.withdraw(300.0);
                            acc.withdraw(2000.0);
                        } catch (InsufficientFundsException e) {
                            System.out.println("Exception Caught: " + e.getMessage());
                        }

                        acc.showBalance();
                    }
                }
                """,
                "NONE", ""),

            new Problem(13, "Part 4", "Q3",
                "Multi-Threading with Thread Class",
                "Implement multiple child classes using Thread class and run them in parallel. Three threads run simultaneously printing their counts.",
                """
                class ThreadOne extends Thread {
                    public void run() {
                        for (int i = 1; i <= 3; i++) {
                            System.out.println("ThreadOne - Count: " + i);
                        }
                    }
                }

                class ThreadTwo extends Thread {
                    public void run() {
                        for (int i = 1; i <= 3; i++) {
                            System.out.println("ThreadTwo - Count: " + i);
                        }
                    }
                }

                class ThreadThree extends Thread {
                    public void run() {
                        for (int i = 1; i <= 3; i++) {
                            System.out.println("ThreadThree - Count: " + i);
                        }
                    }
                }

                public class MultiThreadDemo {
                    public static void main(String[] args) throws InterruptedException {
                        ThreadOne t1 = new ThreadOne();
                        ThreadTwo t2 = new ThreadTwo();
                        ThreadThree t3 = new ThreadThree();
                        t1.start();
                        t2.start();
                        t3.start();
                        t1.join();
                        t2.join();
                        t3.join();
                    }
                }
                """,
                "NONE", ""),

            new Problem(14, "Part 4", "Q4",
                "Interface – Rectangle & Circle",
                "Create interfaces named Rectangle and Circle having area methods. Implement both interfaces in a single Shape class.",
                """
                interface Rectangle {
                    double rectArea(double length, double breadth);
                }

                interface Circle {
                    double circleArea(double radius);
                }

                class Shape implements Rectangle, Circle {
                    public double rectArea(double length, double breadth) {
                        return length * breadth;
                    }

                    public double circleArea(double radius) {
                        return 3.14159 * radius * radius;
                    }
                }

                public class InterfaceDemo {
                    public static void main(String[] args) {
                        Shape s = new Shape();
                        System.out.println("Rectangle Area (5 x 4) = " + s.rectArea(5, 4));
                        System.out.println("Circle Area (radius 7) = " + s.circleArea(7));
                    }
                }
                """,
                "NONE", "")
        );
    }
}
