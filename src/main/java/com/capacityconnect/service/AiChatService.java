package com.capacityconnect.service;

import com.capacityconnect.dto.AiChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class AiChatService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ollama.url:http://localhost:11434/api/generate}")
    private String ollamaUrl;

    @Value("${ollama.model:qwen3:4b}")
    private String ollamaModel;

    public AiChatService() {
        this.restClient = RestClient.create();
    }

    public AiChatResponse chat(String message) {
        String prompt = """
                You are Capacity AI, an educational LMS assistant.

                Answer the user's question clearly and helpfully.

                After the answer, generate exactly 4 short, natural follow-up questions
                that are directly related to the user's question and your answer.
                These must be useful clickable queries for continuing the conversation.

                Return ONLY valid JSON:
                {
                  "answer": "your answer",
                  "quickQueries": [
                    "follow-up question 1",
                    "follow-up question 2",
                    "follow-up question 3",
                    "follow-up question 4"
                  ]
                }

                User question:
                %s
                """.formatted(message);

        String body = """
                {
                  "model": "%s",
                  "prompt": %s,
                  "stream": false,
                  "format": "json"
                }
                """.formatted(ollamaModel, quote(prompt));

        String raw = restClient.post()
                .uri(ollamaUrl)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(raw);
            String response = root.path("response").asText();
            JsonNode result = objectMapper.readTree(response);

            List<String> quickQueries = objectMapper.convertValue(
                    result.path("quickQueries"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );

            return new AiChatResponse(
                    result.path("answer").asText(),
                    quickQueries
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response", e);
        }
    }

    private String quote(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
