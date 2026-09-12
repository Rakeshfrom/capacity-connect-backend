package com.capacityconnect.controller;

import com.capacityconnect.dto.AiChatRequest;
import com.capacityconnect.dto.AiChatResponse;
import com.capacityconnect.service.AiChatService;
import com.capacityconnect.service.AiResourceService;
import org.springframework.http.MediaType;
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

    public AiChatController(
            AiChatService aiChatService,
            AiResourceService aiResourceService
    ) {
        this.aiChatService = aiChatService;
        this.aiResourceService = aiResourceService;
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
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        String resourceText = aiResourceService.extractText(file);

        return aiChatService.chat(message, resourceText);
    }

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public AiChatResponse chat(
            @Valid @RequestBody AiChatRequest request
    ) {
        return aiChatService.chat(
                request.message(),
                request.resourceText() == null ? "" : request.resourceText()
        );
    }
}
