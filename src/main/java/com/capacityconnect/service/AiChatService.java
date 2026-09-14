package com.capacityconnect.service;

import com.capacityconnect.dto.AiChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

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
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(8000);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
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

    public AiChatResponse publicChat(String message) {
        String prompt = """
                You are Capacity AI, the public website assistant for CAPACITY CONNECT,
                a digital capacity building and learning management platform for the
                Ministry of Earth Sciences / India Meteorological Department.

                Answer the visitor's question using only these verified website facts:
                - CAPACITY CONNECT is a centralized digital learning environment.
                - It serves three roles: trainee, trainer and administrator.
                - Trainees can discover learning programmes, access modules/resources,
                  complete assessments, track progress and earn certificates.
                - Trainers can create and manage courses, resources, assessments,
                  questionnaires and trainee activities.
                - Administrators manage users, trainer applications, courses, assessments,
                  certifications, analytics, competency mapping, announcements, achievements
                  and audit/governance functions.
                - The platform includes AI-assisted course/content and assessment workflows.
                - The platform brings learning, resources, assessment, certification,
                  analytics and professional development together.

                Keep the answer concise (prefer 80-120 words). Do not invent site features.
                Return ONLY valid JSON:
                {
                  "answer": "...",
                  "quickQueries": ["...", "...", "...", "..."]
                }

                Generate exactly four short follow-up questions directly related to the
                visitor's question and the website.

                Visitor question:
                %s
                """.formatted(message == null ? "" : message.trim());

        AiChatResponse fastResponse = fastPublicResponse(message);
        if (fastResponse != null) {
            return fastResponse;
        }

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "You are the fast public-facing Capacity AI website assistant."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", prompt
                            )
                    ),
                    "stream", false,
                    "temperature", 0.2,
                    "max_tokens", 450,
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
                    .asText()
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            JsonNode result = objectMapper.readTree(content);
            String answer = result.path("answer").asText();
            List<String> quickQueries = objectMapper.convertValue(
                    result.path("quickQueries"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );

            return new AiChatResponse(answer, quickQueries.stream().filter(q -> q != null && !q.isBlank()).limit(4).toList());
        } catch (Exception e) {
            throw new RuntimeException("QwenCloud public AI request failed", e);
        }
    }

    public AiChatResponse chat(String message) {
        return chat(message, "", "TRAINEE");
    }

    public AiChatResponse chat(String message, String resourceText) {
        return chat(message, resourceText, "TRAINEE");
    }

    public AiChatResponse chat(String message, String resourceText, String userRole) {
        return chat(message, resourceText, userRole, "");
    }

    public AiChatResponse chat(String message, String resourceText, String userRole, String activityContext) {
        String context = resourceText == null || resourceText.isBlank()
                ? ""
                : "\n\nRESOURCE CONTENT:\n" + resourceText.substring(0, Math.min(resourceText.length(), 30000));

        String role = "TRAINER".equalsIgnoreCase(userRole) ? "trainer" : "trainee";
        String activity = activityContext == null || activityContext.isBlank()
                ? "No recent LMS activity is available."
                : activityContext.substring(0, Math.min(activityContext.length(), 20000));

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

                RECENT LMS ACTIVITY CONTEXT:
                %s

                Use this activity context to personalise the answer and the 4 follow-up
                questions. Use only facts present in the activity context. Never invent
                course names, trainee names, scores, progress, notifications, or actions.
                Prioritise the user's latest learning/training activity and next useful action.

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
                """.formatted(role, roleGuidance, activity, message + context);

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
    private AiChatResponse fastPublicResponse(String message) {
        String q = message == null ? "" : message.trim().toLowerCase();

        if (q.isBlank()) {
            return new AiChatResponse(
                    "I can help you understand CAPACITY CONNECT, its roles, learning workflow and platform features.",
                    List.of(
                            "What is CAPACITY CONNECT?",
                            "What can trainees do?",
                            "How do trainers use the platform?",
                            "How does the AI assistant help?"
                    )
            );
        }

        if (q.contains("what is capacity connect") || q.contains("what is capacity") ||
                q.contains("about capacity connect")) {
            return new AiChatResponse(
                    "CAPACITY CONNECT is a centralized digital capacity-building and learning management platform for the Ministry of Earth Sciences and India Meteorological Department. It brings training programmes, learning resources, assessments, certification, analytics and professional development into one role-based platform for trainees, trainers and administrators.",
                    List.of(
                            "What can trainees do on CAPACITY CONNECT?",
                            "What can trainers manage?",
                            "What does the administrator handle?",
                            "How does AI support the platform?"
                    )
            );
        }

        if (q.contains("trainee") && (q.contains("what") || q.contains("do") || q.contains("role"))) {
            return new AiChatResponse(
                    "Trainees can discover learning programmes, access modules and digital resources, complete assessments, track learning progress, receive feedback and earn eligible certificates.",
                    List.of(
                            "How do trainees track progress?",
                            "What resources can trainees access?",
                            "How do assessments work?",
                            "How are certificates earned?"
                    )
            );
        }

        if (q.contains("trainer") && (q.contains("what") || q.contains("do") || q.contains("role"))) {
            return new AiChatResponse(
                    "Trainers can create and manage courses, modules, learning resources, assessments and questionnaires, while also monitoring trainee participation and performance.",
                    List.of(
                            "How does a trainer create a course?",
                            "How can trainers use AI?",
                            "How are trainee assessments managed?",
                            "What trainer analytics are available?"
                    )
            );
        }

        if ((q.contains("admin") || q.contains("administrator")) &&
                (q.contains("what") || q.contains("do") || q.contains("role"))) {
            return new AiChatResponse(
                    "Administrators manage users, trainer applications, courses, assessments, certifications, analytics, competency mapping, announcements, achievements and platform governance.",
                    List.of(
                            "How are trainer applications reviewed?",
                            "What platform analytics can admins see?",
                            "How are courses governed?",
                            "What does the audit system cover?"
                    )
            );
        }

        if (q.contains("ai assistant") || q.contains("how does ai") || q.contains("what can ai")) {
            return new AiChatResponse(
                    "Capacity AI provides role-aware assistance across the platform. It can help visitors understand CAPACITY CONNECT, while authenticated trainees and trainers can use contextual AI support for learning, course work, assessments and training activities.",
                    List.of(
                            "How can trainees use Capacity AI?",
                            "How can trainers use AI for courses?",
                            "Can AI generate assessments?",
                            "Can AI help with learning resources?"
                    )
            );
        }

        return null;
    }

}
