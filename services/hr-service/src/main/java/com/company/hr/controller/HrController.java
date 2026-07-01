package com.company.hr.controller;

import com.company.hr.model.Employee;
import com.company.hr.service.HrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * ================================================================
 * HR CONTROLLER — JWT-Protected HR API
 * ================================================================
 *
 * JWT flow in HR service (SAME as payroll, same JWT token works!):
 *
 * User logs in ONCE → gets JWT from Keycloak
 * Same JWT token is used for:
 *   - Payroll API (port 8082)  → validates same JWT locally
 *   - HR API (port 8083)       → validates same JWT locally ← HERE
 *   - Inventory API (port 8084)→ validates same JWT locally
 *
 * This is SSO — Single Sign-On at the API level!
 */
@RestController
@RequestMapping("/api/hr")
@Tag(name = "HR", description = "HR Service — protected by Keycloak JWT")
@SecurityRequirement(name = "bearerAuth")
public class HrController {

    private static final Logger log = LoggerFactory.getLogger(HrController.class);
    private final HrService hrService;

    public HrController(HrService hrService) { this.hrService = hrService; }

    /**
     * GET /api/hr/employees/me — Get own employee profile from JWT username.
     * Any authenticated employee can access their own HR profile.
     */
    @GetMapping("/employees/me")
    @Operation(summary = "My HR profile", description = "Get own employee profile. Username from JWT claim.")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        log.info("HR profile requested by: {}", username);

        Optional<Employee> emp = hrService.getByUsername(username);
        return emp.map(e -> ResponseEntity.ok((Object) Map.of(
            "employee", e,
            "requestedBy", username,
            "note", "Profile fetched using JWT preferred_username claim"
        ))).orElse(ResponseEntity.ok(Map.of("message", "Employee profile not found", "username", username)));
    }

    /**
     * GET /api/hr/employees — All employees.
     * Requires ROLE_HR or ROLE_ADMIN.
     */
    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    @Operation(summary = "All employees", description = "HR team/Admin only — list all employees")
    public ResponseEntity<Map<String, Object>> getAllEmployees(@AuthenticationPrincipal Jwt jwt) {
        log.info("All employees listed by: {} (HR/ADMIN role verified)", jwt.getClaimAsString("preferred_username"));
        return ResponseEntity.ok(Map.of(
            "employees", hrService.getAllEmployees(),
            "count", hrService.getAllEmployees().size(),
            "requestedBy", jwt.getClaimAsString("preferred_username"),
            "accessGrantedVia", "ROLE_HR or ROLE_ADMIN in JWT"
        ));
    }

    /**
     * GET /api/hr/stats — HR statistics dashboard.
     * Requires ROLE_HR or ROLE_ADMIN.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    @Operation(summary = "HR statistics", description = "Employee statistics and analytics")
    public ResponseEntity<Map<String, Object>> getHrStats(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
            "stats", hrService.getHrStats(),
            "generatedBy", jwt.getClaimAsString("preferred_username"),
            "timestamp", Instant.now().toString()
        ));
    }

    /**
     * POST /api/hr/employees — Create new employee.
     * Requires ROLE_HR or ROLE_ADMIN.
     */
    @PostMapping("/employees")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    @Operation(summary = "Create employee", description = "HR team/Admin — onboard a new employee")
    public ResponseEntity<Map<String, Object>> createEmployee(
            @RequestBody Map<String, String> employeeData,
            @AuthenticationPrincipal Jwt jwt) {
        String createdBy = jwt.getClaimAsString("preferred_username");
        log.info("New employee created by: {} (HR/ADMIN role)", createdBy);
        return ResponseEntity.ok(Map.of(
            "message", "Employee created successfully",
            "data", employeeData,
            "createdBy", createdBy,
            "timestamp", Instant.now().toString(),
            "note", "In production: save to HR database and sync to LDAP"
        ));
    }

    /**
     * DELETE /api/hr/employees/{id} — Terminate employee.
     * ADMIN ONLY — even HR managers can't delete (offboarding needs admin approval).
     */
    @DeleteMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Terminate employee", description = "Admin only — employee offboarding")
    public ResponseEntity<Map<String, Object>> terminateEmployee(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
            "message", "Employee termination processed",
            "employeeId", id,
            "approvedBy", jwt.getClaimAsString("preferred_username"),
            "timestamp", Instant.now().toString(),
            "note", "ROLE_ADMIN required — HR managers cannot terminate without admin approval"
        ));
    }
}
