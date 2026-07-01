package keyclock.example.config;

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
 * ================================================================
 * KEYCLOAK JWT CONVERTER
 * ================================================================
 *
 * Problem: Keycloak puts roles in a NON-STANDARD location inside JWT:
 *   {
 *     "realm_access": {
 *       "roles": ["ADMIN", "USER", "PAYROLL"]   <── here
 *     },
 *     "resource_access": {
 *       "spring-api": {
 *         "roles": ["api:read", "api:write"]     <── and here
 *       }
 *     }
 *   }
 *
 * Spring Security by default looks for roles in "scope" or "scp" claim.
 * This converter bridges that gap — it extracts Keycloak roles and
 * converts them to Spring Security GrantedAuthority objects.
 *
 * Result:
 *   "ADMIN"   → ROLE_ADMIN   (Spring convention prefix)
 *   "USER"    → ROLE_USER
 *   "PAYROLL" → ROLE_PAYROLL
 */
@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    // Default Spring Security scope converter (handles "scope" claim)
    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    // The claim in JWT that holds the Keycloak client ID
    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";

    // The preferred claim to use as the principal name
    private static final String PRINCIPAL_ATTRIBUTE = "preferred_username";

    /**
     * Converts a Jwt token into a Spring Security authentication token.
     * This is called by Spring Security for EVERY incoming request with a JWT.
     *
     * @param jwt The decoded JWT token from Keycloak
     * @return JwtAuthenticationToken with all extracted roles as authorities
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Combine standard scopes + Keycloak realm roles + client roles
        Collection<GrantedAuthority> authorities = Stream.concat(
            defaultConverter.convert(jwt).stream(),              // Standard scopes
            Stream.concat(
                extractRealmRoles(jwt).stream(),                 // Realm-level roles
                extractResourceRoles(jwt).stream()               // Client-level roles
            )
        ).collect(Collectors.toSet());

        // Use preferred_username as the principal name (from LDAP uid attribute)
        String principalName = jwt.getClaimAsString(PRINCIPAL_ATTRIBUTE);
        if (principalName == null) {
            principalName = jwt.getSubject(); // Fallback to sub claim (UUID)
        }

        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    /**
     * Extract realm-level roles from: jwt.realm_access.roles[]
     *
     * These are the global roles assigned to the user in the Keycloak realm.
     * Example JWT claim:
     *   "realm_access": { "roles": ["ADMIN", "USER"] }
     *
     * @param jwt The decoded JWT
     * @return Collection of GrantedAuthority with ROLE_ prefix
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS);
        if (realmAccess == null || !realmAccess.containsKey(ROLES)) {
            return Collections.emptyList();
        }

        List<String> roles = (List<String>) realmAccess.get(ROLES);
        return roles.stream()
            .filter(role -> !role.startsWith("default-roles-"))  // Skip Keycloak internal roles
            .filter(role -> !role.equals("offline_access"))
            .filter(role -> !role.equals("uma_authorization"))
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            .collect(Collectors.toList());
    }

    /**
     * Extract client-level roles from: jwt.resource_access.<clientId>.roles[]
     *
     * These are roles specific to a particular client (service).
     * Example JWT claim:
     *   "resource_access": {
     *     "spring-api": { "roles": ["api:read", "api:write"] },
     *     "payroll-service": { "roles": ["payroll:read"] }
     *   }
     *
     * @param jwt The decoded JWT
     * @return Collection of GrantedAuthority with ROLE_ prefix
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS);
        if (resourceAccess == null) {
            return Collections.emptyList();
        }

        return resourceAccess.entrySet().stream()
            .filter(entry -> entry.getValue() instanceof Map)
            .flatMap(entry -> {
                Map<String, Object> clientRoles = (Map<String, Object>) entry.getValue();
                Object rolesObj = clientRoles.get(ROLES);
                if (!(rolesObj instanceof List)) return Stream.empty();
                List<String> roles = (List<String>) rolesObj;
                return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase().replace(":", "_")));
            })
            .collect(Collectors.toList());
    }

    /**
     * Helper method to inspect what roles are in a JWT.
     * Useful for debugging.
     */
    public static Map<String, Object> extractAllClaims(Jwt jwt) {
        return new HashMap<>(jwt.getClaims());
    }
}
