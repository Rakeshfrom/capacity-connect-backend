package com.capacityconnect.dto;

import java.util.List;

public record AiChatResponse(
        String answer,
        List<String> quickQueries
) {}
