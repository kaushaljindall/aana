package ems;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Embedded HTTP server providing REST API and serving the frontend.
 * No external dependencies - uses only JDK classes.
 */
public class Main {
    private static final int PORT = 8080;
    private static final EmployeeService service = new EmployeeService();
    private static final SalaryCalculator calculator = new SalaryCalculator();
    private static final CsvStorage storage = new CsvStorage("employees.csv");

    public static void main(String[] args) throws IOException {
        // Load existing data
        try {
            List<Employee> loaded = storage.load();
            for (Employee e : loaded) {
                service.addEmployee(e);
            }
            System.out.println("Loaded " + loaded.size() + " employees from CSV");
        } catch (IOException ex) {
            System.out.println("No existing data file, starting fresh: " + ex.getMessage());
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // API endpoints
        server.createContext("/api/employees", new EmployeesHandler());
        server.createContext("/api/slip", new SlipHandler());

        // Serve static files (frontend)
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null); // default executor
        server.start();

        System.out.println("Server started at http://localhost:" + PORT);
        System.out.println("Press Ctrl+C to stop");
    }

    static class EmployeesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if ("GET".equals(method)) {
                handleGetEmployees(exchange);
            } else if ("POST".equals(method)) {
                handleAddEmployee(exchange);
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }

        private void handleGetEmployees(HttpExchange exchange) throws IOException {
            List<Employee> employees = service.getAllEmployees();
            String json = employees.stream()
                .map(this::toJson)
                .collect(Collectors.joining(",", "[", "]"));
            sendResponse(exchange, 200, json);
        }

        private void handleAddEmployee(HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> data = parseJson(body);

            String id = data.get("id");
            String name = data.get("name");
            String department = data.get("department");
            String designation = data.get("designation");
            String basicStr = data.get("basicSalary");

            if (id == null || name == null || department == null || designation == null || basicStr == null ||
                id.isBlank() || name.isBlank() || department.isBlank() || designation.isBlank() || basicStr.isBlank()) {
                sendResponse(exchange, 400, "{\"error\":\"All fields are required\"}");
                return;
            }

            double basic;
            try {
                basic = Double.parseDouble(basicStr);
                if (basic <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "{\"error\":\"Basic salary must be a positive number\"}");
                return;
            }

            if (service.findById(id) != null) {
                sendResponse(exchange, 409, "{\"error\":\"Employee ID already exists\"}");
                return;
            }

            Employee emp = new Employee(id.trim().toUpperCase(), name.trim(), department.trim(), designation.trim(), basic);
            service.addEmployee(emp);

            // Persist to CSV
            try {
                storage.save(service.getAllEmployees());
            } catch (IOException e) {
                sendResponse(exchange, 500, "{\"error\":\"Failed to save data\"}");
                return;
            }

            sendResponse(exchange, 201, toJson(emp));
        }

        private String toJson(Employee e) {
            return String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"department\":\"%s\",\"designation\":\"%s\",\"basicSalary\":%.2f}",
                e.getId(), escapeJson(e.getName()), escapeJson(e.getDepartment()),
                escapeJson(e.getDesignation()), e.getBasicSalary()
            );
        }

        private String escapeJson(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        }

        private Map<String, String> parseJson(String json) {
            Map<String, String> map = new HashMap<>();
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1);
            }
            String[] pairs = json.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String pair : pairs) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "");
                    String value = kv[1].trim().replace("\"", "");
                    map.put(key, value);
                }
            }
            return map;
        }
    }

    static class SlipHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String id = null;
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("id=")) {
                        id = param.substring(3);
                        break;
                    }
                }
            }

            if (id == null || id.isBlank()) {
                sendResponse(exchange, 400, "{\"error\":\"Employee ID is required\"}");
                return;
            }

            Employee emp = service.findById(id);
            if (emp == null) {
                sendResponse(exchange, 404, "{\"error\":\"Employee not found\"}");
                return;
            }

            SalarySlip slip = calculator.generateSlip(emp);
            String json = slipToJson(slip);
            sendResponse(exchange, 200, json);
        }

        private String slipToJson(SalarySlip slip) {
            Employee e = slip.getEmployee();
            return String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"department\":\"%s\",\"designation\":\"%s\",\"basic\":%.2f,\"hra\":%.2f,\"da\":%.2f,\"ta\":%.2f,\"gross\":%.2f,\"pf\":%.2f,\"tax\":%.2f,\"deductions\":%.2f,\"net\":%.2f}",
                e.getId(), escapeJson(e.getName()), escapeJson(e.getDepartment()),
                escapeJson(e.getDesignation()),
                e.getBasicSalary(),
                slip.getHra(), slip.getDa(), slip.getTa(),
                slip.getGrossSalary(),
                slip.getProvidentFund(),
                slip.getIncomeTax(),
                slip.getTotalDeductions(),
                slip.getNetSalary()
            );
        }

        private String escapeJson(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Security: prevent directory traversal
            if (path.contains("..")) {
                sendResponse(exchange, 403, "Forbidden");
                return;
            }

            Path filePath = Paths.get("frontend", path.substring(1)).normalize();

            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                sendResponse(exchange, 404, "Not Found");
                return;
            }

            String contentType = getContentType(filePath.toString());
            byte[] content = Files.readAllBytes(filePath);

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
        }

        private String getContentType(String fileName) {
            if (fileName.endsWith(".html")) return "text/html; charset=utf-8";
            if (fileName.endsWith(".css")) return "text/css; charset=utf-8";
            if (fileName.endsWith(".js")) return "application/javascript; charset=utf-8";
            if (fileName.endsWith(".json")) return "application/json; charset=utf-8";
            return "application/octet-stream";
        }
    }

    private static void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}