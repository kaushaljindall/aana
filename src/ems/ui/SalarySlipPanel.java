package ems.ui;

import ems.Employee;
import ems.EmployeeService;
import ems.SalaryCalculator;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

public class SalarySlipPanel extends JPanel {
    private final JTextField id = new JTextField(16);
    private final JTextArea preview = new JTextArea();
    private final JButton save = new JButton("Save as .txt");
    private final JButton print = new JButton("Print");

    public SalarySlipPanel(EmployeeService service, SalaryCalculator calculator) {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel("Employee ID:");
        label.setLabelFor(id);
        JButton generate = new JButton("Generate");
        toolbar.add(label);
        toolbar.add(id);
        toolbar.add(generate);
        toolbar.add(save);
        toolbar.add(print);
        add(toolbar, BorderLayout.NORTH);
        preview.setEditable(false);
        preview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        add(new JScrollPane(preview), BorderLayout.CENTER);
        invalidateSlip();
        id.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { invalidateSlip(); }
            public void removeUpdate(DocumentEvent event) { invalidateSlip(); }
            public void changedUpdate(DocumentEvent event) { invalidateSlip(); }
        });
        generate.addActionListener(event -> {
            invalidateSlip();
            String query = id.getText().trim();
            Employee employee = query.isEmpty() ? null : service.findById(query);
            if (employee == null) {
                JOptionPane.showMessageDialog(this,
                        query.isEmpty() ? "Please enter an employee ID." : "No employee found with that ID.",
                        "Salary slip", JOptionPane.WARNING_MESSAGE);
                return;
            }
            preview.setText(calculator.generateSlip(employee).format());
            preview.setCaretPosition(0);
            save.setEnabled(true);
            print.setEnabled(true);
        });
        id.addActionListener(event -> generate.doClick());
        save.addActionListener(event -> saveSlip());
        print.addActionListener(event -> {
            try {
                preview.print();
            } catch (PrinterException | SecurityException ex) {
                JOptionPane.showMessageDialog(this, "Could not print: " + ex.getMessage(),
                        "Print failed", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void invalidateSlip() {
        preview.setText("Enter an employee ID and click Generate to view a salary slip.");
        save.setEnabled(false);
        print.setEnabled(false);
    }

    private void saveSlip() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save salary slip");
        chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        chooser.setSelectedFile(new File("salary-slip.txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".txt")) {
            file = new File(file.getParentFile(), file.getName() + ".txt");
        }
        if (file.exists() && JOptionPane.showConfirmDialog(this,
                "Replace the existing file?\n" + file.getAbsolutePath(), "Confirm overwrite",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            Files.write(file.toPath(), preview.getText().getBytes(StandardCharsets.UTF_8));
            JOptionPane.showMessageDialog(this, "Salary slip saved to:\n" + file.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save salary slip: " + ex.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
