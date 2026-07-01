package com.company.hr.model;
import lombok.*;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Employee {
    private String employeeId;
    private String username;         // Matches JWT preferred_username / LDAP uid
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private String position;
    private String managerId;
    private LocalDate joiningDate;
    private String employmentType;   // FULL_TIME, PART_TIME, CONTRACT
    private String status;           // ACTIVE, ON_LEAVE, TERMINATED
    private String location;
    private String phoneNumber;
}
