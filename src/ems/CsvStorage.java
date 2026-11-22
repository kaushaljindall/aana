package ems;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** UTF-8 CSV storage with strict, single-line records and replacement-safe saves. */
public class CsvStorage {

    private static final String HEADER =
        "id,name,department,designation,basicSalary";
    private static final String[] FIELD_NAMES = {
        "id",
        "name",
        "department",
        "designation",
        "basicSalary",
    };
    private final File file;

    public CsvStorage(String path) {
        this.file = new File(path);
    }

    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    /** Returns an empty list only if the file does not exist yet. */
    public List<Employee> load() throws IOException {
        List<Employee> result = new ArrayList<Employee>();
        BufferedReader opened;
        try {
            opened = Files.newBufferedReader(
                file.toPath(),
                StandardCharsets.UTF_8
            );
        } catch (NoSuchFileException ex) {
            return result;
        }
        try (BufferedReader reader = opened) {
            if (!HEADER.equals(reader.readLine())) {
                throw new IOException(
                    "Invalid CSV header in " +
                        getAbsolutePath() +
                        "; expected exactly: " +
                        HEADER
                );
            }
            Set<String> ids = new TreeSet<String>(
                String.CASE_INSENSITIVE_ORDER
            );
            String line;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                List<String> fields = parseLine(line, row);
                if (fields.size() != FIELD_NAMES.length) {
                    throw invalid(
                        row,
                        "expected 5 fields but found " + fields.size()
                    );
                }
                double salary;
                try {
                    salary = Double.parseDouble(fields.get(4));
                } catch (NumberFormatException ex) {
                    throw new IOException(
                        "CSV row " + row + ": invalid basicSalary",
                        ex
                    );
                }
                Employee employee = new Employee(
                    fields.get(0),
                    fields.get(1),
                    fields.get(2),
                    fields.get(3),
                    salary
                );
                validate(employee, ids, row);
                result.add(employee);
            }
        }
        return result;
    }

    public void save(List<Employee> employees) throws IOException {
        if (employees == null) {
            throw new IOException("Cannot save a null employee list");
        }
        Path destination = file.toPath().toAbsolutePath();
        Path temporary = Files.createTempFile(
            destination.getParent(),
            ".ems-csv-",
            ".tmp"
        );
        try {
            try (
                BufferedWriter writer = Files.newBufferedWriter(
                    temporary,
                    StandardCharsets.UTF_8
                )
            ) {
                writer.write(HEADER);
                writer.newLine();
                Set<String> ids = new TreeSet<String>(
                    String.CASE_INSENSITIVE_ORDER
                );
                int row = 1;
                for (Employee employee : employees) {
                    validate(employee, ids, ++row);
                    writer.write(
                        escape(employee.getId()) +
                            "," +
                            escape(employee.getName()) +
                            "," +
                            escape(employee.getDepartment()) +
                            "," +
                            escape(employee.getDesignation()) +
                            "," +
                            Double.toString(employee.getBasicSalary())
                    );
                    writer.newLine();
                }
            }
            try {
                Files.move(
                    temporary,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException ex) {
                // Some file systems cannot atomically replace a sibling file.
                Files.move(
                    temporary,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (IOException | RuntimeException | Error ex) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupFailure) {
                ex.addSuppressed(cleanupFailure);
            }
            throw ex;
        }
    }

    private static void validate(
        Employee employee,
        Set<String> ids,
        int row
    ) throws IOException {
        if (employee == null) {
            throw invalid(row, "employee must not be null");
        }
        String[] values = {
            employee.getId(),
            employee.getName(),
            employee.getDepartment(),
            employee.getDesignation(),
        };
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            if (value == null || value.trim().isEmpty()) {
                throw invalid(row, FIELD_NAMES[i] + " must not be empty");
            }
            if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
                throw invalid(
                    row,
                    FIELD_NAMES[i] +
                        " contains an embedded newline; only single-line fields are supported"
                );
            }
        }
        double salary = employee.getBasicSalary();
        if (Double.isNaN(salary) || Double.isInfinite(salary) || salary <= 0) {
            throw invalid(
                row,
                "basicSalary must be finite and greater than zero"
            );
        }
        // Match EmployeeService's treatment of surrounding whitespace in IDs.
        if (!ids.add(employee.getId().trim())) {
            throw invalid(
                row,
                "duplicate employee ID (case-insensitive): " + employee.getId()
            );
        }
    }

    private static IOException invalid(int row, String message) {
        return new IOException("CSV row " + row + ": " + message);
    }

    private static String escape(String value) {
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static List<String> parseLine(
        String line,
        int row
    ) throws IOException {
        List<String> fields = new ArrayList<String>();
        int index = 0;
        while (true) {
            StringBuilder field = new StringBuilder();
            if (index < line.length() && line.charAt(index) == '"') {
                index++;
                boolean closed = false;
                while (index < line.length()) {
                    char c = line.charAt(index++);
                    if (c != '"') {
                        field.append(c);
                    } else if (
                        index < line.length() && line.charAt(index) == '"'
                    ) {
                        field.append('"');
                        index++;
                    } else {
                        closed = true;
                        break;
                    }
                }
                if (!closed) {
                    throw invalid(
                        row,
                        "unterminated quoted field; embedded newlines are not supported"
                    );
                }
                if (index < line.length() && line.charAt(index) != ',') {
                    throw invalid(
                        row,
                        "unexpected character after closing quote"
                    );
                }
            } else {
                while (index < line.length() && line.charAt(index) != ',') {
                    char c = line.charAt(index++);
                    if (c == '"') {
                        throw invalid(row, "quote inside an unquoted field");
                    }
                    field.append(c);
                }
            }
            fields.add(field.toString());
            if (index == line.length()) {
                return fields;
            }
            index++; // Consume the comma, retaining any trailing empty field.
        }
    }
}
