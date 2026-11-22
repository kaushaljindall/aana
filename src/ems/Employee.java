package ems;

public class Employee {
    private final String id;
    private String name;
    private String department;
    private String designation;
    private double basicSalary;

    public Employee(String id, String name, String department, String designation, double basicSalary) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.designation = designation;
        this.basicSalary = basicSalary;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public String getDesignation() {
        return designation;
    }

    public double getBasicSalary() {
        return basicSalary;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public void setBasicSalary(double basicSalary) {
        this.basicSalary = basicSalary;
    }

    @Override
    public String toString() {
        return String.format("ID: %-8s Name: %-15s Dept: %-12s Designation: %-15s Basic: %.2f",
                id, name, department, designation, basicSalary);
    }
}
