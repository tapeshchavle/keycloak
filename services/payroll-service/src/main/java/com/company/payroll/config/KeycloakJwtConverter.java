package com.company.payroll.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Payroll Service — Keycloak JWT Role Converter
 *
 * IDENTICAL pattern used across ALL services (payroll, hr, inventory).
 * Each service independently validates the JWT using Keycloak's public key.
 * NO network call to Keycloak per request — only the public key fetch at startup.
 *
 * JWT validation steps (all done locally):
 * 1. Decode Base64 header + payload
 * 2. Verify RSA signature using Keycloak's public key (from JWKS endpoint)
 * 3. Check exp claim (not expired)
 * 4. Check iss claim (matches our Keycloak realm)
 * 5. Extract realm_access.roles → Spring GrantedAuthority
 */
@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
            defaultConverter.convert(jwt).stream(),
            extractRealmRoles(jwt).stream()
        ).collect(Collectors.toSet());

        String principalName = jwt.getClaimAsString("preferred_username");
        if (principalName == null) principalName = jwt.getSubject();

        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) return Collections.emptyList();
        List<String> roles = (List<String>) realmAccess.get("roles");
        return roles.stream()
            .filter(r -> !r.startsWith("default-roles-") && !r.equals("offline_access") && !r.equals("uma_authorization"))
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            .collect(Collectors.toList());
    }
}
