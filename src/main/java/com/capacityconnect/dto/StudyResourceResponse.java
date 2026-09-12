package com.capacityconnect.dto;

import com.capacityconnect.entity.StudyResource;

import java.time.LocalDateTime;

public record StudyResourceResponse(
        Long id,
        String title,
        String description,
        String department,
        StudyResource.Type type,
        String url,
        String originalFileName,
        String contentType,
        LocalDateTime createdAt
) {}
