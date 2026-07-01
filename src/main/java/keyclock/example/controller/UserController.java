package keyclock.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ================================================================
 * USER CONTROLLER — User Management (Protected Endpoints)
 * ================================================================
 *
 * All endpoints require a valid JWT Bearer token.
 * Role-based access is enforced via @PreAuthorize annotations.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management - requires JWT Bearer token")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    /**
     * GET /api/users/me — Any authenticated user can access their own profile.
     *
     * JWT claim extraction — no database needed!
     * All user info is embedded in the JWT by Keycloak at login time.
     */
    @GetMapping("/me")
    @Operation(summary = "My profile", description = "Get the currently authenticated user's profile from JWT")
    public ResponseEntity<Map<String, Object>> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
            "userId", jwt.getSubject(),
            "username", jwt.getClaimAsString("preferred_username"),
            "email", jwt.getClaimAsString("email"),
            "firstName", jwt.getClaimAsString("given_name"),
            "lastName", jwt.getClaimAsString("family_name"),
            "tokenIssuedAt", jwt.getIssuedAt().toString(),
            "tokenExpiresAt", jwt.getExpiresAt().toString(),
            "message", "User profile extracted directly from JWT — no database call!"
        ));
    }

    /**
     * GET /api/users — Admin only. List all users.
     * Requires ROLE_ADMIN in JWT realm_access.roles
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users", description = "Admin only — list all users. Requires ROLE_ADMIN.")
    public ResponseEntity<Map<String, Object>> listUsers(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
            "requestedBy", jwt.getClaimAsString("preferred_username"),
            "users", List.of(
                Map.of("username", "john.doe", "email", "john.doe@company.com", "roles", List.of("USER", "PAYROLL")),
                Map.of("username", "jane.smith", "email", "jane.smith@company.com", "roles", List.of("USER", "HR")),
                Map.of("username", "bob.johnson", "email", "bob.johnson@company.com", "roles", List.of("USER", "INVENTORY")),
                Map.of("username", "alice.chen", "email", "alice.chen@company.com", "roles", List.of("USER"))
            ),
            "note", "In production: fetch from Keycloak Admin API or LDAP"
        ));
    }

    /**
     * GET /api/users/roles — Show roles the current user has.
     * Any authenticated user can see their own roles.
     */
    @GetMapping("/roles")
    @Operation(summary = "My roles", description = "Show roles assigned to the current JWT user")
    public ResponseEntity<Map<String, Object>> getMyRoles(@AuthenticationPrincipal Jwt jwt) {
        @SuppressWarnings("unchecked")
        Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");

        return ResponseEntity.ok(Map.of(
            "username", jwt.getClaimAsString("preferred_username"),
            "realmRoles", realmAccess != null ? realmAccess.get("roles") : List.of(),
            "resourceAccess", jwt.getClaims().getOrDefault("resource_access", Map.of()),
            "scopes", jwt.getClaimAsString("scope"),
            "explanation", "realm_access.roles are the Keycloak realm roles (mapped from LDAP groups)"
        ));
    }

    /**
     * GET /api/users/admin-info — Admin only protected endpoint demo.
     */
    @GetMapping("/admin-info")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin info", description = "Only accessible with ROLE_ADMIN in JWT")
    public ResponseEntity<Map<String, Object>> adminInfo(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
            "message", "You have ADMIN access!",
            "adminUser", jwt.getClaimAsString("preferred_username"),
            "sessionState", jwt.getClaimAsString("session_state"),
            "note", "This endpoint returns 403 Forbidden for non-admin JWT tokens"
        ));
    }
}
