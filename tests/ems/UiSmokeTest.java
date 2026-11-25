package ems;

import ems.ui.AddEmployeePanel;
import ems.ui.EmployeeListPanel;
import ems.ui.SalarySlipPanel;
import ems.ui.SearchPanel;
import javax.swing.*;
import java.awt.*;

/** Headless checks of panel wiring and the existing salary calculation. */
public class UiSmokeTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EmployeeService service = new EmployeeService();
            Employee employee = new Employee("E101", "Alice", "Engineering", "Developer", 40000);
            check(service.addEmployee(employee), "Add employee");
            check(service.findById(" e101 ") == employee, "Case-insensitive search");
            check(!service.addEmployee(employee), "Duplicate ID rejected");
            SalarySlip slip = new SalaryCalculator().generateSlip(employee);
            check(Math.abs(slip.getNetSalary() - 43800) < 0.001, "Expected net salary");
            check(Math.abs(slip.getGrossSalary() - 54000) < 0.001, "Expected gross salary");

            EmployeeListPanel list = new EmployeeListPanel(service);
            check(find(list, JTable.class).getRowCount() == 1, "Table shows employees");
            SearchPanel search = new SearchPanel(service);
            find(search, JTextField.class).setText("e101");
            button(search, "Search").doClick();
            check(find(search, JTextArea.class).getText().contains("Alice"), "Search displays details");

            SalarySlipPanel salary = new SalarySlipPanel(service, new SalaryCalculator());
            check(!button(salary, "Save as .txt").isEnabled(), "Export initially disabled");
            find(salary, JTextField.class).setText("E101");
            button(salary, "Generate").doClick();
            check(find(salary, JTextArea.class).getText().contains("NET SALARY"), "Slip generated");
            check(button(salary, "Save as .txt").isEnabled(), "Export enabled for generated slip");
            find(salary, JTextField.class).setText("different ID");
            check(!button(salary, "Save as .txt").isEnabled(), "Changing ID invalidates slip");

            AddEmployeePanel add = new AddEmployeePanel(value -> true);
            find(add, JTextField.class).setText("Temporary");
            button(add, "Clear").doClick();
            check(find(add, JTextField.class).getText().isEmpty(), "Clear resets fields");
            add.setSavingEnabled(false);
            check(!button(add, "Save Employee").isEnabled(), "Saving can be disabled after load failure");
        });
        System.out.println("UiSmokeTest: all tests passed");
    }

    private static JButton button(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JButton && text.equals(((JButton) component).getText())) {
                return (JButton) component;
            }
            if (component instanceof Container) {
                JButton result = button((Container) component, text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static <T> T find(Container parent, Class<T> type) {
        for (Component component : parent.getComponents()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
            if (component instanceof Container) {
                T result = find((Container) component, type);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
