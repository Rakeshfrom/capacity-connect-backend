package com.capacityconnect.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Service
public class GmailApiEmailService {

    private static final String TOKEN_URL =
            "https://oauth2.googleapis.com/token";

    private static final String SEND_URL =
            "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${gmail.oauth.client-id:}")
    private String clientId;

    @Value("${gmail.oauth.client-secret:}")
    private String clientSecret;

    @Value("${gmail.oauth.refresh-token:}")
    private String refreshToken;

    @Value("${gmail.oauth.sender-email:rakeshfromsiraha@gmail.com}")
    private String senderEmail;

    @Value("${gmail.oauth.sender-name:CAPACITY CONNECT}")
    private String senderName;

    public void sendPasswordResetEmail(
            String recipient,
            String resetLink,
            String idempotencyKey) {

        String accessToken = getAccessToken();
        String mimeMessage = buildMimeMessage(
                recipient,
                resetLink
        );

        String encodedMessage = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        mimeMessage.getBytes(StandardCharsets.UTF_8)
                );

        try {
            String payload = objectMapper.writeValueAsString(
                    Map.of("raw", encodedMessage)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SEND_URL))
                    .timeout(Duration.ofSeconds(20))
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + accessToken
                    )
                    .header(
                            HttpHeaders.CONTENT_TYPE,
                            MediaType.APPLICATION_JSON_VALUE
                    )
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {
                System.err.println(
                        "Gmail API send failed: "
                                + response.statusCode()
                                + " "
                                + response.body()
                );
                throw new IllegalStateException(
                        "Unable to send password reset email"
                );
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Unable to send password reset email",
                    e
            );

        } catch (Exception e) {
            if (e instanceof IllegalStateException stateException) {
                throw stateException;
            }

            System.err.println(
                    "Gmail API connection failed: " + e.getMessage()
            );

            throw new IllegalStateException(
                    "Unable to send password reset email",
                    e
            );
        }
    }

    private String getAccessToken() {
        if (clientId.isBlank()
                || clientSecret.isBlank()
                || refreshToken.isBlank()) {
            throw new IllegalStateException(
                    "Gmail OAuth credentials are not configured"
            );
        }

        String form = "client_id="
                + encode(clientId)
                + "&client_secret="
                + encode(clientSecret)
                + "&refresh_token="
                + encode(refreshToken)
                + "&grant_type=refresh_token";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(15))
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_FORM_URLENCODED_VALUE
                )
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {
                System.err.println(
                        "Google OAuth token refresh failed: "
                                + response.statusCode()
                                + " "
                                + response.body()
                );

                throw new IllegalStateException(
                        "Gmail OAuth authorization is invalid or expired"
                );
            }

            Map<String, Object> body = objectMapper.readValue(
                    response.body(),
                    new TypeReference<Map<String, Object>>() {}
            );

            Object accessToken = body.get("access_token");

            if (accessToken == null
                    || String.valueOf(accessToken).isBlank()) {
                throw new IllegalStateException(
                        "Google OAuth did not return an access token"
                );
            }

            return String.valueOf(accessToken);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Unable to refresh Gmail authorization",
                    e
            );

        } catch (Exception e) {
            if (e instanceof IllegalStateException stateException) {
                throw stateException;
            }

            throw new IllegalStateException(
                    "Unable to refresh Gmail authorization",
                    e
            );
        }
    }

    private String buildMimeMessage(
            String recipient,
            String resetLink) {

        String from = senderName == null || senderName.isBlank()
                ? senderEmail
                : senderName + " <" + senderEmail + ">";

        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;color:#173F60">
                  <h2 style="margin-bottom:8px">Reset your CAPACITY CONNECT password</h2>
                  <p>We received a request to create a new password for your account.</p>
                  <p>
                    <a href="%s"
                       style="display:inline-block;padding:12px 20px;background:#0B5A91;color:#fff;text-decoration:none;border-radius:6px">
                      Create new password
                    </a>
                  </p>
                  <p style="font-size:13px;color:#657887">
                    This link expires in 30 minutes and can be used only once.
                  </p>
                  <p style="font-size:13px;color:#657887">
                    If you did not request a password reset, you can ignore this email.
                  </p>
                </div>
                """.formatted(resetLink);

        return "MIME-Version: 1.0\\r\\n"
                + "From: " + from + "\\r\\n"
                + "To: " + recipient + "\\r\\n"
                + "Subject: Reset your CAPACITY CONNECT password\\r\\n"
                + "Content-Type: text/html; charset=UTF-8\\r\\n"
                + "Content-Transfer-Encoding: 8bit\\r\\n"
                + "\\r\\n"
                + html;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
