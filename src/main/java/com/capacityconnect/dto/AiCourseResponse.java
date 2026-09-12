package com.capacityconnect.dto;

import java.util.List;

public record AiCourseResponse(
        String title,
        String description,
        String category,
        String department,
        String level,
        Integer durationHours,
        List<Module> modules
) {
    public record Module(
            String title,
            String description
    ) {}
}
