package com.company.hr.service;
import com.company.hr.model.Employee;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service
public class HrService {

    private final List<Employee> employeeDb = List.of(
        Employee.builder().employeeId("EMP001").username("john.doe")
            .firstName("John").lastName("Doe").email("john.doe@company.com")
            .department("Finance").position("Payroll Manager").managerId("EMP005")
            .joiningDate(LocalDate.of(2020, 3, 15)).employmentType("FULL_TIME")
            .status("ACTIVE").location("New York").phoneNumber("+1-555-0002").build(),

        Employee.builder().employeeId("EMP002").username("jane.smith")
            .firstName("Jane").lastName("Smith").email("jane.smith@company.com")
            .department("HR").position("HR Manager").managerId("EMP005")
            .joiningDate(LocalDate.of(2019, 7, 1)).employmentType("FULL_TIME")
            .status("ACTIVE").location("New York").phoneNumber("+1-555-0003").build(),

        Employee.builder().employeeId("EMP003").username("bob.johnson")
            .firstName("Bob").lastName("Johnson").email("bob.johnson@company.com")
            .department("Operations").position("Inventory Manager").managerId("EMP005")
            .joiningDate(LocalDate.of(2021, 1, 10)).employmentType("FULL_TIME")
            .status("ACTIVE").location("Chicago").phoneNumber("+1-555-0004").build(),

        Employee.builder().employeeId("EMP004").username("alice.chen")
            .firstName("Alice").lastName("Chen").email("alice.chen@company.com")
            .department("Engineering").position("Software Engineer").managerId("EMP006")
            .joiningDate(LocalDate.of(2022, 6, 20)).employmentType("FULL_TIME")
            .status("ACTIVE").location("San Francisco").phoneNumber("+1-555-0005").build()
    );

    public List<Employee> getAllEmployees() { return employeeDb; }

    public Optional<Employee> getByUsername(String username) {
        return employeeDb.stream().filter(e -> e.getUsername().equals(username)).findFirst();
    }

    public Map<String, Object> getHrStats() {
        return Map.of(
            "totalEmployees", employeeDb.size(),
            "byDepartment", Map.of("Finance", 1, "HR", 1, "Operations", 1, "Engineering", 1),
            "activeEmployees", employeeDb.stream().filter(e -> "ACTIVE".equals(e.getStatus())).count(),
            "newHiresThisMonth", 0
        );
    }
}
