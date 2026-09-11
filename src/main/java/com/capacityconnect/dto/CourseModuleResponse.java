package com.capacityconnect.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CourseModuleResponse {

    private Long id;
    private Long courseId;
    private String title;
    private String description;
    private Integer orderIndex;
    private Boolean active;

    public CourseModuleResponse(
            Long id,
            Long courseId,
            String title,
            String description,
            Integer orderIndex,
            Boolean active) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.orderIndex = orderIndex;
        this.active = active;
    }
}
