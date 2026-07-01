package com.company.payroll.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Salary Record — represents an employee's payroll data.
 * In production: fetched from a payroll database (PostgreSQL, MySQL, etc.)
 * Here: stored in-memory for demonstration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryRecord {

    private String employeeId;
    private String username;        // Matches LDAP uid / JWT preferred_username
    private String firstName;
    private String lastName;
    private String department;
    private BigDecimal baseSalary;
    private BigDecimal bonus;
    private BigDecimal totalCompensation;
    private String currency;
    private String payPeriod;       // e.g., "2024-12"
    private LocalDate payDate;
    private String status;          // PENDING, PROCESSED, PAID
    private String bankAccount;     // Masked: ****1234
}
