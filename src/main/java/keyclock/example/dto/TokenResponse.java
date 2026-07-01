package keyclock.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from POST /api/auth/token
 * Contains the JWT Access Token, Refresh Token, and metadata.
 *
 * The access_token is a JWT that contains:
 *   - User identity (sub, preferred_username, email)
 *   - Roles (realm_access.roles)
 *   - Expiry (exp claim) — 5 minutes by default
 *
 * The refresh_token is used to get a new access_token when it expires.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    /** The JWT Access Token — send this in Authorization: Bearer header */
    @JsonProperty("access_token")
    private String accessToken;

    /** Token type — always "Bearer" */
    @JsonProperty("token_type")
    private String tokenType;

    /** Seconds until access_token expires (default: 300 = 5 minutes) */
    @JsonProperty("expires_in")
    private Integer expiresIn;

    /** Refresh token — use this to get a new access_token */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /** Seconds until refresh_token expires */
    @JsonProperty("refresh_expires_in")
    private Integer refreshExpiresIn;

    /** OpenID Connect ID Token (contains user profile info) */
    @JsonProperty("id_token")
    private String idToken;

    /** Authorized scopes */
    private String scope;

    /** Session state identifier in Keycloak */
    @JsonProperty("session_state")
    private String sessionState;
}
