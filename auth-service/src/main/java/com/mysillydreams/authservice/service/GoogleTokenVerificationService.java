package com.mysillydreams.authservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

/**
 * Service for verifying Google ID tokens
 * Provides secure token validation using Google's token verification endpoint
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleTokenVerificationService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    private static final String GOOGLE_TOKEN_VERIFY_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    /**
     * Verify Google ID token using Google's verification endpoint
     * This is more secure than just decoding the JWT
     */
    public GoogleUserInfo verifyGoogleIdToken(String idToken) {
        try {
            log.info("🔍 Verifying Google ID token with Google's verification endpoint");

            // First, verify with Google's endpoint for security
            String verificationUrl = GOOGLE_TOKEN_VERIFY_URL + idToken;
            ResponseEntity<String> response = restTemplate.getForEntity(verificationUrl, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Google token verification failed with status: " + response.getStatusCode());
            }

            JsonNode verificationResult = objectMapper.readTree(response.getBody());

            // Verify the audience (client ID) matches our application
            String audience = verificationResult.get("aud").asText();
            if (!googleClientId.equals(audience)) {
                throw new RuntimeException("Token audience mismatch. Expected: " + googleClientId + ", Got: " + audience);
            }

            // Verify token is not expired
            long exp = verificationResult.get("exp").asLong();
            long currentTime = System.currentTimeMillis() / 1000;
            if (currentTime >= exp) {
                throw new RuntimeException("Google ID token has expired");
            }

            // Extract user information from verified token
            GoogleUserInfo userInfo = new GoogleUserInfo();
            userInfo.setGoogleId(verificationResult.get("sub").asText());
            userInfo.setEmail(verificationResult.get("email").asText());
            userInfo.setFirstName(verificationResult.has("given_name") ? verificationResult.get("given_name").asText() : "");
            userInfo.setLastName(verificationResult.has("family_name") ? verificationResult.get("family_name").asText() : "");
            userInfo.setName(verificationResult.has("name") ? verificationResult.get("name").asText() : userInfo.getFirstName() + " " + userInfo.getLastName());
            userInfo.setPicture(verificationResult.has("picture") ? verificationResult.get("picture").asText() : "");
            userInfo.setEmailVerified(verificationResult.has("email_verified") ? verificationResult.get("email_verified").asBoolean() : false);

            log.info("✅ Google ID token verified successfully for user: {}", userInfo.getEmail());
            return userInfo;

        } catch (Exception e) {
            log.error("❌ Failed to verify Google ID token: {}", e.getMessage());
            throw new RuntimeException("Invalid Google ID token: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method: Decode JWT token locally (less secure, for development only)
     * In production, always use the verification endpoint above
     */
    public GoogleUserInfo decodeGoogleIdTokenLocally(String idToken) {
        try {
            log.warn("⚠️ Using local JWT decoding - this should only be used in development!");

            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new RuntimeException("Invalid Google ID token format");
            }

            // Decode payload
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode jsonNode = objectMapper.readTree(payload);

            // Basic validation
            String audience = jsonNode.get("aud").asText();
            if (!googleClientId.equals(audience)) {
                throw new RuntimeException("Token audience mismatch");
            }

            // Extract user information
            GoogleUserInfo userInfo = new GoogleUserInfo();
            userInfo.setGoogleId(jsonNode.get("sub").asText());
            userInfo.setEmail(jsonNode.get("email").asText());
            userInfo.setFirstName(jsonNode.has("given_name") ? jsonNode.get("given_name").asText() : "");
            userInfo.setLastName(jsonNode.has("family_name") ? jsonNode.get("family_name").asText() : "");
            userInfo.setName(jsonNode.has("name") ? jsonNode.get("name").asText() : userInfo.getFirstName() + " " + userInfo.getLastName());
            userInfo.setPicture(jsonNode.has("picture") ? jsonNode.get("picture").asText() : "");
            userInfo.setEmailVerified(jsonNode.has("email_verified") ? jsonNode.get("email_verified").asBoolean() : false);

            log.debug("✅ Google ID token decoded locally for user: {}", userInfo.getEmail());
            return userInfo;

        } catch (Exception e) {
            log.error("❌ Failed to decode Google ID token locally: {}", e.getMessage());
            throw new RuntimeException("Invalid Google ID token", e);
        }
    }

    /**
     * Google user information class
     */
    public static class GoogleUserInfo {
        private String googleId;
        private String email;
        private String firstName;
        private String lastName;
        private String name;
        private String picture;
        private boolean emailVerified;

        // Getters and setters
        public String getGoogleId() { return googleId; }
        public void setGoogleId(String googleId) { this.googleId = googleId; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPicture() { return picture; }
        public void setPicture(String picture) { this.picture = picture; }

        public boolean isEmailVerified() { return emailVerified; }
        public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    }
}
