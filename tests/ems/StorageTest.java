package ems;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Dependency-free regression tests. Run with: java ems.StorageTest */
public class StorageTest {
    private static final String HEADER = "id,name,department,designation,basicSalary";
    private static final String VALID = "E1,Alice,Engineering,Developer,1000.0";

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("ems-storage-test-");
        try {
            testMissingFile(directory);
            testRoundTrip(directory);
            testInvalidLoads(directory);
            testFailedSaves(directory);
            testFailedMoveCleanup(directory);
            System.out.println("StorageTest: all tests passed");
        } finally {
            deleteTree(directory);
        }
    }

    private static void testMissingFile(Path directory) throws Exception {
        Path path = directory.resolve("missing.csv");
        CsvStorage storage = new CsvStorage(path.toString());
        check(storage.load().isEmpty(), "Missing file must produce an empty list");
        check(!Files.exists(path), "Loading must not create the file");
        check(storage.getAbsolutePath().equals(path.toFile().getAbsolutePath()), "Absolute path");
    }

    private static void testRoundTrip(Path directory) throws Exception {
        Path path = directory.resolve("roundtrip.csv");
        CsvStorage storage = new CsvStorage(path.toString());
        List<Employee> employees = Arrays.asList(
                new Employee("E,\"1", "Zoë, \"李\"", "R&D, 東京", "\"Lead\"", 1234.56),
                new Employee("E2", "  María  ", "Développement", "Engineer", Double.MIN_VALUE),
                new Employee("E3", "\"", ",", "Consultant", Double.MAX_VALUE));
        storage.save(employees);
        String csv = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        check(csv.startsWith(HEADER + System.lineSeparator()), "Exact header");
        check(csv.contains("\"Zoë, \"\"李\"\"\""), "CSV quote escaping and UTF-8");
        List<Employee> loaded = storage.load();
        check(loaded.size() == employees.size(), "Round-trip row count");
        for (int i = 0; i < employees.size(); i++) {
            Employee expected = employees.get(i);
            Employee actual = loaded.get(i);
            check(expected.getId().equals(actual.getId()), "Round-trip ID");
            check(expected.getName().equals(actual.getName()), "Round-trip name");
            check(expected.getDepartment().equals(actual.getDepartment()), "Round-trip department");
            check(expected.getDesignation().equals(actual.getDesignation()), "Round-trip designation");
            check(Double.compare(expected.getBasicSalary(), actual.getBasicSalary()) == 0,
                    "Round-trip salary");
        }
        storage.save(Collections.<Employee>emptyList());
        check(storage.load().isEmpty(), "Header-only file must be valid");
        Files.write(path, (HEADER + "\r\n" + VALID + "\r\n").getBytes(StandardCharsets.UTF_8));
        check(storage.load().size() == 1, "CRLF line endings");
        Files.write(path, (HEADER + "\n" + VALID).getBytes(StandardCharsets.UTF_8));
        check(storage.load().size() == 1, "Final newline is optional");
    }

    private static void testInvalidLoads(Path directory) throws Exception {
        final Path path = directory.resolve("invalid.csv");
        final CsvStorage storage = new CsvStorage(path.toString());
        String[] headers = { "", "wrong\n" + VALID, HEADER.toUpperCase() + "\n" + VALID,
                HEADER + ",extra\n" + VALID, " " + HEADER + "\n" + VALID,
                "\uFEFF" + HEADER + "\n" + VALID, VALID };
        for (String content : headers) {
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
            expectIOException(new CheckedAction() {
                public void run() throws Exception { storage.load(); }
            }, "header");
        }
        String[] rows = {
            "", "   ", "E1,Alice,Engineering,Developer", VALID + ",extra",
            ",Alice,Engineering,Developer,1", "E1,,Engineering,Developer,1",
            "E1,Alice,,Developer,1", "E1,Alice,Engineering,,1",
            "E1,Alice,Engineering,Developer,", "E1,\"\",Engineering,Developer,1",
            "E1,   ,Engineering,Developer,1", "E1,Alice,Engineering,Developer,invalid",
            "E1,Alice,Engineering,Developer,NaN", "E1,Alice,Engineering,Developer,Infinity",
            "E1,Alice,Engineering,Developer,-Infinity", "E1,Alice,Engineering,Developer,1e999",
            "E1,Alice,Engineering,Developer,0", "E1,Alice,Engineering,Developer,-0.0",
            "E1,Alice,Engineering,Developer,-1", "E1,Al\"ice,Engineering,Developer,1",
            "E1,\"Alice\"x,Engineering,Developer,1", "E1,\"Alice,Engineering,Developer,1",
            "E1,\"Alice\" ,Engineering,Developer,1"
        };
        for (String row : rows) {
            // A valid row first verifies errors are not silently skipped after partial loading.
            Files.write(path, (HEADER + "\n" + VALID + "\n" + row + "\n")
                    .getBytes(StandardCharsets.UTF_8));
            expectIOException(new CheckedAction() {
                public void run() throws Exception { storage.load(); }
            }, "row 3");
        }
        String[] duplicates = { "E1", "e1", " e1 " };
        for (String id : duplicates) {
            Files.write(path, (HEADER + "\n" + VALID + "\n" + id + ",Bob,Sales,Manager,2\n")
                    .getBytes(StandardCharsets.UTF_8));
            expectIOException(new CheckedAction() {
                public void run() throws Exception { storage.load(); }
            }, "duplicate");
        }
        for (String newline : new String[] { "\n", "\r", "\r\n" }) {
            Files.write(path, (HEADER + "\nE1,\"Alice" + newline
                    + "Smith\",Engineering,Developer,1\n").getBytes(StandardCharsets.UTF_8));
            expectIOException(new CheckedAction() {
                public void run() throws Exception { storage.load(); }
            }, "embedded newlines");
        }
        Files.write(path, new byte[] { (byte) 0xC3, (byte) 0x28 });
        expectIOException(new CheckedAction() {
            public void run() throws Exception { storage.load(); }
        }, null);
    }

    private static void testFailedSaves(Path directory) throws Exception {
        final Path path = directory.resolve("preserved.csv");
        final CsvStorage storage = new CsvStorage(path.toString());
        Employee valid = new Employee("E1", "Alice", "Engineering", "Developer", 1000);
        storage.save(Collections.singletonList(valid));
        byte[] original = Files.readAllBytes(path);
        Employee[] invalid = {
            null,
            new Employee(null, "Name", "Dept", "Role", 1),
            new Employee("", "Name", "Dept", "Role", 1),
            new Employee("E2", null, "Dept", "Role", 1),
            new Employee("E2", "   ", "Dept", "Role", 1),
            new Employee("E2", "Name", "", "Role", 1),
            new Employee("E2", "Name", "Dept", "", 1),
            new Employee("E\n2", "Name", "Dept", "Role", 1),
            new Employee("E2", "Na\rme", "Dept", "Role", 1),
            new Employee("E2", "Name", "De\npt", "Role", 1),
            new Employee("E2", "Name", "Dept", "Ro\r\nle", 1),
            new Employee("E2", "Name", "Dept", "Role", Double.NaN),
            new Employee("E2", "Name", "Dept", "Role", Double.POSITIVE_INFINITY),
            new Employee("E2", "Name", "Dept", "Role", Double.NEGATIVE_INFINITY),
            new Employee("E2", "Name", "Dept", "Role", 0),
            new Employee("E2", "Name", "Dept", "Role", -1),
            new Employee("e1", "Name", "Dept", "Role", 1),
            new Employee(" E1 ", "Name", "Dept", "Role", 1)
        };
        int entries = entryCount(directory);
        for (Employee employee : invalid) {
            final List<Employee> employees = Arrays.asList(valid, employee);
            expectIOException(new CheckedAction() {
                public void run() throws Exception { storage.save(employees); }
            }, "row 3");
            check(Arrays.equals(original, Files.readAllBytes(path)), "Failed save changed existing file");
            check(entryCount(directory) == entries, "Failed save leaked a temporary file");
        }
        expectIOException(new CheckedAction() {
            public void run() throws Exception { storage.save(null); }
        }, "null");
        check(Arrays.equals(original, Files.readAllBytes(path)), "Null list changed existing file");

        final Path absent = directory.resolve("failed-new.csv");
        expectIOException(new CheckedAction() {
            public void run() throws Exception {
                new CsvStorage(absent.toString()).save(Collections.singletonList(
                        new Employee("E2", "\n", "Dept", "Role", 1)));
            }
        }, "row 2");
        check(!Files.exists(absent), "Failed save created destination");
        check(entryCount(directory) == entries, "Failed new save leaked a temporary file");
    }

    private static void testFailedMoveCleanup(Path directory) throws Exception {
        final Path target = Files.createDirectory(directory.resolve("not-a-file"));
        Path marker = target.resolve("keep.txt");
        Files.write(marker, new byte[] { 42 });
        int entries = entryCount(directory);
        expectIOException(new CheckedAction() {
            public void run() throws Exception {
                new CsvStorage(target.toString()).save(Collections.<Employee>emptyList());
            }
        }, null);
        check(Files.readAllBytes(marker)[0] == 42, "Failed move changed destination");
        check(entryCount(directory) == entries, "Failed move leaked a temporary file");
    }

    private static void expectIOException(CheckedAction action, String messagePart) throws Exception {
        try {
            action.run();
        } catch (IOException ex) {
            check(ex.getMessage() != null && !ex.getMessage().isEmpty(), "Descriptive IOException");
            if (messagePart != null) {
                check(ex.getMessage().contains(messagePart), "Expected '" + messagePart
                        + "' in error, got: " + ex.getMessage());
            }
            return;
        }
        throw new AssertionError("Expected IOException");
    }

    private static int entryCount(Path directory) throws IOException {
        int count = 0;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path ignored : entries) {
                count++;
            }
        }
        return count;
    }

    private static void deleteTree(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteTree(entry);
                }
            }
        }
        Files.deleteIfExists(path);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private interface CheckedAction {
        void run() throws Exception;
    }
}
