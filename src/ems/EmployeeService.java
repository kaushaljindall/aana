package ems;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmployeeService {
    private final Map<String, Employee> employees = new LinkedHashMap<String, Employee>();

    public boolean addEmployee(Employee employee) {
        if (employees.containsKey(employee.getId())) {
            return false;
        }
        employees.put(employee.getId(), employee);
        return true;
    }

    public List<Employee> getAllEmployees() {
        return new ArrayList<Employee>(employees.values());
    }
}