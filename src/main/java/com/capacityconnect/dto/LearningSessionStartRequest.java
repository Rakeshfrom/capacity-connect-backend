package com.capacityconnect.dto;

public record LearningSessionStartRequest(
        Long courseId,
        Long moduleId
) {}
