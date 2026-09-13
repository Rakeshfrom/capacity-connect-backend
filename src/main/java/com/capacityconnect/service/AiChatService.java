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

    public String generateAssessmentQuestions(
            String topic,
            String context,
            int questionCount,
            String difficulty
    ) {
        String prompt = """
                You are Capacity AI helping a trainer create an assessment for an LMS.

                Generate exactly %d multiple-choice questions.

                Topic:
                %s

                Additional context:
                %s

                Difficulty:
                %s

                Every question must have:
                - questionText
                - optionA
                - optionB
                - optionC
                - optionD
                - correctOption (A, B, C, or D)
                - marks (integer)

                Return ONLY valid JSON in this exact structure:
                {
                  "questions": [
                    {
                      "questionText": "...",
                      "optionA": "...",
                      "optionB": "...",
                      "optionC": "...",
                      "optionD": "...",
                      "correctOption": "A",
                      "marks": 1
                    }
                  ]
                }

                Do not add explanations or markdown.
                """.formatted(
                questionCount,
                topic == null ? "" : topic,
                context == null ? "" : context.substring(0, Math.min(context.length(), 20000)),
                difficulty == null ? "MEDIUM" : difficulty
        );

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "You are an expert educational assessment generator."
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

            return content.replace("```json", "")
                    .replace("```", "")
                    .trim();

        } catch (Exception e) {
            throw new RuntimeException("QwenCloud assessment generation failed", e);
        }
    }

    public String generateCourse(
            String topic,
            String context,
            String level,
            int moduleCount
    ) {
        String prompt = """
                You are Capacity AI, an expert instructional designer for a professional LMS.

                Create a complete course draft.

                Topic:
                %s

                Additional context:
                %s

                Difficulty level:
                %s

                Generate exactly %d modules.

                Return ONLY valid JSON:
                {
                  "title": "...",
                  "description": "...",
                  "category": "...",
                  "department": "...",
                  "level": "BEGINNER",
                  "durationHours": 10,
                  "modules": [
                    {
                      "title": "...",
                      "description": "..."
                    }
                  ]
                }

                Requirements:
                - Make the course practical and professionally structured.
                - Use concise module descriptions.
                - durationHours must be a realistic positive integer.
                - level must be BEGINNER, INTERMEDIATE, or ADVANCED.
                - Do not add markdown or explanations.
                """.formatted(
                topic,
                context == null ? "" : context.substring(0, Math.min(context.length(), 20000)),
                level == null ? "BEGINNER" : level,
                moduleCount
        );

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "You are an expert instructional designer."
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

            return content.replace("```json", "")
                    .replace("```", "")
                    .trim();

        } catch (Exception e) {
            throw new RuntimeException("QwenCloud course generation failed", e);
        }
    }

    public AiChatResponse chat(String message) {
        return chat(message, "", "TRAINEE");
    }

    public AiChatResponse chat(String message, String resourceText) {
        return chat(message, resourceText, "TRAINEE");
    }

    public AiChatResponse chat(String message, String resourceText, String userRole) {
        String context = resourceText == null || resourceText.isBlank()
                ? ""
                : "\n\nRESOURCE CONTENT:\n" + resourceText.substring(0, Math.min(resourceText.length(), 30000));

        String role = "TRAINER".equalsIgnoreCase(userRole) ? "trainer" : "trainee";

        String roleGuidance = role.equals("trainer")
                ? """
                  Focus on course design, lesson planning, assessment creation,
                  trainee performance, learner engagement, feedback and training delivery.
                  """
                : """
                  Focus on course concepts, explanations, revision, practice questions,
                  study planning, progress and the learner's next best study action.
                  """;

        String prompt = """
                You are Capacity AI inside the CAPACITY CONNECT LMS.

                Current user role: %s.

                Answer the user's question clearly, accurately and practically.

                %s

                After the answer, generate exactly 4 short, natural follow-up questions
                that this same role would genuinely ask next. The questions must be
                specific to the user's role and directly related to the current topic.

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

                Do not generate generic questions when a role-specific question is possible.

                If resource content is provided, use it as the primary source for
                answering questions about that resource.

                User question:
                %s
                """.formatted(role, roleGuidance, message + context);

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
