package keyclock.example.service;

import keyclock.example.dto.TokenRequest;
import keyclock.example.dto.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * ================================================================
 * KEYCLOAK TOKEN SERVICE
 * ================================================================
 *
 * This service acts as a proxy between clients and Keycloak's
 * Token Endpoint. It handles:
 *
 * 1. PASSWORD grant (Resource Owner Password Credentials)
 *    Client → Our API → Keycloak → JWT Token
 *
 * 2. REFRESH_TOKEN grant
 *    Client sends refresh_token → Our API → Keycloak → New JWT Token
 *
 * 3. LOGOUT
 *    Client sends refresh_token → Our API → Keycloak revokes session
 *
 * WHY PROXY? So the client_secret never needs to be in the frontend.
 * The client only sends username/password or refresh_token.
 * We add client_id + client_secret on the server side.
 */
@Service
public class KeycloakTokenService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakTokenService.class);

    @Value("${keycloak.token-endpoint}")
    private String tokenEndpoint;

    @Value("${keycloak.logout-endpoint}")
    private String logoutEndpoint;

    @Value("${keycloak.client.id}")
    private String clientId;

    @Value("${keycloak.client.secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Exchange username/password for JWT tokens.
     *
     * FLOW:
     * 1. Receive username + password from client
     * 2. Add client_id + client_secret (from server config, not exposed to client)
     * 3. POST to Keycloak /token endpoint
     * 4. Keycloak verifies against OpenLDAP
     * 5. Returns JWT access_token + refresh_token
     *
     * @param request Contains username + password
     * @return TokenResponse with access_token, refresh_token, expiry info
     */
    public TokenResponse getToken(TokenRequest request) {
        log.debug("Requesting token for user: {}", request.getUsername());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Build the form data for Keycloak Token Endpoint
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");           // Resource Owner Password Grant
        body.add("client_id", clientId);              // Added server-side (never in client)
        body.add("client_secret", clientSecret);       // Added server-side (never in client)
        body.add("username", request.getUsername());
        body.add("password", request.getPassword());
        body.add("scope", "openid profile email roles"); // Request user info + roles in token

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                tokenEndpoint,
                entity,
                TokenResponse.class
            );

            log.info("Token issued successfully for user: {}", request.getUsername());
            return response.getBody();

        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Authentication failed for user: {}", request.getUsername());
            throw new RuntimeException("Invalid username or password");
        } catch (HttpClientErrorException e) {
            log.error("Token request failed: {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Authentication service error: " + e.getMessage());
        }
    }

    /**
     * Use a refresh_token to get a new access_token.
     *
     * ACCESS TOKENS expire in 5 minutes (short for security).
     * The client uses the refresh_token to silently get a new access_token
     * without asking the user to log in again.
     *
     * @param refreshToken The refresh_token from previous token response
     * @return New TokenResponse with fresh access_token
     */
    public TokenResponse refreshToken(String refreshToken) {
        log.debug("Refreshing token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                tokenEndpoint,
                entity,
                TokenResponse.class
            );

            log.info("Token refreshed successfully");
            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.warn("Token refresh failed — session may have expired");
            throw new RuntimeException("Refresh token expired or invalid. Please login again.");
        }
    }

    /**
     * Logout: Revoke the refresh_token in Keycloak.
     *
     * After this call:
     * - The refresh_token is invalidated (can't get new access tokens)
     * - Existing access_tokens remain valid until they expire (5 min max)
     * - For immediate revocation, use token introspection on the resource server
     *
     * @param refreshToken The refresh_token to revoke
     */
    public void logout(String refreshToken) {
        log.debug("Logging out — revoking refresh token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(logoutEndpoint, entity, Void.class);
            log.info("User logged out — refresh token revoked");
        } catch (Exception e) {
            log.warn("Logout call failed (token may already be expired): {}", e.getMessage());
        }
    }
}
