package com.capacityconnect.controller;

import com.capacityconnect.dto.AiChatRequest;
import com.capacityconnect.dto.AiChatResponse;
import com.capacityconnect.dto.AiCourseRequest;
import com.capacityconnect.dto.AiAssessmentRequest;
import com.capacityconnect.service.AiChatService;
import com.capacityconnect.service.AiResourceService;
import com.capacityconnect.service.StudyResourceService;
import com.capacityconnect.entity.StudyResource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiResourceService aiResourceService;
    private final StudyResourceService studyResourceService;

    public AiChatController(
            AiChatService aiChatService,
            AiResourceService aiResourceService,
            StudyResourceService studyResourceService
    ) {
        this.aiChatService = aiChatService;
        this.aiResourceService = aiResourceService;
        this.studyResourceService = studyResourceService;
    }


    @PostMapping(value = "/resource", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public String extractResource(@RequestParam("file") MultipartFile file) throws Exception {
        return aiResourceService.extractText(file);
    }

    @PostMapping(value = "/chat/resource", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public AiChatResponse chatWithResource(
            @RequestParam("message") String message,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "activityContext", required = false) String activityContext,
            org.springframework.security.core.Authentication authentication
    ) throws Exception {
        String resourceText = aiResourceService.extractText(file);

        return aiChatService.chat(message, resourceText, roleOf(authentication), activityContext);
    }

    @PostMapping("/chat/link")
    @PreAuthorize("isAuthenticated()")
    public AiChatResponse chatWithLink(
            @RequestParam("message") String message,
            @RequestParam("url") String url,
            @RequestParam(value = "activityContext", required = false) String activityContext,
            org.springframework.security.core.Authentication authentication
    ) throws Exception {
        String resourceText = aiResourceService.extractTextFromUrl(url);
        return aiChatService.chat(message, resourceText, roleOf(authentication), activityContext);
    }

    @PostMapping(value = "/chat/resource-id", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public AiChatResponse chatWithStoredResource(
            @RequestParam("message") String message,
            @RequestParam("resourceId") Long resourceId,
            @RequestParam(value = "activityContext", required = false) String activityContext,
            org.springframework.security.core.Authentication authentication
    ) throws Exception {
        String resourceText = studyResourceService.getAiContent(resourceId, authentication);
        return aiChatService.chat(message, resourceText, roleOf(authentication), activityContext);
    }

    @PostMapping("/public-chat")
    public AiChatResponse publicChat(
            @Valid @RequestBody AiChatRequest request
    ) {
        return aiChatService.publicChat(request.message());
    }

    @PostMapping(value = "/public-chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> publicChatStream(
            @Valid @RequestBody AiChatRequest request
    ) {
        StreamingResponseBody body = outputStream -> aiChatService.streamPublicChat(request.message(), outputStream);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(body);
    }

    @PostMapping("/course/generate")
    @PreAuthorize("hasRole('TRAINER')")
    public String generateCourse(
            @Valid @RequestBody AiCourseRequest request
    ) {
        return aiChatService.generateCourse(
                request.getTopic(),
                request.getContext(),
                request.getLevel(),
                request.getModuleCount()
        );
    }

    @PostMapping("/assessment/generate")
    @PreAuthorize("hasRole('TRAINER')")
    public String generateAssessment(
            @Valid @RequestBody AiAssessmentRequest request
    ) {
        return aiChatService.generateAssessmentQuestions(
                request.getTopic(),
                request.getContext(),
                request.getQuestionCount(),
                request.getDifficulty()
        );
    }

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public AiChatResponse chat(
            @Valid @RequestBody AiChatRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        return aiChatService.chat(
                request.message(),
                request.resourceText() == null ? "" : request.resourceText(),
                roleOf(authentication),
                request.activityContext()
        );
    }

    private String roleOf(org.springframework.security.core.Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_TRAINER".equals(a.getAuthority()))) {
            return "TRAINER";
        }
        return "TRAINEE";
    }
}
