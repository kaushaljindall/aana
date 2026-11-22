package ems;

import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static final EmployeeService service = new EmployeeService();
    private static final SalaryCalculator calculator = new SalaryCalculator();

    public static void main(String[] args) {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Enter your choice: ");
            switch (choice) {
                case 1:
                    addEmployee();
                    break;
                case 2:
                    searchEmployee();
                    break;
                case 3:
                    generateSlip();
                    break;
                case 4:
                    listEmployees();
                    break;
                case 0:
                    running = false;
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("===== Employee Salary Management System =====");
        System.out.println("1. Add employee");
        System.out.println("2. Search employee by ID");
        System.out.println("3. Generate salary slip");
        System.out.println("4. List all employees");
        System.out.println("0. Exit");
    }

    private static void addEmployee() {
        String id = readNonEmpty("Employee ID: ");
        if (service.findById(id) != null) {
            System.out.println("An employee with this ID already exists.");
            return;
        }
        String name = readNonEmpty("Name: ");
        String department = readNonEmpty("Department: ");
        String designation = readNonEmpty("Designation: ");
        double basic = readPositiveDouble("Basic salary: ");

        service.addEmployee(new Employee(id.trim().toUpperCase(), name, department, designation, basic));
        System.out.println("Employee added successfully.");
    }

    private static void searchEmployee() {
        String id = readNonEmpty("Enter employee ID: ");
        Employee e = service.findById(id);
        if (e == null) {
            System.out.println("No employee found with ID " + id.trim().toUpperCase());
        } else {
            System.out.println(e);
        }
    }

    private static void generateSlip() {
        String id = readNonEmpty("Enter employee ID: ");
        Employee e = service.findById(id);
        if (e == null) {
            System.out.println("No employee found with ID " + id.trim().toUpperCase());
            return;
        }
        SalarySlip slip = calculator.generateSlip(e);
        System.out.println();
        System.out.print(slip.format());
    }

    private static void listEmployees() {
        List<Employee> all = service.getAllEmployees();
        if (all.isEmpty()) {
            System.out.println("No employees added yet.");
            return;
        }
        for (Employee e : all) {
            System.out.println(e);
        }
    }

    // ---------- Input helpers ----------

    private static String readNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("Value cannot be empty.");
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static double readPositiveDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double value = Double.parseDouble(scanner.nextLine().trim());
                if (value > 0) {
                    return value;
                }
                System.out.println("Value must be greater than 0.");
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
}
