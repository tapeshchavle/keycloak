package keyclock.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * ================================================================
 * SECURITY CONFIGURATION — Enterprise Keycloak JWT Resource Server
 * ================================================================
 *
 * HOW JWT VALIDATION WORKS HERE:
 * 1. Client sends: Authorization: Bearer <JWT_TOKEN>
 * 2. Spring Security intercepts the request via JwtAuthenticationFilter
 * 3. JwtDecoder fetches Keycloak's public key from JWKS endpoint (cached)
 * 4. Validates: RSA signature, expiry (exp claim), issuer (iss claim)
 * 5. KeycloakJwtConverter extracts roles from realm_access.roles claim
 * 6. Creates JwtAuthenticationToken with granted authorities
 * 7. Request proceeds to controller if authorized
 *
 * NO session, NO cookies, NO state — 100% stateless JWT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // Enables @PreAuthorize, @PostAuthorize
public class SecurityConfig {

    private final KeycloakJwtConverter keycloakJwtConverter;

    public SecurityConfig(KeycloakJwtConverter keycloakJwtConverter) {
        this.keycloakJwtConverter = keycloakJwtConverter;
    }

    /**
     * Main security filter chain.
     * - Stateless (no HTTP sessions)
     * - CSRF disabled (JWT-based, no cookies)
     * - JWT Bearer token validation via Keycloak JWKS
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── CSRF: Disabled for stateless REST API (JWT handles security) ──
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS: Allow frontend/other services ──────────────────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── Session: STATELESS — no HTTP sessions created ─────────────────
            // Each request MUST carry a valid JWT Bearer token
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ── Authorization Rules ───────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth

                // Public endpoints — no JWT needed
                .requestMatchers(HttpMethod.POST, "/api/auth/token").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()

                // Admin only — requires ROLE_ADMIN in JWT
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")

                // User info — any authenticated user
                .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()

                // All other requests — must be authenticated with valid JWT
                .anyRequest().authenticated()
            )

            // ── OAuth2 Resource Server: Validate JWT tokens ───────────────────
            // Spring Security calls Keycloak's JWKS endpoint to get public key
            // Then verifies the JWT signature, expiry, and issuer automatically
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    // Custom converter: extract Keycloak roles → Spring authorities
                    .jwtAuthenticationConverter(keycloakJwtConverter)
                )
                // Return 401 Unauthorized for missing/invalid tokens
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(401);
                    response.getWriter().write("""
                        {
                          "error": "UNAUTHORIZED",
                          "message": "Valid JWT Bearer token is required",
                          "hint": "POST /api/auth/token to get a token"
                        }
                        """);
                })
            );

        return http.build();
    }

    /**
     * CORS configuration — allow frontend apps and other services.
     * In production: restrict allowedOrigins to your actual domains.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",    // React frontend (dev)
            "http://localhost:5173",    // Vite frontend (dev)
            "http://localhost:8081",    // Main app
            "http://localhost:8082",    // Payroll service
            "http://localhost:8083",    // HR service
            "http://localhost:8084"     // Inventory service
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
