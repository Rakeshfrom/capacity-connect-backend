package com.capacityconnect.service;

import com.capacityconnect.dto.AiChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class AiChatService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${DASHSCOPE_API_KEY:}")
    private String apiKey;

    @Value("${DASHSCOPE_BASE_URL:https://dashscope-intl.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${DASHSCOPE_MODEL:qwen3.7-plus}")
    private String model;

    public AiChatService() {
        this.restClient = RestClient.create();
    }

    public AiChatResponse chat(String message) {
        return chat(message, "");
    }

    public AiChatResponse chat(String message, String resourceText) {
        String context = resourceText == null || resourceText.isBlank()
                ? ""
                : "\n\nRESOURCE CONTENT:\n" + resourceText.substring(0, Math.min(resourceText.length(), 30000));

        String prompt = """
                You are Capacity AI, an educational LMS assistant.

                Answer the user's question clearly and accurately.

                After the answer, generate exactly 4 short, natural follow-up questions
                that are directly related to the user's current question.

                Return ONLY valid JSON in this exact structure:
                {
                  "answer": "your answer",
                  "quickQueries": [
                    "question 1",
                    "question 2",
                    "question 3",
                    "question 4"
                  ]
                }

                If resource content is provided, use it as the primary source for
                answering questions about that resource.

                User question:
                """ + message + context;

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "You are Capacity AI, a helpful educational LMS assistant."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", prompt
                            )
                    ),
                    "stream", false,
                    "extra_body", Map.of("enable_thinking", false)
            );

            String raw = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

            content = content.replace("```json", "")
                    .replace("```", "")
                    .trim();

            JsonNode result = objectMapper.readTree(content);

            String answer = result.path("answer").asText();
            List<String> quickQueries = objectMapper.convertValue(
                    result.path("quickQueries"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );

            return new AiChatResponse(answer, quickQueries);

        } catch (Exception e) {
            throw new RuntimeException("QwenCloud AI request failed", e);
        }
    }
}
