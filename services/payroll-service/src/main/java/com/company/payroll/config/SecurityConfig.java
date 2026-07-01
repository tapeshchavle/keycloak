package com.company.payroll.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ================================================================
 * PAYROLL SERVICE — Security Configuration
 * ================================================================
 *
 * This service ONLY validates JWT tokens — it never talks to LDAP or Keycloak
 * for user verification. It just checks the token signature + roles.
 *
 * Access Rules:
 *   GET  /api/payroll/my-salary    → Any authenticated employee (own salary)
 *   GET  /api/payroll/salaries     → ROLE_PAYROLL or ROLE_ADMIN
 *   POST /api/payroll/process      → ROLE_ADMIN only
 *   GET  /api/payroll/reports/**   → ROLE_PAYROLL or ROLE_ADMIN
 *
 * JWT validation is done via Spring Security's OAuth2 Resource Server.
 * The JWKS public key is fetched from:
 *   {KEYCLOAK_ISSUER_URI}/protocol/openid-connect/certs
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final KeycloakJwtConverter keycloakJwtConverter;

    public SecurityConfig(KeycloakJwtConverter keycloakJwtConverter) {
        this.keycloakJwtConverter = keycloakJwtConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public: health checks
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()

                // Own salary: any authenticated employee
                .requestMatchers(HttpMethod.GET, "/api/payroll/my-salary").authenticated()

                // Payroll team + admin: see all salaries and reports
                .requestMatchers(HttpMethod.GET, "/api/payroll/salaries").hasAnyRole("PAYROLL", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/payroll/reports/**").hasAnyRole("PAYROLL", "ADMIN")

                // Admin only: process salary, create/update records
                .requestMatchers(HttpMethod.POST, "/api/payroll/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/payroll/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/payroll/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter))
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setContentType("application/json");
                    res.setStatus(401);
                    res.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"service\":\"payroll-service\",\"message\":\"Valid JWT Bearer token required\"}");
                })
            );
        return http.build();
    }
}
