package ems.ui;

import ems.Employee;
import ems.EmployeeService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class EmployeeListPanel extends JPanel {
    private final EmployeeService service;
    private final JLabel count = new JLabel();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Name", "Department", "Designation", "Basic Salary"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 4 ? Double.class : String.class;
        }
    };

    public EmployeeListPanel(EmployeeService service) {
        this.service = service;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel toolbar = new JPanel(new BorderLayout());
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refresh());
        toolbar.add(count, BorderLayout.WEST);
        toolbar.add(refresh, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);
        refresh();
    }

    public void refresh() {
        model.setRowCount(0);
        for (Employee employee : service.getAllEmployees()) {
            model.addRow(new Object[]{employee.getId(), employee.getName(), employee.getDepartment(),
                    employee.getDesignation(), employee.getBasicSalary()});
        }
        count.setText("Employees: " + model.getRowCount());
    }
}
