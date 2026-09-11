package com.capacityconnect.controller;

import com.capacityconnect.dto.AiChatRequest;
import com.capacityconnect.dto.AiChatResponse;
import com.capacityconnect.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        return aiChatService.chat(request.message());
    }
}
