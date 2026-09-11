package com.capacityconnect.dto;

import com.capacityconnect.entity.Assessment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AssessmentResponse {

    private Long id;
    private String title;
    private String description;
    private Long courseId;
    private Long moduleId;
    private Integer timeLimitMinutes;
    private Integer passingPercentage;
    private Assessment.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
