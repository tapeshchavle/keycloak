package keyclock.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /api/auth/token
 * Client sends username + password → we exchange with Keycloak → return JWT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    /**
     * Optional: grant_type (defaults to "password" / Resource Owner Password Credentials)
     * For production, prefer "authorization_code" with PKCE.
     */
    @JsonProperty("grant_type")
    @Builder.Default
    private String grantType = "password";
}
