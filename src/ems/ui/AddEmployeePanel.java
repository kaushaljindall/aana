package ems.ui;

import ems.Employee;
import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.function.Predicate;

public class AddEmployeePanel extends JPanel {
    private final JTextField id = new JTextField(24);
    private final JTextField name = new JTextField(24);
    private final JTextField department = new JTextField(24);
    private final JTextField designation = new JTextField(24);
    private final JTextField basicSalary = new JTextField(24);
    private final JButton save = new JButton("Save Employee");

    public AddEmployeePanel(Predicate<Employee> onSave) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.anchor = GridBagConstraints.WEST;
        JTextField[] fields = {id, name, department, designation, basicSalary};
        String[] labels = {"Employee ID", "Name", "Department", "Designation", "Monthly basic salary"};
        for (int row = 0; row < fields.length; row++) {
            c.gridx = 0;
            c.gridy = row;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            JLabel label = new JLabel(labels[row]);
            label.setLabelFor(fields[row]);
            form.add(label, c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            form.add(fields[row], c);
        }
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton clear = new JButton("Clear");
        buttons.add(save);
        buttons.add(clear);
        c.gridx = 1;
        c.gridy = fields.length;
        form.add(buttons, c);
        add(form, BorderLayout.NORTH);
        clear.addActionListener(event -> clear());
        save.addActionListener(event -> {
            try {
                for (JTextField field : fields) {
                    if (field.getText().trim().isEmpty()) {
                        field.requestFocusInWindow();
                        throw new IllegalArgumentException("Please fill in every field.");
                    }
                }
                double salary;
                try {
                    salary = Double.parseDouble(basicSalary.getText().trim());
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Enter a valid salary, for example 40000 or 40000.50.");
                }
                if (!Double.isFinite(salary) || salary <= 0 || !Double.isFinite(salary * 1.35)) {
                    throw new IllegalArgumentException("Salary must be a finite number greater than zero and within range.");
                }
                Employee employee = new Employee(id.getText().trim().toUpperCase(Locale.ROOT),
                        name.getText().trim(), department.getText().trim(), designation.getText().trim(), salary);
                if (onSave.test(employee)) {
                    clear();
                    JOptionPane.showMessageDialog(this, "Employee added and saved successfully.");
                }
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Check employee details", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    private void clear() {
        id.setText("");
        name.setText("");
        department.setText("");
        designation.setText("");
        basicSalary.setText("");
        id.requestFocusInWindow();
    }

    public void setSavingEnabled(boolean enabled) {
        save.setEnabled(enabled);
    }
}
