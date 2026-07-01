package com.company.payroll.service;

import com.company.payroll.model.SalaryRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Payroll Service — Business Logic
 *
 * In production: would interact with a payroll database.
 * Here: uses in-memory data for demonstration.
 */
@Service
public class PayrollService {

    // Simulated payroll database (In production: use JPA/PostgreSQL)
    private final List<SalaryRecord> salaryDatabase = List.of(
        SalaryRecord.builder()
            .employeeId("EMP001")
            .username("john.doe")
            .firstName("John").lastName("Doe")
            .department("Finance")
            .baseSalary(new BigDecimal("95000.00"))
            .bonus(new BigDecimal("12000.00"))
            .totalCompensation(new BigDecimal("107000.00"))
            .currency("USD")
            .payPeriod("2024-12")
            .payDate(LocalDate.of(2024, 12, 31))
            .status("PAID")
            .bankAccount("****5678")
            .build(),

        SalaryRecord.builder()
            .employeeId("EMP002")
            .username("jane.smith")
            .firstName("Jane").lastName("Smith")
            .department("HR")
            .baseSalary(new BigDecimal("88000.00"))
            .bonus(new BigDecimal("9000.00"))
            .totalCompensation(new BigDecimal("97000.00"))
            .currency("USD")
            .payPeriod("2024-12")
            .payDate(LocalDate.of(2024, 12, 31))
            .status("PAID")
            .bankAccount("****9012")
            .build(),

        SalaryRecord.builder()
            .employeeId("EMP003")
            .username("bob.johnson")
            .firstName("Bob").lastName("Johnson")
            .department("Operations")
            .baseSalary(new BigDecimal("72000.00"))
            .bonus(new BigDecimal("6000.00"))
            .totalCompensation(new BigDecimal("78000.00"))
            .currency("USD")
            .payPeriod("2024-12")
            .payDate(LocalDate.of(2024, 12, 31))
            .status("PAID")
            .bankAccount("****3456")
            .build(),

        SalaryRecord.builder()
            .employeeId("EMP004")
            .username("alice.chen")
            .firstName("Alice").lastName("Chen")
            .department("Engineering")
            .baseSalary(new BigDecimal("115000.00"))
            .bonus(new BigDecimal("18000.00"))
            .totalCompensation(new BigDecimal("133000.00"))
            .currency("USD")
            .payPeriod("2024-12")
            .payDate(LocalDate.of(2024, 12, 31))
            .status("PAID")
            .bankAccount("****7890")
            .build()
    );

    /**
     * Get all salary records — only for PAYROLL/ADMIN roles.
     */
    public List<SalaryRecord> getAllSalaries() {
        return salaryDatabase;
    }

    /**
     * Get salary for a specific employee by their LDAP username.
     * Used for "my salary" endpoint — filtered by JWT username.
     *
     * @param username The JWT preferred_username claim (= LDAP uid)
     */
    public Optional<SalaryRecord> getSalaryByUsername(String username) {
        return salaryDatabase.stream()
            .filter(s -> s.getUsername().equals(username))
            .findFirst();
    }

    /**
     * Get salary summary statistics — for payroll reports.
     */
    public java.util.Map<String, Object> getSalarySummary() {
        BigDecimal total = salaryDatabase.stream()
            .map(SalaryRecord::getTotalCompensation)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return java.util.Map.of(
            "totalEmployees", salaryDatabase.size(),
            "totalPayroll", total,
            "averageSalary", total.divide(new BigDecimal(salaryDatabase.size()), 2, java.math.RoundingMode.HALF_UP),
            "currency", "USD",
            "payPeriod", "2024-12",
            "status", "PROCESSED"
        );
    }
}
