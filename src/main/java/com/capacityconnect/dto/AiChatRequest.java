package com.capacityconnect.dto;

import jakarta.validation.constraints.NotBlank;

public record AiChatRequest(
        @NotBlank String message,
        String resourceText
) {}
