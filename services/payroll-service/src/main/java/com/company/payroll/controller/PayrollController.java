package com.company.payroll.controller;

import com.company.payroll.model.SalaryRecord;
import com.company.payroll.service.PayrollService;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ================================================================
 * PAYROLL CONTROLLER — JWT-Protected Payroll API
 * ================================================================
 *
 * HOW JWT VERIFICATION WORKS IN THIS CONTROLLER:
 *
 * 1. Request arrives: GET /api/payroll/salaries
 *    Headers: Authorization: Bearer eyJhbGci...  (the JWT)
 *
 * 2. Spring Security JwtAuthenticationFilter intercepts:
 *    - Splits JWT into Header.Payload.Signature
 *    - Downloads Keycloak public key from JWKS (cached at startup)
 *    - Verifies: RSA_VERIFY(Header.Payload, Signature, publicKey) = true?
 *    - Checks: not expired (exp claim > now)
 *    - Checks: issuer matches (iss == "http://keycloak:8080/realms/company")
 *
 * 3. KeycloakJwtConverter extracts roles:
 *    JWT.realm_access.roles = ["PAYROLL", "USER"] → ROLE_PAYROLL, ROLE_USER
 *
 * 4. @PreAuthorize("hasAnyRole('PAYROLL', 'ADMIN')") checks:
 *    Does the authenticated user have ROLE_PAYROLL or ROLE_ADMIN? → Proceed
 *    If not → 403 Forbidden returned immediately
 *
 * 5. Controller method executes, @AuthenticationPrincipal Jwt jwt
 *    gives access to all JWT claims inline.
 */
@RestController
@RequestMapping("/api/payroll")
@Tag(name = "Payroll", description = "Payroll Service — protected by Keycloak JWT")
@SecurityRequirement(name = "bearerAuth")
public class PayrollController {

    private static final Logger log = LoggerFactory.getLogger(PayrollController.class);

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    /**
     * GET /api/payroll/my-salary
     * ─────────────────────────────────────────────────────
     * Any authenticated employee can see THEIR OWN salary.
     * The username is extracted from the JWT — no query param needed!
     *
     * JWT Claim used: preferred_username (= LDAP uid)
     * This ensures employees can ONLY see their own salary.
     *
     * Example:
     *   john.doe's JWT → preferred_username = "john.doe"
     *   → Returns only John's salary record
     *
     * curl -H "Authorization: Bearer <JWT>" http://localhost:8082/api/payroll/my-salary
     */
    @GetMapping("/my-salary")
    @Operation(summary = "My salary", description = "Get your own salary. Username extracted from JWT — no spoofing possible!")
    public ResponseEntity<?> getMySalary(@AuthenticationPrincipal Jwt jwt) {
        // Extract username from JWT — user can't forge this (JWT is signed by Keycloak)
        String username = jwt.getClaimAsString("preferred_username");
        log.info("Salary request from: {} (verified by JWT)", username);

        Optional<SalaryRecord> salary = payrollService.getSalaryByUsername(username);

        if (salary.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "message", "No salary record found",
                "username", username,
                "hint", "Contact HR to set up your payroll profile"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "data", salary.get(),
            "requestedBy", username,
            "jwtVerified", true,
            "message", "Salary data fetched using username from JWT claims"
        ));
    }

    /**
     * GET /api/payroll/salaries
     * ─────────────────────────────────────────────────────
     * Returns ALL employee salaries.
     * Restricted to: ROLE_PAYROLL or ROLE_ADMIN
     *
     * If a regular employee (ROLE_USER only) tries this:
     * → 403 Forbidden: "You don't have the required role"
     *
     * curl -H "Authorization: Bearer <PAYROLL_JWT>" http://localhost:8082/api/payroll/salaries
     */
    @GetMapping("/salaries")
    @PreAuthorize("hasAnyRole('PAYROLL', 'ADMIN')")
    @Operation(summary = "All salaries", description = "Admin/Payroll team only — list all employee salaries")
    public ResponseEntity<Map<String, Object>> getAllSalaries(@AuthenticationPrincipal Jwt jwt) {
        String requester = jwt.getClaimAsString("preferred_username");
        log.info("All salaries requested by: {} (PAYROLL/ADMIN role verified)", requester);

        List<SalaryRecord> salaries = payrollService.getAllSalaries();

        return ResponseEntity.ok(Map.of(
            "data", salaries,
            "count", salaries.size(),
            "requestedBy", requester,
            "accessGrantedVia", "ROLE_PAYROLL or ROLE_ADMIN in JWT",
            "timestamp", Instant.now().toString()
        ));
    }

    /**
     * GET /api/payroll/reports/summary
     * ─────────────────────────────────────────────────────
     * Payroll summary report.
     * Restricted to: ROLE_PAYROLL or ROLE_ADMIN
     */
    @GetMapping("/reports/summary")
    @PreAuthorize("hasAnyRole('PAYROLL', 'ADMIN')")
    @Operation(summary = "Payroll summary report", description = "Summary statistics for payroll period")
    public ResponseEntity<Map<String, Object>> getPayrollSummary(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
            "summary", payrollService.getSalarySummary(),
            "generatedBy", jwt.getClaimAsString("preferred_username"),
            "timestamp", Instant.now().toString()
        ));
    }

    /**
     * POST /api/payroll/process
     * ─────────────────────────────────────────────────────
     * Process/approve payroll run.
     * Restricted to: ROLE_ADMIN only
     *
     * Demonstrates that even ROLE_PAYROLL can't do this —
     * only ROLE_ADMIN can trigger salary payments.
     */
    @PostMapping("/process")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process payroll", description = "Admin only — trigger payroll processing run")
    public ResponseEntity<Map<String, Object>> processPayroll(
            @RequestBody(required = false) Map<String, String> request,
            @AuthenticationPrincipal Jwt jwt) {

        String approvedBy = jwt.getClaimAsString("preferred_username");
        String period = request != null ? request.getOrDefault("period", "2024-12") : "2024-12";

        log.info("Payroll processing approved by ADMIN: {} for period: {}", approvedBy, period);

        return ResponseEntity.ok(Map.of(
            "status", "PROCESSING_INITIATED",
            "period", period,
            "approvedBy", approvedBy,
            "approvedAt", Instant.now().toString(),
            "adminRoleVerified", true,
            "message", "Payroll run initiated. Bank transfers will process within 2 business days."
        ));
    }

    /**
     * GET /api/payroll/jwt-debug
     * ─────────────────────────────────────────────────────
     * Debug endpoint: shows exactly what JWT claims this service sees.
     * Remove in production!
     */
    @GetMapping("/jwt-debug")
    @Operation(summary = "JWT Debug", description = "Shows raw JWT claims received by payroll service")
    public ResponseEntity<Map<String, Object>> jwtDebug(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
            "service", "payroll-service",
            "jwtClaims", jwt.getClaims(),
            "issuedAt", jwt.getIssuedAt(),
            "expiresAt", jwt.getExpiresAt(),
            "issuer", jwt.getIssuer().toString(),
            "subject", jwt.getSubject(),
            "username", jwt.getClaimAsString("preferred_username"),
            "note", "This service validated JWT locally using Keycloak public key. No call to Keycloak was made for this request!"
        ));
    }
}
