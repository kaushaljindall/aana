package ems;

public class SalaryCalculator {
    // Allowances (percentage of basic salary)
    private static final double HRA_RATE = 0.20;
    private static final double DA_RATE = 0.10;
    private static final double TA_RATE = 0.05;

    // Deductions
    private static final double PF_RATE = 0.12; // percentage of basic salary

    // Income tax slabs (applied on gross salary)
    private static final double SLAB_1_LIMIT = 25000;
    private static final double SLAB_2_LIMIT = 50000;
    private static final double SLAB_2_RATE = 0.05;
    private static final double SLAB_3_RATE = 0.10;

    public SalarySlip generateSlip(Employee employee) {
        double basic = employee.getBasicSalary();

        double hra = basic * HRA_RATE;
        double da = basic * DA_RATE;
        double ta = basic * TA_RATE;
        double gross = basic + hra + da + ta;

        double pf = basic * PF_RATE;
        double tax = calculateTax(gross);

        return new SalarySlip(employee, hra, da, ta, gross, pf, tax);
    }

    private double calculateTax(double gross) {
        if (gross <= SLAB_1_LIMIT) {
            return 0;
        } else if (gross <= SLAB_2_LIMIT) {
            return gross * SLAB_2_RATE;
        } else {
            return gross * SLAB_3_RATE;
        }
    }
}
