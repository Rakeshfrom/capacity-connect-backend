package com.capacityconnect.service;

import com.capacityconnect.dto.PasswordResetRequest;
import com.capacityconnect.entity.PasswordResetToken;
import com.capacityconnect.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class PasswordResetService {

    @Value("${keycloak.url:https://keycloak-production-66a8.up.railway.app}")
    private String keycloakUrl;

    @Value("${keycloak.realm:capacity-connect}")
    private String realm;

    @Value("${keycloak.admin-client-id:capacity-connect-admin}")
    private String adminClientId;

    @Value("${keycloak.admin-client-secret:}")
    private String adminClientSecret;

    @Value("${keycloak.frontend-url:https://capacity-connect-frontend-tawny.vercel.app}")
    private String frontendUrl;

    @Value("${password-reset.token-minutes:30}")
    private long tokenMinutes;

    private final PasswordResetTokenRepository tokenRepository;
    private final GmailApiEmailService gmailApiEmailService;
    private final RestClient restClient = RestClient.create();
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            GmailApiEmailService gmailApiEmailService) {
        this.tokenRepository = tokenRepository;
        this.gmailApiEmailService = gmailApiEmailService;
    }

    public void sendPasswordReset(String email) {
        String normalizedEmail = email == null ? "" : email.trim();

        if (normalizedEmail.isBlank()) {
            return;
        }

        String adminToken = getAdminToken();

        try {
            List<?> users = restClient.get()
                    .uri(
                            keycloakUrl.replaceAll("/$", "")
                                    + "/admin/realms/"
                                    + realm
                                    + "/users?email="
                                    + URLEncoder.encode(normalizedEmail, StandardCharsets.UTF_8)
                    )
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve()
                    .body(List.class);

            if (users == null) {
                return;
            }

            String userId = null;

            for (Object item : users) {
                if (!(item instanceof Map<?, ?> user)) {
                    continue;
                }

                Object id = user.get("id");
                Object userEmail = user.get("email");

                if (id != null
                        && userEmail != null
                        && normalizedEmail.equalsIgnoreCase(String.valueOf(userEmail))) {
                    userId = String.valueOf(id);
                    break;
                }
            }

            if (userId == null) {
                return;
            }

            tokenRepository.deleteByKeycloakUserIdAndUsedAtIsNull(userId);

            String rawToken = generateToken();
            String tokenHash = sha256(rawToken);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .tokenHash(tokenHash)
                    .keycloakUserId(userId)
                    .email(normalizedEmail)
                    .expiresAt(LocalDateTime.now().plusMinutes(tokenMinutes))
                    .build();

            resetToken = tokenRepository.save(resetToken);

            String resetLink = frontendUrl.replaceAll("/$", "")
                    + "/reset-password?token="
                    + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

            try {
                gmailApiEmailService.sendPasswordResetEmail(
                        normalizedEmail,
                        resetLink,
                        "password-reset-" + tokenHash
                );
            } catch (RuntimeException e) {
                tokenRepository.deleteById(resetToken.getId());
                throw e;
            }

        } catch (RestClientResponseException e) {
            System.err.println(
                    "Keycloak password reset user lookup failed: "
                            + e.getStatusCode()
                            + " "
                            + e.getResponseBodyAsString()
            );
            throw new IllegalStateException("Unable to process password reset request");
        }
    }

    public void resetPassword(PasswordResetRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        String tokenHash = sha256(request.token().trim());

        PasswordResetToken resetToken = tokenRepository
                .findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
                        tokenHash,
                        LocalDateTime.now()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "This password reset link is invalid or has expired"
                        )
                );

        String adminToken = getAdminToken();

        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", request.newPassword(),
                "temporary", false
        );

        Map<String, Object> payload = Map.of(
                "credentials", List.of(credential)
        );

        try {
            restClient.put()
                    .uri(
                            keycloakUrl.replaceAll("/$", "")
                                    + "/admin/realms/"
                                    + realm
                                    + "/users/"
                                    + resetToken.getKeycloakUserId()
                    )
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            resetToken.setUsedAt(LocalDateTime.now());
            tokenRepository.save(resetToken);

        } catch (RestClientResponseException e) {
            System.err.println(
                    "Keycloak password update failed: "
                            + e.getStatusCode()
                            + " "
                            + e.getResponseBodyAsString()
            );
            throw new IllegalStateException("Unable to reset password");
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to create reset token");
        }
    }

    private String getAdminToken() {
        try {
            LinkedMultiValueMap<String, String> form =
                    new LinkedMultiValueMap<>();

            form.add("grant_type", "client_credentials");
            form.add("client_id", adminClientId);
            form.add("client_secret", adminClientSecret);

            Map<?, ?> response = restClient.post()
                    .uri(
                            keycloakUrl.replaceAll("/$", "")
                                    + "/realms/"
                                    + realm
                                    + "/protocol/openid-connect/token"
                    )
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            Object accessToken = response == null ? null : response.get("access_token");

            if (accessToken == null || String.valueOf(accessToken).isBlank()) {
                throw new IllegalStateException("Keycloak admin authentication failed");
            }

            return String.valueOf(accessToken);

        } catch (RestClientResponseException e) {
            System.err.println(
                    "Keycloak admin authentication failed: "
                            + e.getStatusCode()
                            + " "
                            + e.getResponseBodyAsString()
            );
            throw new IllegalStateException("Keycloak admin authentication failed");
        }
    }
}
