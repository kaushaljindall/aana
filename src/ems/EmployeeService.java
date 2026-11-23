package ems;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmployeeService {
    private final Map<String, Employee> employees = new LinkedHashMap<String, Employee>();

    /** Returns false if an employee with the same ID already exists. */
    public boolean addEmployee(Employee employee) {
        String key = normalize(employee.getId());
        if (employees.containsKey(key)) {
            return false;
        }
        employees.put(key, employee);
        return true;
    }

    /** Returns the employee, or null if not found. */
    public Employee findById(String id) {
        return employees.get(normalize(id));
    }

    public List<Employee> getAllEmployees() {
        return new ArrayList<Employee>(employees.values());
    }

    private String normalize(String id) {
        return id.trim().toUpperCase();
    }
}
