package ems;

public class SalarySlip {
    private final Employee employee;
    private final double hra;
    private final double da;
    private final double ta;
    private final double grossSalary;
    private final double providentFund;
    private final double incomeTax;
    private final double totalDeductions;
    private final double netSalary;

    public SalarySlip(Employee employee, double hra, double da, double ta,
                      double grossSalary, double providentFund, double incomeTax) {
        this.employee = employee;
        this.hra = hra;
        this.da = da;
        this.ta = ta;
        this.grossSalary = grossSalary;
        this.providentFund = providentFund;
        this.incomeTax = incomeTax;
        this.totalDeductions = providentFund + incomeTax;
        this.netSalary = grossSalary - totalDeductions;
    }

    public double getNetSalary() {
        return netSalary;
    }

    public double getGrossSalary() {
        return grossSalary;
    }

    public double getTotalDeductions() {
        return totalDeductions;
    }

    public double getHra() {
        return hra;
    }

    public double getDa() {
        return da;
    }

    public double getTa() {
        return ta;
    }

    public double getProvidentFund() {
        return providentFund;
    }

    public double getIncomeTax() {
        return incomeTax;
    }

    public Employee getEmployee() {
        return employee;
    }

    public String format() {
        String line = "==========================================\n";
        StringBuilder sb = new StringBuilder();
        sb.append(line);
        sb.append("              SALARY SLIP\n");
        sb.append(line);
        sb.append(String.format("Employee ID  : %s%n", employee.getId()));
        sb.append(String.format("Name         : %s%n", employee.getName()));
        sb.append(String.format("Department   : %s%n", employee.getDepartment()));
        sb.append(String.format("Designation  : %s%n", employee.getDesignation()));
        sb.append("------------------------------------------\n");
        sb.append("EARNINGS\n");
        sb.append(String.format("  Basic Salary       : %12.2f%n", employee.getBasicSalary()));
        sb.append(String.format("  HRA                : %12.2f%n", hra));
        sb.append(String.format("  DA                 : %12.2f%n", da));
        sb.append(String.format("  TA                 : %12.2f%n", ta));
        sb.append(String.format("  Gross Salary       : %12.2f%n", grossSalary));
        sb.append("------------------------------------------\n");
        sb.append("DEDUCTIONS\n");
        sb.append(String.format("  Provident Fund     : %12.2f%n", providentFund));
        sb.append(String.format("  Income Tax         : %12.2f%n", incomeTax));
        sb.append(String.format("  Total Deductions   : %12.2f%n", totalDeductions));
        sb.append(line);
        sb.append(String.format("NET SALARY           : %12.2f%n", netSalary));
        sb.append(line);
        return sb.toString();
    }
}
