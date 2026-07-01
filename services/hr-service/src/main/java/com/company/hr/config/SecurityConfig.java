package com.company.hr.config;
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
 * HR Service Security:
 *   GET  /api/hr/employees/me     → Any authenticated employee (own profile)
 *   GET  /api/hr/employees        → ROLE_HR or ROLE_ADMIN
 *   POST /api/hr/employees        → ROLE_HR or ROLE_ADMIN (create new hire)
 *   PUT  /api/hr/employees/**     → ROLE_HR or ROLE_ADMIN
 *   DELETE /api/hr/employees/**   → ROLE_ADMIN only
 */
@Configuration @EnableWebSecurity @EnableMethodSecurity
public class SecurityConfig {
    private final KeycloakJwtConverter converter;
    public SecurityConfig(KeycloakJwtConverter converter) { this.converter = converter; }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health","/actuator/info","/swagger-ui/**","/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/hr/employees/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/hr/employees/**").hasAnyRole("HR","ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/hr/**").hasAnyRole("HR","ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/hr/**").hasAnyRole("HR","ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/hr/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((req,res,ex)->{
                    res.setContentType("application/json"); res.setStatus(401);
                    res.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"service\":\"hr-service\"}");
                }));
        return http.build();
    }
}
