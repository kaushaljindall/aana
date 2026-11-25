package ems.ui;

import ems.Employee;
import ems.EmployeeService;
import javax.swing.*;
import java.awt.*;

public class SearchPanel extends JPanel {
    public SearchPanel(EmployeeService service) {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JTextField id = new JTextField(20);
        JLabel label = new JLabel("Employee ID:");
        label.setLabelFor(id);
        JButton search = new JButton("Search");
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(label);
        toolbar.add(id);
        toolbar.add(search);
        add(toolbar, BorderLayout.NORTH);
        JTextArea details = new JTextArea("Enter an employee ID to search.");
        details.setEditable(false);
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        add(new JScrollPane(details), BorderLayout.CENTER);
        search.addActionListener(event -> {
            String query = id.getText().trim();
            if (query.isEmpty()) {
                details.setText("Please enter an employee ID.");
                return;
            }
            Employee employee = service.findById(query);
            if (employee == null) {
                details.setText("No employee found with ID: " + query);
            } else {
                details.setText(String.format("Employee ID : %s%nName        : %s%nDepartment  : %s%n"
                                + "Designation : %s%nBasic Salary: %.2f%n",
                        employee.getId(), employee.getName(), employee.getDepartment(),
                        employee.getDesignation(), employee.getBasicSalary()));
            }
            details.setCaretPosition(0);
        });
        id.addActionListener(event -> search.doClick());
    }
}
