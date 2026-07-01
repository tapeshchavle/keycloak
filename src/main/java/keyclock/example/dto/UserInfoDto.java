package keyclock.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * User information extracted from JWT claims.
 * This is what we return to the client about the authenticated user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDto {

    /** Unique user ID (UUID from Keycloak / LDAP entryUUID) */
    private String userId;

    /** LDAP uid attribute — used as the username */
    private String username;

    /** User's email from LDAP mail attribute */
    private String email;

    /** First name from LDAP givenName attribute */
    private String firstName;

    /** Last name from LDAP sn attribute */
    private String lastName;

    /** Full display name */
    private String fullName;

    /** Realm-level roles extracted from JWT */
    private List<String> roles;

    /** JWT token expiry time (epoch seconds) */
    private Long tokenExpiry;

    /** Keycloak session ID */
    private String sessionState;
}
