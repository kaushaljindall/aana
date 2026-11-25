package ems.ui;

import ems.CsvStorage;
import ems.Employee;
import ems.EmployeeService;
import ems.SalaryCalculator;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {
    private final EmployeeService service = new EmployeeService();
    private final CsvStorage storage = new CsvStorage("employees.csv");
    private final JLabel status = new JLabel(" ");
    private EmployeeListPanel employeeList;
    private AddEmployeePanel addPanel;

    public MainFrame() {
        super("Employee Salary Management System");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // Entry Screen
        JPanel entryPanel = new JPanel(new GridBagLayout());
        JLabel welcome = new JLabel("Welcome to EMS");
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 32f));
        welcome.setForeground(new Color(0x2E, 0x7D, 0x32));
        JButton start = new JButton("Get Started");
        start.addActionListener(e -> showMainArea());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(20, 20, 20, 20);
        entryPanel.add(welcome, gbc);
        gbc.gridy = 1;
        entryPanel.add(start, gbc);

        setContentPane(entryPanel);
        setSize(500, 400);
        setLocationRelativeTo(null);
    }

    private void showMainArea() {
        JPanel mainArea = new JPanel(new BorderLayout(12, 12));
        JLabel title = new JLabel("Employee Salary Management System");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(new Color(0x2E, 0x7D, 0x32));
        title.setBorder(BorderFactory.createEmptyBorder(18, 18, 6, 18));
        mainArea.add(title, BorderLayout.NORTH);

        employeeList = new EmployeeListPanel(service);
        addPanel = new AddEmployeePanel(this::saveEmployee);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Add Employee", addPanel);
        tabs.addTab("Employees", employeeList);
        tabs.addTab("Search", new SearchPanel(service));
        tabs.addTab("Salary Slip", new SalarySlipPanel(service, new SalaryCalculator()));
        tabs.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        tabs.addChangeListener(event -> employeeList.refresh());
        mainArea.add(tabs, BorderLayout.CENTER);

        JPanel footer = new JPanel(new GridLayout(0, 1, 0, 4));
        footer.setBorder(BorderFactory.createEmptyBorder(8, 18, 12, 18));
        footer.add(status);
        footer.add(new JLabel("Data file: " + storage.getAbsolutePath()));
        mainArea.add(footer, BorderLayout.SOUTH);

        setContentPane(mainArea);
        revalidate();
        setSize(900, 680);
        setLocationRelativeTo(null);
    }

    private boolean saveEmployee(Employee employee) {
        if (service.findById(employee.getId()) != null) {
            JOptionPane.showMessageDialog(this, "An employee with this ID already exists.",
                    "Duplicate ID", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        List<Employee> updated = new ArrayList<Employee>(service.getAllEmployees());
        updated.add(employee);
        try {
            // Persist first so a failed save never leaves an unsaved employee in the UI.
            storage.save(updated);
            service.addEmployee(employee);
            employeeList.refresh();
            status.setText("Saved employee " + employee.getId() + ". Total employees: " + updated.size());
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Employee was not added because saving failed.\n" + ex.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void loadEmployees() {
        try {
            List<Employee> loaded = storage.load();
            for (Employee employee : loaded) {
                service.addEmployee(employee);
            }
            employeeList.refresh();
            status.setText("Ready. Employees loaded: " + loaded.size());
        } catch (IOException ex) {
            // Prevent an unreadable file from being overwritten by a fresh, empty session.
            addPanel.setSavingEnabled(false);
            status.setText("Loading failed. Adding employees is disabled to protect the data file.");
            JOptionPane.showMessageDialog(this,
                    "Could not load employees. The file has not been changed.\n"
                            + ex.getMessage() + "\nFix or back up the file, then restart the application.",
                    "Load failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            applyGreenTheme();
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // The default Swing theme remains usable if the system theme is unavailable.
            }
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
            frame.loadEmployees();
        });
    }

    /** White background with green accents: applied via UI defaults so every panel inherits it. */
    private static void applyGreenTheme() {
        Color white = Color.WHITE;
        Color green = new Color(0x2E, 0x7D, 0x32);
        Color lightGreen = new Color(0xE8, 0xF5, 0xE9);
        UIManager.put("Panel.background", white);
        UIManager.put("OptionPane.background", white);
        UIManager.put("Label.foreground", new Color(0x21, 0x21, 0x21));
        UIManager.put("Button.background", lightGreen);
        UIManager.put("Button.foreground", green);
        UIManager.put("Button.rollover", true);
        UIManager.put("TextField.background", white);
        UIManager.put("TextArea.background", white);
        UIManager.put("Table.background", white);
        UIManager.put("Table.selectionBackground", lightGreen);
        UIManager.put("Table.selectionForeground", green);
        UIManager.put("TableHeader.background", lightGreen);
        UIManager.put("TableHeader.foreground", green);
        UIManager.put("TabbedPane.background", white);
        UIManager.put("TabbedPane.foreground", green);
        UIManager.put("TabbedPane.selected", lightGreen);
    }
}
