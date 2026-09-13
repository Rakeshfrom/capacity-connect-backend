package com.capacityconnect.service;

import com.capacityconnect.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import org.springframework.util.LinkedMultiValueMap;

@Service
public class KeycloakRegistrationService {

    @Value("${keycloak.url:https://keycloak-production-66a8.up.railway.app}")
    private String keycloakUrl;

    @Value("${keycloak.realm:capacity-connect}")
    private String realm;

    @Value("${keycloak.client-id:capacity-connect-frontend}")
    private String clientId;

    @Value("${keycloak.admin-client-id:capacity-connect-admin}")
    private String adminClientId;

    @Value("${keycloak.admin-client-secret:}")
    private String adminClientSecret;

    @Value("${keycloak.frontend-url:https://capacity-connect-frontend-tawny.vercel.app}")
    private String frontendUrl;

    private final RestClient restClient = RestClient.create();

    public void register(RegisterRequest request) {
        String[] name = request.fullName().trim().split("\\s+", 2);
        String firstName = name[0];
        String lastName = name.length > 1 ? name[1] : "";

        String token = getAdminToken();

        Map<String, Object> user = Map.of(
                "username", request.email().trim(),
                "email", request.email().trim(),
                "firstName", firstName,
                "lastName", lastName,
                "enabled", true,
                "emailVerified", false,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", request.password(),
                        "temporary", false
                ))
        );

        try {
            var response = restClient.post()
                    .uri(keycloakUrl + "/admin/realms/" + realm + "/users")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(user)
                    .retrieve()
                    .toBodilessEntity();

            String location = response.getHeaders().getFirst("Location");

            if (location != null) {
                String userId = location.substring(location.lastIndexOf('/') + 1);
                assignTraineeRole(token, userId);
            }
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 409) {
                throw new IllegalArgumentException("An account with this email already exists");
            }
            System.err.println("Keycloak user creation failed: "
                    + e.getStatusCode() + " " + e.getResponseBodyAsString());
            throw new IllegalArgumentException("Unable to create account");
        }
    }

    public void sendPasswordReset(String email) {
        String normalizedEmail = email == null ? "" : email.trim();

        if (normalizedEmail.isBlank()) {
            return;
        }

        String token = getAdminToken();

        try {
            List<?> users = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme(keycloakUrl.startsWith("https://") ? "https" : "http")
                            .host(keycloakUrl
                                    .replace("https://", "")
                                    .replace("http://", "")
                                    .replaceAll("/$", ""))
                            .path("/admin/realms/" + realm + "/users")
                            .queryParam("email", normalizedEmail)
                            .build())
                    .header("Authorization", "Bearer " + token)
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

                if (
                        id != null &&
                        userEmail != null &&
                        normalizedEmail.equalsIgnoreCase(
                                String.valueOf(userEmail)
                        )
                ) {
                    userId = String.valueOf(id);
                    break;
                }
            }

            if (userId == null) {
                return;
            }

            String redirectUri =
                    frontendUrl.replaceAll("/$", "") + "/login";

            final String finalUserId = userId;

            restClient
                    .put()
                    .uri(uriBuilder -> uriBuilder
                            .scheme(
                                    keycloakUrl.startsWith("https://")
                                            ? "https"
                                            : "http"
                            )
                            .host(
                                    keycloakUrl
                                            .replace("https://", "")
                                            .replace("http://", "")
                                            .replaceAll("/$", "")
                            )
                            .path(
                                    "/admin/realms/" +
                                            realm +
                                            "/users/" +
                                            finalUserId +
                                            "/execute-actions-email"
                            )
                            .queryParam(
                                    "client_id",
                                    clientId
                            )
                            .queryParam(
                                    "redirect_uri",
                                    redirectUri
                            )
                            .build())
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of("UPDATE_PASSWORD"))
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientResponseException e) {
            System.err.println(
                    "Keycloak password reset request failed: "
                            + e.getStatusCode()
                            + " "
                            + e.getResponseBodyAsString()
            );

            throw new IllegalStateException(
                    "Unable to send password reset email"
            );
        }
    }

    private LinkedMultiValueMap<String, String> adminTokenForm() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", adminClientId);
        form.add("client_secret", adminClientSecret);
        return form;
    }

    private String getAdminToken() {
        try {
            Map<?, ?> response = restClient.post()
                    .uri(keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(adminTokenForm())
                    .retrieve()
                    .body(Map.class);

            return (String) response.get("access_token");
        } catch (RestClientResponseException e) {
            System.err.println("Keycloak admin authentication failed: "
                    + e.getStatusCode() + " " + e.getResponseBodyAsString());
            throw new IllegalStateException("Keycloak admin authentication failed");
        }
    }

    private void assignTraineeRole(String token, String userId) {
        Map<?, ?> role = restClient.get()
                .uri(keycloakUrl + "/admin/realms/" + realm + "/roles/TRAINEE")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(Map.class);

        restClient.post()
                .uri(keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
    }
}
