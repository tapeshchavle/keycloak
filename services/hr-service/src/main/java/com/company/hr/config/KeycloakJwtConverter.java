package com.company.hr.config;
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

/** HR Service — Same JWT converter pattern as all other services. One shared token, all services. */
@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
            defaultConverter.convert(jwt).stream(), extractRealmRoles(jwt).stream()
        ).collect(Collectors.toSet());
        String name = jwt.getClaimAsString("preferred_username");
        return new JwtAuthenticationToken(jwt, authorities, name != null ? name : jwt.getSubject());
    }
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> ra = jwt.getClaimAsMap("realm_access");
        if (ra == null || !ra.containsKey("roles")) return Collections.emptyList();
        return ((List<String>) ra.get("roles")).stream()
            .filter(r -> !r.startsWith("default-roles-") && !r.equals("offline_access") && !r.equals("uma_authorization"))
            .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
            .collect(Collectors.toList());
    }
}
