package com.company.inventory.config;
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
 * Inventory Service Access:
 *   GET  /api/inventory/products     → Any authenticated USER (read catalog)
 *   GET  /api/inventory/stock        → ROLE_INVENTORY or ROLE_ADMIN
 *   POST /api/inventory/products     → ROLE_INVENTORY or ROLE_ADMIN
 *   PUT  /api/inventory/products/**  → ROLE_INVENTORY or ROLE_ADMIN
 *   DELETE /api/inventory/**         → ROLE_ADMIN only
 */
@Configuration @EnableWebSecurity @EnableMethodSecurity
public class SecurityConfig {
    private final KeycloakJwtConverter converter;
    public SecurityConfig(KeycloakJwtConverter converter) { this.converter = converter; }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a
                .requestMatchers("/actuator/health","/actuator/info","/swagger-ui/**","/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/inventory/products").hasAnyRole("USER","INVENTORY","ADMIN")
                .requestMatchers(HttpMethod.GET,"/api/inventory/stock/**").hasAnyRole("INVENTORY","ADMIN")
                .requestMatchers(HttpMethod.POST,"/api/inventory/**").hasAnyRole("INVENTORY","ADMIN")
                .requestMatchers(HttpMethod.PUT,"/api/inventory/**").hasAnyRole("INVENTORY","ADMIN")
                .requestMatchers(HttpMethod.DELETE,"/api/inventory/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((req,res,ex)->{
                    res.setContentType("application/json"); res.setStatus(401);
                    res.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"service\":\"inventory-service\"}");
                }));
        return http.build();
    }
}
