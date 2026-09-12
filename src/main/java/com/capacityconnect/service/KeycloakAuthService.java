package com.capacityconnect.service;

import com.capacityconnect.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Service
public class KeycloakAuthService {

    @Value("${keycloak.url:https://keycloak-production-66a8.up.railway.app}")
    private String keycloakUrl;

    @Value("${keycloak.realm:capacity-connect}")
    private String realm;

    @Value("${keycloak.client-id:capacity-connect-frontend}")
    private String clientId;

    private final RestClient restClient = RestClient.create();

    public LoginResponse login(String username, String password) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("username", username);
        form.add("password", password);

        try {
            Map<?, ?> response = restClient.post()
                    .uri(keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            return new LoginResponse(
                    (String) response.get("access_token"),
                    (String) response.get("refresh_token"),
                    (String) response.get("id_token"),
                    response.get("expires_in") instanceof Number n ? n.longValue() : 0L,
                    response.get("refresh_expires_in") instanceof Number n ? n.longValue() : 0L,
                    (String) response.get("token_type"),
                    (String) response.get("session_state")
            );
        } catch (RestClientResponseException e) {
            System.err.println("Keycloak login failed: status=" + e.getStatusCode()
                    + ", body=" + e.getResponseBodyAsString());
            throw new IllegalArgumentException("Invalid username or password");
        }
    }
}
