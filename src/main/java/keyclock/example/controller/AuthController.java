package keyclock.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import keyclock.example.dto.TokenRequest;
import keyclock.example.dto.TokenResponse;
import keyclock.example.dto.UserInfoDto;
import keyclock.example.service.KeycloakTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ================================================================
 * AUTH CONTROLLER — Token Issuance & Session Management
 * ================================================================
 *
 * Endpoints:
 *   POST /api/auth/token      — Login: get JWT access + refresh tokens
 *   POST /api/auth/refresh    — Refresh: get new access token
 *   POST /api/auth/logout     — Logout: revoke session
 *   GET  /api/auth/me         — Current user info from JWT (no DB call)
 *   GET  /api/auth/validate   — Validate JWT and show decoded claims
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Token issuance, refresh, and user info via Keycloak")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final KeycloakTokenService tokenService;

    public AuthController(KeycloakTokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * LOGIN — Exchange username/password for JWT tokens.
     *
     * FLOW:
     *   Client → POST /api/auth/token {username, password}
     *         → This service → Keycloak /token endpoint
     *         → Keycloak → OpenLDAP (verify credentials + get groups)
     *         → Keycloak returns JWT
     *         → We return JWT to client
     *
     * The JWT contains: user identity + roles (from LDAP groups) + expiry
     *
     * Example request:
     *   POST /api/auth/token
     *   { "username": "john.doe", "password": "Test@1234" }
     *
     * Example response:
     *   { "access_token": "eyJhbGci...", "expires_in": 300, "refresh_token": "..." }
     */
    @PostMapping("/token")
    @Operation(summary = "Login", description = "Exchange username/password for JWT access token and refresh token")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody TokenRequest request) {
        log.info("Login request for user: {}", request.getUsername());
        TokenResponse tokenResponse = tokenService.getToken(request);
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * REFRESH — Use refresh token to get a new access token.
     *
     * Access tokens expire in 5 minutes (short for security).
     * When expired, the client sends the refresh_token to get a new one.
     * This does NOT require the user to enter their password again.
     *
     * Example request:
     *   POST /api/auth/refresh
     *   { "refresh_token": "eyJhbGci..." }
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Get a new access token using a refresh token")
    public ResponseEntity<TokenResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refresh_token");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        TokenResponse newTokens = tokenService.refreshToken(refreshToken);
        return ResponseEntity.ok(newTokens);
    }

    /**
     * LOGOUT — Revoke the session in Keycloak.
     *
     * After logout:
     * - refresh_token is revoked → can't get new access tokens
     * - Existing access_token still works until it expires (max 5 min)
     *
     * Example request:
     *   POST /api/auth/logout
     *   { "refresh_token": "eyJhbGci..." }
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoke refresh token and invalidate session")
    public ResponseEntity<Map<String, String>> logout(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refresh_token");
        tokenService.logout(refreshToken);
        return ResponseEntity.ok(Map.of(
            "message", "Logged out successfully",
            "hint", "Access token remains valid until expiry. Discard it on client side."
        ));
    }

    /**
     * CURRENT USER — Extract user info directly from JWT (no DB call needed!).
     *
     * The JWT contains everything about the user that was put in at login time.
     * We just decode it — no network call, no DB query. Pure JWT magic.
     *
     * Example: GET /api/auth/me
     * Headers: Authorization: Bearer eyJhbGci...
     *
     * @param jwt Automatically injected by Spring Security from the Bearer token
     */
    @GetMapping("/me")
    @Operation(summary = "Current user", description = "Get current authenticated user info from JWT claims")
    public ResponseEntity<UserInfoDto> currentUser(@AuthenticationPrincipal Jwt jwt) {
        // Extract all info directly from JWT claims (no network call!)
        @SuppressWarnings("unchecked")
        Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");
        @SuppressWarnings("unchecked")
        List<String> roles = realmAccess != null ? (List<String>) realmAccess.get("roles") : List.of();

        UserInfoDto userInfo = UserInfoDto.builder()
            .userId(jwt.getSubject())                              // UUID from Keycloak
            .username(jwt.getClaimAsString("preferred_username")) // From LDAP uid
            .email(jwt.getClaimAsString("email"))                 // From LDAP mail
            .firstName(jwt.getClaimAsString("given_name"))        // From LDAP givenName
            .lastName(jwt.getClaimAsString("family_name"))        // From LDAP sn
            .fullName(jwt.getClaimAsString("name"))               // Full display name
            .roles(roles)                                          // From LDAP groups → Keycloak roles
            .tokenExpiry(jwt.getExpiresAt() != null ?
                jwt.getExpiresAt().getEpochSecond() : null)        // When this JWT expires
            .sessionState(jwt.getClaimAsString("session_state"))  // Keycloak session ID
            .build();

        return ResponseEntity.ok(userInfo);
    }

    /**
     * VALIDATE — Show all decoded JWT claims. Useful for debugging.
     *
     * Example: GET /api/auth/validate
     * Headers: Authorization: Bearer eyJhbGci...
     *
     * Returns the raw JWT claims so you can see exactly what Keycloak put in.
     */
    @GetMapping("/validate")
    @Operation(summary = "Validate token", description = "Show all decoded JWT claims for debugging")
    public ResponseEntity<Map<String, Object>> validateToken(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of(
            "valid", true,
            "claims", jwt.getClaims(),
            "issued_at", jwt.getIssuedAt(),
            "expires_at", jwt.getExpiresAt(),
            "issuer", jwt.getIssuer().toString(),
            "subject", jwt.getSubject()
        ));
    }
}
